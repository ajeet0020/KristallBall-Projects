import React from 'react';
import ReactDOM from 'react-dom/client';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import App from './App.jsx';
import './styles.css';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#245846', dark: '#17392d' },
    secondary: { main: '#ad772a' },
    background: { default: '#f3f5f2', paper: '#ffffff' },
  },
  shape: { borderRadius: 12 },
  typography: { fontFamily: 'Inter, ui-sans-serif, system-ui, sans-serif' },
});

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  </React.StrictMode>,
);
