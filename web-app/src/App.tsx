import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { ImageParametersPage } from './pages/ImageParametersPage';

const theme = createTheme({ palette: { mode: 'light' } });

// URLs available to the React app (injected at build time by vite-ws-plugin)
export const WS_LOCAL_URL = `ws://localhost:${__WS_PORT__}`;
export const WS_LAN_URL = `ws://${__WS_HOST__}:${__WS_PORT__}`;

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ImageParametersPage wsLanUrl={WS_LAN_URL} />
    </ThemeProvider>
  );
}

export default App;
