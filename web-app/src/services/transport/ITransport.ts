export interface ParameterMessage {
  type: 'update' | 'sync';
  payload: Partial<{
    gain: number;
    depth: number;
    zoom: number;
  }>;
}

export interface ITransport {
  /** Establish connection to the given endpoint URL/address. */
  connect(url: string): void;
  /** Gracefully close the connection. */
  disconnect(): void;
  /** Send a parameter message through the transport. */
  send(message: ParameterMessage): void;
  /** Register a callback for incoming messages. */
  onMessage(callback: (message: ParameterMessage) => void): void;
  /** Register a callback invoked when the connection is established. */
  onConnect(callback: () => void): void;
  /** Register a callback invoked when the connection is lost. */
  onDisconnect(callback: () => void): void;
  /** Returns true when the transport has an active connection. */
  isConnected(): boolean;
}
