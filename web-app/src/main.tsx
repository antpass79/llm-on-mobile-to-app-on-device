import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { parameterService } from './services/ParameterService'
import { WebSocketTransport } from './services/transport/WebSocketTransport'

// Configure transport ONCE for the lifetime of the page (survives Fast Refresh).
parameterService.setTransport(new WebSocketTransport())
parameterService.connect(`ws://localhost:${__WS_PORT__}`)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
