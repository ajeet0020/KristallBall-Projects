import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, CircularProgress, FormControl, InputLabel, MenuItem,
  Paper, Select, Snackbar, Stack, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import { api, messageFromError } from './api.js';

const today = new Date().toISOString().slice(0, 10);
const emptyFilters = { baseId: '', fromDate: '', toDate: '', equipmentType: '', personnel: '', expended: '' };
const emptyAssignment = () => ({ baseId: '', equipmentType: '', quantity: '1', assignedToPersonnel: '', date: today });

function formatTimestamp(timestamp) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(timestamp));
}

export default function AssignmentsPage({ user }) {
  const [bases, setBases] = useState([]);
  const [equipment, setEquipment] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [filters, setFilters] = useState(emptyFilters);
  const [form, setForm] = useState(emptyAssignment());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [expendingId, setExpendingId] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const loadData = useCallback(async (activeFilters = emptyFilters) => {
    setLoading(true);
    setError('');
    try {
      const params = Object.fromEntries(Object.entries(activeFilters).filter(([, value]) => value !== ''));
      const [baseResponse, assignmentResponse] = await Promise.all([
        api.get('/assignments/bases'),
        api.get('/assignments', { params }),
      ]);
      setBases(baseResponse.data);
      setAssignments(assignmentResponse.data);
      setForm((current) => {
        const selectedBaseExists = baseResponse.data.some((base) => String(base.id) === current.baseId);
        return { ...current, baseId: selectedBaseExists ? current.baseId : (baseResponse.data[0] ? String(baseResponse.data[0].id) : '') };
      });
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  const loadEquipment = useCallback(async (baseId) => {
    if (!baseId) { setEquipment([]); return; }
    try {
      const { data } = await api.get('/assignments/equipment', { params: { baseId } });
      setEquipment(data);
    } catch (requestError) {
      setError(messageFromError(requestError));
    }
  }, []);

  useEffect(() => { loadData(emptyFilters); }, [loadData]);
  useEffect(() => { loadEquipment(form.baseId); }, [form.baseId, loadEquipment]);

  const submitAssignment = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await api.post('/assignments', {
        baseId: Number(form.baseId),
        equipmentType: form.equipmentType,
        quantity: Number(form.quantity),
        assignedToPersonnel: form.assignedToPersonnel.trim(),
        date: form.date,
      });
      setNotice('Equipment assigned to personnel.');
      setForm((current) => ({ ...current, equipmentType: '', quantity: '1', assignedToPersonnel: '', date: today }));
      await Promise.all([loadData(filters), loadEquipment(form.baseId)]);
    } catch (requestError) {
      setError(messageFromError(requestError));
      await loadEquipment(form.baseId);
    } finally {
      setSaving(false);
    }
  };

  const markExpended = async (assignment) => {
    setExpendingId(assignment.id);
    setError('');
    try {
      await api.post(`/assignments/${assignment.id}/expend`);
      setNotice(`${assignment.equipmentType} marked as expended.`);
      await loadData(filters);
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setExpendingId(null);
    }
  };

  const resetFilters = () => {
    setFilters(emptyFilters);
    loadData(emptyFilters);
  };

  const selectedEquipment = equipment.find((item) => item.type === form.equipmentType);
  const availableEquipment = equipment.filter((item) => item.availableQuantity > 0 && item.status !== 'UNAVAILABLE');

  return (
    <Stack spacing={2.5}>
      {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}

      <Paper component="form" elevation={0} onSubmit={submitAssignment} sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider' }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 2.5 }}>
          <Box>
            <Typography variant="h6" fontWeight={750}>Assign equipment</Typography>
            <Typography variant="body2" color="text.secondary">Assignments reserve available base stock for named personnel.</Typography>
          </Box>
          <Chip label="Expended is tracked separately" color="primary" variant="outlined" />
        </Stack>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: '1fr 1.3fr 1fr 0.7fr 1fr auto' }, gap: 1.5, alignItems: 'start' }}>
          <FormControl fullWidth required>
            <InputLabel id="assignment-base-label">Base</InputLabel>
            <Select labelId="assignment-base-label" label="Base" value={form.baseId}
              onChange={(event) => setForm({ ...form, baseId: event.target.value, equipmentType: '' })}>
              {bases.map((base) => <MenuItem key={base.id} value={String(base.id)}>{base.name}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl fullWidth required>
            <InputLabel id="assignment-equipment-label">Equipment</InputLabel>
            <Select labelId="assignment-equipment-label" label="Equipment" value={form.equipmentType}
              onChange={(event) => setForm({ ...form, equipmentType: event.target.value })}>
              {availableEquipment.map((item) => <MenuItem key={item.id} value={item.type}>
                {item.name} · {item.type} · {item.availableQuantity.toLocaleString()} available
              </MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="Assigned to personnel" required value={form.assignedToPersonnel} inputProps={{ maxLength: 160 }}
            onChange={(event) => setForm({ ...form, assignedToPersonnel: event.target.value })} />
          <TextField label="Quantity" type="number" required inputProps={{ min: 1, max: selectedEquipment?.availableQuantity || undefined, step: 1 }} value={form.quantity}
            helperText={selectedEquipment ? `${selectedEquipment.availableQuantity.toLocaleString()} available` : 'Select equipment first'}
            onChange={(event) => setForm({ ...form, quantity: event.target.value })} />
          <TextField label="Assignment date" type="date" required InputLabelProps={{ shrink: true }} value={form.date}
            onChange={(event) => setForm({ ...form, date: event.target.value })} />
          <Button type="submit" variant="contained" size="large" disabled={saving || !form.baseId || !selectedEquipment || Number(form.quantity) > selectedEquipment.availableQuantity} sx={{ minHeight: 56 }}>
            {saving ? <CircularProgress size={22} color="inherit" /> : 'Assign equipment'}
          </Button>
        </Box>
      </Paper>

      <Paper elevation={0} sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider' }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 2 }}>
          <Box>
            <Typography variant="h6" fontWeight={750}>Assignment & expenditure history</Typography>
            <Typography variant="body2" color="text.secondary">Assignments are scoped to your base. Mark an item expended when it is consumed.</Typography>
          </Box>
          <Chip label={`${assignments.length} record${assignments.length === 1 ? '' : 's'}`} color="primary" variant="outlined" />
        </Stack>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: user.role === 'ADMIN' ? '1fr 1fr 1fr 1.1fr 1.1fr 1fr auto auto' : '1fr 1fr 1.1fr 1.1fr 1fr auto auto' }, gap: 1.25, mb: 2 }}>
          {user.role === 'ADMIN' && <FormControl fullWidth>
            <InputLabel id="assignment-filter-base-label">Any base</InputLabel>
            <Select labelId="assignment-filter-base-label" label="Any base" value={filters.baseId} onChange={(event) => setFilters({ ...filters, baseId: event.target.value })}>
              <MenuItem value="">Any base</MenuItem>
              {bases.map((base) => <MenuItem key={base.id} value={String(base.id)}>{base.name}</MenuItem>)}
            </Select>
          </FormControl>}
          <TextField label="From date" type="date" InputLabelProps={{ shrink: true }} value={filters.fromDate} onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })} />
          <TextField label="To date" type="date" InputLabelProps={{ shrink: true }} value={filters.toDate} onChange={(event) => setFilters({ ...filters, toDate: event.target.value })} />
          <TextField label="Equipment type" value={filters.equipmentType} onChange={(event) => setFilters({ ...filters, equipmentType: event.target.value })} />
          <TextField label="Personnel" value={filters.personnel} onChange={(event) => setFilters({ ...filters, personnel: event.target.value })} />
          <FormControl fullWidth>
            <InputLabel id="assignment-expended-filter-label">Any expenditure</InputLabel>
            <Select labelId="assignment-expended-filter-label" label="Any expenditure" value={filters.expended}
              onChange={(event) => setFilters({ ...filters, expended: event.target.value })}>
              <MenuItem value="">Assigned & expended</MenuItem><MenuItem value="false">Assigned</MenuItem><MenuItem value="true">Expended</MenuItem>
            </Select>
          </FormControl>
          <Button variant="contained" onClick={() => loadData(filters)} disabled={loading} sx={{ minHeight: 56 }}>Apply</Button>
          <Button variant="text" onClick={resetFilters} sx={{ minHeight: 56 }}>Clear</Button>
        </Box>

        <TableContainer sx={{ width: '100%', overflowX: 'auto' }}>
          <Table size="small" aria-label="Assignment and expenditure history" sx={{ minWidth: 820 }}>
            <TableHead><TableRow>
              <TableCell>Assignment date</TableCell><TableCell>Recorded at</TableCell><TableCell>Base</TableCell>
              <TableCell>Equipment</TableCell><TableCell>Assigned to</TableCell><TableCell align="right">Quantity</TableCell>
              <TableCell>Status</TableCell><TableCell>Action</TableCell>
            </TableRow></TableHead>
            <TableBody>
              {loading ? <TableRow><TableCell colSpan={8} align="center" sx={{ py: 5 }}><CircularProgress size={26} /></TableCell></TableRow>
                : assignments.length === 0 ? <TableRow><TableCell colSpan={8} align="center" sx={{ py: 5, color: 'text.secondary' }}>No assignments match these filters.</TableCell></TableRow>
                  : assignments.map((assignment) => <TableRow key={assignment.id} hover>
                    <TableCell>{assignment.date}</TableCell><TableCell>{formatTimestamp(assignment.createdAt)}</TableCell>
                    <TableCell>{assignment.baseName}</TableCell><TableCell>{assignment.equipmentType}</TableCell>
                    <TableCell>{assignment.assignedToPersonnel}</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 700 }}>{assignment.quantity.toLocaleString()}</TableCell>
                    <TableCell><Chip size="small" color={assignment.expended ? 'success' : 'info'} label={assignment.expended ? 'Expended' : 'Assigned'} /></TableCell>
                    <TableCell>{assignment.expended ? <Typography variant="caption" color="text.secondary">Complete</Typography> :
                      <Button size="small" color="secondary" disabled={expendingId === assignment.id} onClick={() => markExpended(assignment)}>
                        {expendingId === assignment.id ? <CircularProgress size={18} /> : 'Mark expended'}
                      </Button>}</TableCell>
                  </TableRow>)}
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
