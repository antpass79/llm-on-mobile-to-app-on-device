import type { ITransport, ParameterMessage } from './transport/ITransport';
import type { ImageParameters } from '../store/imageParametersStore';
import { useImageParametersStore } from '../store/imageParametersStore';

export type ConnectionStatus = 'disconnected' | 'connected';

type StatusCallback = (status: ConnectionStatus) => void;

class ParameterService {
  private transport: ITransport | null = null;
  private statusCallback: StatusCallback | null = null;
  private _status: ConnectionStatus = 'disconnected';

  /**
   * Bind a transport implementation.
   * Call this once at application startup before connect().
   * Swap the transport here to switch between WebSocket, RabbitMQ, etc.
   */
  setTransport(transport: ITransport): void {
    this.transport = transport;

    transport.onMessage((message: ParameterMessage) => {
      if (message.type === 'update' || message.type === 'sync') {
        useImageParametersStore.getState().setParameters(message.payload);
      }
    });

    transport.onConnect(() => {
      this._status = 'connected';
      this.statusCallback?.(this._status);
    });

    transport.onDisconnect(() => {
      this._status = 'disconnected';
      this.statusCallback?.(this._status);
    });
  }

  connect(url: string): void {
    this.transport?.connect(url);
  }

  disconnect(): void {
    this.transport?.disconnect();
  }

  /**
   * Update a single image parameter locally and broadcast it via transport.
   */
  sendParameter<K extends keyof ImageParameters>(key: K, value: ImageParameters[K]): void {
    this.transport?.send({
      type: 'update',
      payload: { [key]: value },
    });
  }

  onStatusChange(callback: StatusCallback): void {
    this.statusCallback = callback;
  }

  get status(): ConnectionStatus {
    return this._status;
  }
}

/** Singleton instance shared across the application. */
export const parameterService = new ParameterService();
