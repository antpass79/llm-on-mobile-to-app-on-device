import type { ITransport, ParameterMessage } from './ITransport';

export class WebSocketTransport implements ITransport {
  private ws: WebSocket | null = null;
  private messageCallback: ((message: ParameterMessage) => void) | null = null;
  private connectCallback: (() => void) | null = null;
  private disconnectCallback: (() => void) | null = null;
  private connected = false;

  connect(url: string): void {
    if (this.ws) {
      this.ws.close();
    }

    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      this.connected = true;
      this.connectCallback?.();
    };

    this.ws.onclose = () => {
      this.connected = false;
      this.ws = null;
      this.disconnectCallback?.();
    };

    this.ws.onerror = () => {
      this.connected = false;
    };

    this.ws.onmessage = (event: MessageEvent) => {
      const handle = (text: string) => {
        try {
          const message = JSON.parse(text) as ParameterMessage;
          this.messageCallback?.(message);
        } catch {
          console.error('Failed to parse WebSocket message:', text);
        }
      };
      const data: unknown = event.data;
      if (typeof data === 'string') {
        handle(data);
      } else if (data instanceof Blob) {
        data.text().then(handle);
      } else if (data instanceof ArrayBuffer) {
        handle(new TextDecoder().decode(data));
      } else {
        console.error('Failed to parse WebSocket message:', data);
      }
    };
  }

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
    this.connected = false;
  }

  send(message: ParameterMessage): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('[WS] send dropped, readyState=', this.ws?.readyState, message);
    }
  }

  onMessage(callback: (message: ParameterMessage) => void): void {
    this.messageCallback = callback;
  }

  onConnect(callback: () => void): void {
    this.connectCallback = callback;
  }

  onDisconnect(callback: () => void): void {
    this.disconnectCallback = callback;
  }

  isConnected(): boolean {
    return this.connected;
  }
}
