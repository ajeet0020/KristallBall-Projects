import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Card, CardActionArea, CardContent, CircularProgress,
  Dialog, DialogActions, DialogContent, DialogTitle, Divider, FormControl,
  InputLabel, MenuItem, Paper, Select, Stack, TextField, Typography,
} from '@mui/material';
import { api, messageFromError } from './api.js';

const today = new Date().toISOString().slice(0, 10);
const monthStart = `${today.slice(0, 7)}-01`;
const initialFilters = { fromDate: monthStart, toDate: today, baseId: '', equipmentType: '' };

const metrics = [
  { key: 'openingBalance', label: 'Opening balance', color: '#425b52' },
  { key: 'closingBalance', label: 'Closing balance', color: '#245846' },
  { key: 'netMovement', label: 'Net movement', color: '#ad772a', clickable: true },
  { key: 'assigned', label: 'Assigned', color: '#496a93' },
  { key: 'expended', label: 'Expended', color: '#a14842' },
];

export default function DashboardPage({ user }) {
  const [bases, setBases] = useState([]);
  const [filters, setFilters] = useState(initialFilters);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [breakdownOpen, setBreakdownOpen] = useState(false);

  const loadData = useCallback(async (activeFilters) => {
    setLoading(true);
    setError('');
    try {
      const params = Object.fromEntries(Object.entries(activeFilters).filter(([, value]) => value !== ''));
      const [baseResponse, summaryResponse] = await Promise.all([
        api.get('/bases'),
        api.get('/dashboard', { params }),
      ]);
      setBases(baseResponse.data);
      setSummary(summaryResponse.data);
      if (user.role === 'BASE_COMMANDER' && baseResponse.data[0]) {
        setFilters((current) => ({ ...current, baseId: String(baseResponse.data[0].id) }));
      }
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setLoading(false);
    }
  }, [user.role]);

  useEffect(() => { loadData(initialFilters); }, [loadData]);

  const applyFilters = () => loadData(filters);
  const clearFilters = () => {
    const reset = { ...initialFilters, baseId: user.role === 'BASE_COMMANDER' && user.baseId ? String(user.baseId) : '' };
    setFilters(reset);
    loadData(reset);
  };

  const metricCard = (metric) => {
    const value = summary?.[metric.key] ?? 0;
    return (
      <Card key={metric.key} elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderTop: `4px solid ${metric.color}`, minWidth: 0 }}>
        {metric.clickable ? <CardActionArea onClick={() => setBreakdownOpen(true)} aria-label="Show net movement breakdown" sx={{ height: '100%' }}>
          <CardContent>
            <Typography variant="body2" color="text.secondary">{metric.label}</Typography>
            <Typography variant="h4" fontWeight={800} sx={{ mt: 1, color: metric.color }}>{value.toLocaleString()}</Typography>
            <Typography variant="caption" color="primary.main">Click to view breakdown</Typography>
          </CardContent>
        </CardActionArea> : <CardContent>
          <Typography variant="body2" color="text.secondary">{metric.label}</Typography>
          <Typography variant="h4" fontWeight={800} sx={{ mt: 1, color: metric.color }}>{value.toLocaleString()}</Typography>
        </CardContent>}
      </Card>
    );
  };

  return (
    <Stack spacing={2.5}>
      {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}

      <Paper elevation={0} sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider' }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 2.5 }}>
          <Box>
            <Typography variant="h6" fontWeight={750}>Inventory dashboard</Typography>
            <Typography variant="body2" color="text.secondary">Inventory movement and personnel allocation for the selected period.</Typography>
          </Box>
          {summary && <Typography variant="caption" color="text.secondary">{summary.fromDate} — {summary.toDate}</Typography>}
        </Stack>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: user.role === 'ADMIN' ? '1fr 1fr 1.2fr 1.2fr 1.2fr auto auto' : '1fr 1fr 1.2fr 1.2fr 1.2fr auto auto' }, gap: 1.25, alignItems: 'start' }}>
          {user.role === 'ADMIN' ? <FormControl fullWidth>
            <InputLabel id="dashboard-base-label">All bases</InputLabel>
            <Select labelId="dashboard-base-label" label="All bases" value={filters.baseId} onChange={(event) => setFilters({ ...filters, baseId: event.target.value })}>
              <MenuItem value="">All bases</MenuItem>
              {bases.map((base) => <MenuItem key={base.id} value={String(base.id)}>{base.name}</MenuItem>)}
            </Select>
          </FormControl> : <TextField label="Base" value={bases[0]?.name || 'Assigned base'} disabled />}
          <TextField label="From date" type="date" required InputLabelProps={{ shrink: true }} value={filters.fromDate}
            onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })} />
          <TextField label="To date" type="date" required InputLabelProps={{ shrink: true }} value={filters.toDate}
            onChange={(event) => setFilters({ ...filters, toDate: event.target.value })} />
          <TextField label="Equipment type" placeholder="All equipment" value={filters.equipmentType}
            inputProps={{ maxLength: 80 }} onChange={(event) => setFilters({ ...filters, equipmentType: event.target.value })} />
          <Button variant="contained" onClick={applyFilters} disabled={loading} sx={{ minHeight: 56 }}>
            {loading ? <CircularProgress size={22} color="inherit" /> : 'Apply filters'}
          </Button>
          <Button variant="text" onClick={clearFilters} sx={{ minHeight: 56 }}>Reset</Button>
        </Box>
      </Paper>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: 'repeat(5, minmax(0, 1fr))' }, gap: 1.5 }}>
        {metrics.map(metricCard)}
      </Box>

      <Dialog open={breakdownOpen} onClose={() => setBreakdownOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle fontWeight={750}>Net movement breakdown</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Purchases + transfer in − transfer out for {summary?.fromDate} through {summary?.toDate}.
          </Typography>
          <Stack spacing={1.5}>
            <BreakdownRow label="Purchases" value={summary?.purchases ?? 0} />
            <BreakdownRow label="Transfer in" value={summary?.transferIn ?? 0} />
            <BreakdownRow label="Transfer out" value={-(summary?.transferOut ?? 0)} />
            <Divider />
            <BreakdownRow label="Net movement" value={summary?.netMovement ?? 0} strong />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}><Button onClick={() => setBreakdownOpen(false)}>Close</Button></DialogActions>
      </Dialog>
    </Stack>
  );
}

function BreakdownRow({ label, value, strong = false }) {
  return <Stack direction="row" justifyContent="space-between" alignItems="center">
    <Typography fontWeight={strong ? 750 : 500}>{label}</Typography>
    <Typography fontWeight={strong ? 800 : 650}>{value.toLocaleString()}</Typography>
  </Stack>;
}
