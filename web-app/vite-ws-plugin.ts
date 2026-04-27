import { WebSocketServer } from 'ws';
import os from 'os';
import type { Plugin } from 'vite';

export const WS_PORT = 8080;

export function getLocalIP(): string {
  const interfaces = os.networkInterfaces();
  for (const iface of Object.values(interfaces)) {
    for (const alias of iface ?? []) {
      if (alias.family === 'IPv4' && !alias.internal) {
        return alias.address;
      }
    }
  }
  return '127.0.0.1';
}

export function wsServerPlugin(): Plugin {
  const localIP = getLocalIP();
  let wss: WebSocketServer | null = null;

  return {
    name: 'vite-plugin-ws-server',

    // Inject host and port as global constants so the React app can read them.
    config() {
      return {
        define: {
          __WS_HOST__: JSON.stringify(localIP),
          __WS_PORT__: WS_PORT,
        },
      };
    },

    // Start the WebSocket server alongside the Vite dev server.
    configureServer(server) {
      wss = new WebSocketServer({ port: WS_PORT });

      wss.on('connection', (ws) => {
        ws.on('message', (data) => {
          // Relay to every OTHER connected client — no echo back to sender.
          wss!.clients.forEach((client) => {
            if (client !== ws && client.readyState === 1 /* OPEN */) {
              client.send(data);
            }
          });
        });
      });

      // Close the WS server when the Vite dev server closes.
      server.httpServer?.on('close', () => wss?.close());

      console.log(`\n  ⚡ WebSocket server:  ws://localhost:${WS_PORT}`);
      console.log(`     (LAN clients use)   ws://${localIP}:${WS_PORT}\n`);
    },

    closeBundle() {
      wss?.close();
    },
  };
}
