import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, CircularProgress, FormControl, InputLabel, MenuItem,
  Paper, Select, Snackbar, Stack, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import { api, messageFromError } from './api.js';

const today = new Date().toISOString().slice(0, 10);
const emptyFilters = { baseId: '', fromDate: '', toDate: '', equipmentType: '', status: '' };
const statusTransitions = {
  PENDING: ['IN_TRANSIT', 'REJECTED'],
  IN_TRANSIT: ['COMPLETED', 'REJECTED'],
  COMPLETED: [],
  REJECTED: [],
};

function prettyStatus(status) {
  return status.replaceAll('_', ' ');
}

function formatTimestamp(timestamp) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(timestamp));
}

export default function TransfersPage({ user }) {
  const [bases, setBases] = useState([]);
  const [transfers, setTransfers] = useState([]);
  const [filters, setFilters] = useState(emptyFilters);
  const [form, setForm] = useState({ fromBaseId: '', toBaseId: '', equipmentType: '', quantity: '1', date: today });
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [updatingId, setUpdatingId] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const canUpdateStatus = ['ADMIN', 'BASE_COMMANDER'].includes(user.role);
  const sourceBases = user.role === 'ADMIN' ? bases : bases.filter((base) => base.id === user.baseId);

  const loadData = useCallback(async (activeFilters = emptyFilters) => {
    setLoading(true);
    setError('');
    try {
      const params = Object.fromEntries(Object.entries(activeFilters).filter(([, value]) => value !== ''));
      const [baseResponse, transferResponse] = await Promise.all([
        api.get('/transfers/bases'),
        api.get('/transfers', { params }),
      ]);
      setBases(baseResponse.data);
      setTransfers(transferResponse.data);
      setForm((current) => {
        const allowedSources = user.role === 'ADMIN' ? baseResponse.data : baseResponse.data.filter((base) => base.id === user.baseId);
        const sourceValid = allowedSources.some((base) => String(base.id) === current.fromBaseId);
        const destinationValid = baseResponse.data.some((base) => String(base.id) === current.toBaseId);
        const defaultSource = allowedSources[0] ? String(allowedSources[0].id) : '';
        const defaultDestination = baseResponse.data.find((base) => String(base.id) !== defaultSource);
        return {
          ...current,
          fromBaseId: sourceValid ? current.fromBaseId : defaultSource,
          toBaseId: destinationValid && current.toBaseId !== (sourceValid ? current.fromBaseId : defaultSource)
            ? current.toBaseId : (defaultDestination ? String(defaultDestination.id) : ''),
        };
      });
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setLoading(false);
    }
  }, [user.baseId, user.role]);

  useEffect(() => { loadData(emptyFilters); }, [loadData]);

  const submitTransfer = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await api.post('/transfers', {
        fromBaseId: Number(form.fromBaseId),
        toBaseId: Number(form.toBaseId),
        equipmentType: form.equipmentType.trim(),
        quantity: Number(form.quantity),
        date: form.date,
      });
      setNotice('Transfer initiated with Pending status.');
      setForm((current) => ({ ...current, equipmentType: '', quantity: '1', date: today }));
      await loadData(filters);
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setSaving(false);
    }
  };

  const updateStatus = async (transfer, status) => {
    setUpdatingId(transfer.id);
    setError('');
    try {
      await api.patch(`/transfers/${transfer.id}/status`, { status });
      setNotice(`Transfer status updated to ${prettyStatus(status)}.`);
      await loadData(filters);
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setUpdatingId(null);
    }
  };

  const resetFilters = () => {
    setFilters(emptyFilters);
    loadData(emptyFilters);
  };

  const getStatusColor = (status) => ({ PENDING: 'warning', IN_TRANSIT: 'info', COMPLETED: 'success', REJECTED: 'error' }[status] || 'default');

  return (
    <Stack spacing={2.5}>
      {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}

      <Paper component="form" elevation={0} onSubmit={submitTransfer} sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider' }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 2.5 }}>
          <Box>
            <Typography variant="h6" fontWeight={750}>Initiate a transfer</Typography>
            <Typography variant="body2" color="text.secondary">New transfers start as Pending and are recorded in the audit log.</Typography>
          </Box>
          <Chip label="Initial status · Pending" color="warning" variant="outlined" />
        </Stack>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: '1fr 1fr 1.15fr 0.7fr 1fr auto' }, gap: 1.5, alignItems: 'start' }}>
          <FormControl fullWidth required>
            <InputLabel id="from-base-label">From base</InputLabel>
            <Select labelId="from-base-label" label="From base" value={form.fromBaseId}
              onChange={(event) => setForm((current) => ({ ...current, fromBaseId: event.target.value,
                toBaseId: current.toBaseId === event.target.value ? (bases.find((base) => String(base.id) !== event.target.value) ? String(bases.find((base) => String(base.id) !== event.target.value).id) : '') : current.toBaseId }))}>
              {sourceBases.map((base) => <MenuItem key={base.id} value={String(base.id)}>{base.name}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl fullWidth required>
            <InputLabel id="to-base-label">To base</InputLabel>
            <Select labelId="to-base-label" label="To base" value={form.toBaseId} onChange={(event) => setForm({ ...form, toBaseId: event.target.value })}>
              {bases.filter((base) => String(base.id) !== form.fromBaseId).map((base) => <MenuItem key={base.id} value={String(base.id)}>{base.name}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="Equipment type" placeholder="e.g. VEHICLE" required value={form.equipmentType}
            inputProps={{ maxLength: 80 }} onChange={(event) => setForm({ ...form, equipmentType: event.target.value })} />
          <TextField label="Quantity" type="number" required inputProps={{ min: 1, step: 1 }} value={form.quantity}
            onChange={(event) => setForm({ ...form, quantity: event.target.value })} />
          <TextField label="Transfer date" type="date" required InputLabelProps={{ shrink: true }} value={form.date}
            onChange={(event) => setForm({ ...form, date: event.target.value })} />
          <Button type="submit" variant="contained" size="large" disabled={saving || !form.fromBaseId || !form.toBaseId || !bases.length} sx={{ minHeight: 56 }}>
            {saving ? <CircularProgress size={22} color="inherit" /> : 'Initiate transfer'}
          </Button>
        </Box>
      </Paper>

      <Paper elevation={0} sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider' }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 2 }}>
          <Box>
            <Typography variant="h6" fontWeight={750}>Transfer history</Typography>
            <Typography variant="body2" color="text.secondary">Transfers involving your base are included in the history.</Typography>
          </Box>
          <Chip label={`${transfers.length} record${transfers.length === 1 ? '' : 's'}`} color="primary" variant="outlined" />
        </Stack>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: user.role === 'ADMIN' ? '1fr 1fr 1fr 1fr 1.1fr auto auto' : '1fr 1fr 1fr 1.1fr auto auto' }, gap: 1.25, mb: 2 }}>
          {user.role === 'ADMIN' && <FormControl fullWidth>
            <InputLabel id="transfer-filter-base-label">Any base</InputLabel>
            <Select labelId="transfer-filter-base-label" label="Any base" value={filters.baseId} onChange={(event) => setFilters({ ...filters, baseId: event.target.value })}>
              <MenuItem value="">Any base</MenuItem>
              {bases.map((base) => <MenuItem key={base.id} value={String(base.id)}>{base.name}</MenuItem>)}
            </Select>
          </FormControl>}
          <TextField label="From date" type="date" InputLabelProps={{ shrink: true }} value={filters.fromDate}
            onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })} />
          <TextField label="To date" type="date" InputLabelProps={{ shrink: true }} value={filters.toDate}
            onChange={(event) => setFilters({ ...filters, toDate: event.target.value })} />
          <TextField label="Equipment type" value={filters.equipmentType} onChange={(event) => setFilters({ ...filters, equipmentType: event.target.value })} />
          <FormControl fullWidth>
            <InputLabel id="transfer-status-filter-label">Any status</InputLabel>
            <Select labelId="transfer-status-filter-label" label="Any status" value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
              <MenuItem value="">Any status</MenuItem>
              {Object.keys(statusTransitions).map((status) => <MenuItem key={status} value={status}>{prettyStatus(status)}</MenuItem>)}
            </Select>
          </FormControl>
          <Button variant="contained" onClick={() => loadData(filters)} disabled={loading} sx={{ minHeight: 56 }}>Apply</Button>
          <Button variant="text" onClick={resetFilters} sx={{ minHeight: 56 }}>Clear</Button>
        </Box>

        <TableContainer sx={{ width: '100%', overflowX: 'auto' }}>
          <Table size="small" aria-label="Transfer history" sx={{ minWidth: 900 }}>
            <TableHead><TableRow>
              <TableCell>Transfer date</TableCell><TableCell>Recorded at</TableCell><TableCell>Route</TableCell>
              <TableCell>Asset</TableCell><TableCell align="right">Quantity</TableCell><TableCell>Status</TableCell>
              <TableCell>Initiated by</TableCell>{canUpdateStatus && <TableCell>Update status</TableCell>}
            </TableRow></TableHead>
            <TableBody>
              {loading ? <TableRow><TableCell colSpan={canUpdateStatus ? 8 : 7} align="center" sx={{ py: 5 }}><CircularProgress size={26} /></TableCell></TableRow>
                : transfers.length === 0 ? <TableRow><TableCell colSpan={canUpdateStatus ? 8 : 7} align="center" sx={{ py: 5, color: 'text.secondary' }}>No transfers match these filters.</TableCell></TableRow>
                  : transfers.map((transfer) => {
                    const nextStatuses = statusTransitions[transfer.status];
                    return <TableRow key={transfer.id} hover>
                      <TableCell>{transfer.date}</TableCell>
                      <TableCell>{formatTimestamp(transfer.createdAt)}</TableCell>
                      <TableCell>{transfer.fromBaseName} <Box component="span" sx={{ color: 'text.secondary', px: 0.5 }}>→</Box> {transfer.toBaseName}</TableCell>
                      <TableCell><Chip size="small" label={transfer.equipmentType} variant="outlined" /></TableCell>
                      <TableCell align="right" sx={{ fontWeight: 700 }}>{transfer.quantity.toLocaleString()}</TableCell>
                      <TableCell><Chip size="small" color={getStatusColor(transfer.status)} label={prettyStatus(transfer.status)} /></TableCell>
                      <TableCell>{transfer.createdBy}</TableCell>
                      {canUpdateStatus && <TableCell>
                        {nextStatuses.length ? <FormControl size="small" sx={{ minWidth: 140 }}>
                          <InputLabel id={`status-label-${transfer.id}`}>Advance</InputLabel>
                          <Select labelId={`status-label-${transfer.id}`} label="Advance" value="" disabled={updatingId === transfer.id}
                            onChange={(event) => updateStatus(transfer, event.target.value)}>
                            {nextStatuses.map((status) => <MenuItem key={status} value={status}>{prettyStatus(status)}</MenuItem>)}
                          </Select>
                        </FormControl> : <Typography variant="caption" color="text.secondary">Final status</Typography>}
                      </TableCell>}
                    </TableRow>;
                  })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
      <Snackbar open={Boolean(notice)} autoHideDuration={3500} onClose={() => setNotice('')} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="success" variant="filled" onClose={() => setNotice('')}>{notice}</Alert>
      </Snackbar>
    </Stack>
  );
}
