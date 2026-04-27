import { useEffect, useState } from 'react';
import {
  Box,
  Container,
  Paper,
  Slider,
  Stack,
  Typography,
  Chip,
  Divider,
  IconButton,
  Tooltip,
  Alert,
} from '@mui/material';
import WifiIcon from '@mui/icons-material/Wifi';
import WifiOffIcon from '@mui/icons-material/WifiOff';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckIcon from '@mui/icons-material/Check';
import { useImageParametersStore } from '../store/imageParametersStore';
import { parameterService, type ConnectionStatus } from '../services/ParameterService';

const PARAMETER_CONFIG = [
  { key: 'gain' as const, label: 'Gain', min: 0, max: 100, step: 1, unit: 'dB' },
  { key: 'depth' as const, label: 'Depth', min: 10, max: 300, step: 5, unit: 'mm' },
  { key: 'zoom' as const, label: 'Zoom', min: 1, max: 10, step: 0.5, unit: 'x' },
];

interface Props {
  wsLanUrl: string;
}

export function ImageParametersPage({ wsLanUrl }: Props) {
  const { gain, depth, zoom, setGain, setDepth, setZoom } = useImageParametersStore();
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('disconnected');
  const [copied, setCopied] = useState(false);

  const setters = { gain: setGain, depth: setDepth, zoom: setZoom };
  const values = { gain, depth, zoom };

  useEffect(() => {
    parameterService.onStatusChange(setConnectionStatus);
  }, []);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(wsLanUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSliderChange = (key: 'gain' | 'depth' | 'zoom', value: number) => {
    setters[key](value);
    parameterService.sendParameter(key, value);
  };

  const isConnected = connectionStatus === 'connected';

  return (
    <Container maxWidth="sm" sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>
        Image Parameters
      </Typography>

      {/* Server URL Banner */}
      <Paper
        variant="outlined"
        sx={{
          p: 2,
          mb: 3,
          borderColor: isConnected ? 'success.main' : 'grey.400',
          bgcolor: isConnected ? 'success.50' : undefined,
        }}
      >
        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
          <Chip
            icon={isConnected ? <WifiIcon /> : <WifiOffIcon />}
            label={isConnected ? 'Server running' : 'Connecting…'}
            color={isConnected ? 'success' : 'default'}
            size="small"
          />
          <Typography variant="caption" color="text.secondary">
            WebSocket server
          </Typography>
        </Stack>

        <Stack direction="row" alignItems="center" spacing={1}>
          <Typography
            variant="body1"
            fontFamily="monospace"
            sx={{ flexGrow: 1, wordBreak: 'break-all' }}
          >
            {wsLanUrl}
          </Typography>
          <Tooltip title={copied ? 'Copied!' : 'Copy URL'}>
            <IconButton size="small" onClick={handleCopy}>
              {copied ? <CheckIcon fontSize="small" color="success" /> : <ContentCopyIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
        </Stack>

        {isConnected && (
          <Alert severity="info" sx={{ mt: 1.5, py: 0 }}>
            External clients can connect to the URL above to read and update parameters.
          </Alert>
        )}
      </Paper>

      {/* Parameter Controls */}
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
          Parameters
        </Typography>
        <Stack spacing={3} divider={<Divider flexItem />}>
          {PARAMETER_CONFIG.map(({ key, label, min, max, step, unit }) => (
            <Box key={key}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                <Typography variant="body1">{label}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {values[key]}
                  {unit}
                </Typography>
              </Stack>
              <Slider
                value={values[key]}
                min={min}
                max={max}
                step={step}
                onChange={(_, value) => handleSliderChange(key, value as number)}
                valueLabelDisplay="auto"
                valueLabelFormat={(v) => `${v}${unit}`}
              />
            </Box>
          ))}
        </Stack>
      </Paper>
    </Container>
  );
}
