import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { wsServerPlugin } from './vite-ws-plugin';

export default defineConfig({
  plugins: [react(), wsServerPlugin()],
});
