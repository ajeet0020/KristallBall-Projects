import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert, AppBar, Box, Button, Chip, CircularProgress, Container, FormControl,
  InputLabel, MenuItem, Paper, Select, Snackbar, Stack, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Tab, Tabs, TextField, Toolbar, Typography,
} from '@mui/material';
import { api, messageFromError } from './api.js';
import TransfersPage from './TransfersPage.jsx';
import AssignmentsPage from './AssignmentsPage.jsx';
import DashboardPage from './DashboardPage.jsx';

const today = new Date().toISOString().slice(0, 10);
const emptyPurchase = () => ({ baseId: '', equipmentType: '', quantity: '1', date: today });
const emptyFilters = { baseId: '', fromDate: '', toDate: '', equipmentType: '' };

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem('accessToken') || '');
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [user, setUser] = useState(null);
  const [page, setPage] = useState('purchases');
  const [bases, setBases] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [filters, setFilters] = useState(emptyFilters);
  const [form, setForm] = useState(emptyPurchase());
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const canManage = useMemo(() => ['ADMIN', 'BASE_COMMANDER'].includes(user?.role), [user]);

  const loadData = useCallback(async (activeFilters = emptyFilters) => {
    setLoading(true);
    setError('');
    try {
      const params = Object.fromEntries(Object.entries(activeFilters).filter(([, value]) => value !== ''));
      const [baseResponse, purchaseResponse] = await Promise.all([
        api.get('/bases'),
        api.get('/purchases', { params }),
      ]);
      setBases(baseResponse.data);
      setPurchases(purchaseResponse.data);
      if (!editingId && baseResponse.data.length && !form.baseId) {
        setForm((current) => ({ ...current, baseId: String(baseResponse.data[0].id) }));
      }
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setLoading(false);
    }
  }, [editingId, form.baseId]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    api.get('/auth/me')
      .then(({ data }) => {
        if (active) {
          setUser(data);
          if (data.role === 'LOGISTICS_OFFICER') setPage('purchases');
        }
      })
      .catch(() => {
        localStorage.removeItem('accessToken');
        if (active) { setToken(''); setUser(null); }
      });
    return () => { active = false; };
  }, [token]);

  useEffect(() => {
    if (user) loadData(emptyFilters);
    // Load only when the authenticated user changes, not on form/filter state edits.
  }, [user]);

  const handleLogin = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      const { data } = await api.post('/auth/login', credentials);
      localStorage.setItem('accessToken', data.accessToken);
      setToken(data.accessToken);
      setCredentials((current) => ({ ...current, password: '' }));
    } catch (loginError) {
      setError(messageFromError(loginError));
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    setToken('');
    setUser(null);
    setPurchases([]);
    setBases([]);
    setEditingId(null);
    setForm(emptyPurchase());
  };

  const resetForm = () => {
    setEditingId(null);
    setForm({ ...emptyPurchase(), baseId: bases[0] ? String(bases[0].id) : '' });
  };

  const submitPurchase = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    const payload = {
      baseId: Number(form.baseId),
      equipmentType: form.equipmentType.trim(),
      quantity: Number(form.quantity),
      date: form.date,
    };
    try {
      if (editingId) await api.put(`/purchases/${editingId}`, payload);
      else await api.post('/purchases', payload);
      setNotice(editingId ? 'Purchase updated.' : 'Purchase recorded.');
      resetForm();
      await loadData(filters);
    } catch (saveError) {
      setError(messageFromError(saveError));
    } finally {
      setSaving(false);
    }
  };

  const editPurchase = (purchase) => {
    setEditingId(purchase.id);
    setForm({
      baseId: String(purchase.baseId),
      equipmentType: purchase.equipmentType,
      quantity: String(purchase.quantity),
      date: purchase.date,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const deletePurchase = async (purchase) => {
    if (!window.confirm(`Delete the ${purchase.equipmentType} purchase recorded on ${purchase.date}?`)) return;
    setError('');
    try {
      await api.delete(`/purchases/${purchase.id}`);
      setNotice('Purchase deleted.');
      await loadData(filters);
    } catch (deleteError) {
      setError(messageFromError(deleteError));
    }
  };

  if (!token || !user) {
    return (
      <Box className="page-shell">
        <Container maxWidth="sm">
          <Paper elevation={0} sx={{ p: { xs: 3, sm: 5 }, mt: { xs: 5, sm: 10 }, border: '1px solid', borderColor: 'divider' }}>
            <Typography variant="overline" color="primary.main" fontWeight={800}>MILITARY ASSET MANAGEMENT</Typography>
            <Typography variant="h4" fontWeight={750} sx={{ mt: 1, mb: 1 }}>Sign in</Typography>
            <Typography color="text.secondary" sx={{ mb: 3 }}>Use your authorized account to manage base purchases.</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            <Stack component="form" spacing={2} onSubmit={handleLogin}>
              <TextField label="Username" autoComplete="username" required value={credentials.username}
                onChange={(event) => setCredentials({ ...credentials, username: event.target.value })} />
              <TextField label="Password" type="password" autoComplete="current-password" required value={credentials.password}
                onChange={(event) => setCredentials({ ...credentials, password: event.target.value })} />
              <Button type="submit" variant="contained" size="large" disabled={saving}>
                {saving ? <CircularProgress size={22} color="inherit" /> : 'Sign in'}
              </Button>
            </Stack>
          </Paper>
        </Container>
      </Box>
    );
  }

  return (
    <>
      <AppBar position="static" elevation={0} color="primary">
        <Toolbar sx={{ justifyContent: 'space-between', gap: 2 }}>
          <Box>
            <Typography variant="overline" sx={{ opacity: 0.76, lineHeight: 1.2 }}>LOGISTICS OPERATIONS</Typography>
            <Typography variant="h6" fontWeight={750}>{page === 'dashboard' ? 'Dashboard' : page === 'purchases' ? 'Purchases' : page === 'transfers' ? 'Transfers' : 'Assignments & Expenditures'}</Typography>
          </Box>
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Box sx={{ textAlign: 'right', display: { xs: 'none', sm: 'block' } }}>
              <Typography variant="body2" fontWeight={700}>{user.username}</Typography>
              <Typography variant="caption" sx={{ opacity: 0.76 }}>{user.role.replaceAll('_', ' ')}</Typography>
            </Box>
            <Button color="inherit" variant="outlined" onClick={handleLogout}>Sign out</Button>
          </Stack>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" className="page-shell">
        <Paper elevation={0} sx={{ mb: 2.5, border: '1px solid', borderColor: 'divider' }}>
          <Tabs value={page} onChange={(_, value) => setPage(value)} aria-label="Asset operations" variant="fullWidth">
            {user.role !== 'LOGISTICS_OFFICER' && <Tab value="dashboard" label="Dashboard" />}
            <Tab value="purchases" label="Purchases" />
            <Tab value="transfers" label="Transfers" />
            {user.role !== 'LOGISTICS_OFFICER' && <Tab value="assignments" label="Assignments & Expenditures" />}
          </Tabs>
        </Paper>
        {page === 'dashboard' && user.role !== 'LOGISTICS_OFFICER' ? <DashboardPage user={user} /> : page === 'transfers' ? <TransfersPage user={user} /> : page === 'assignments' && user.role !== 'LOGISTICS_OFFICER' ? <AssignmentsPage user={user} /> :
        <Stack spacing={2.5}>
          {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}

          <Paper component="form" elevation={0} onSubmit={submitPurchase} sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider' }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 2.5 }}>
              <Box>
                <Typography variant="h6" fontWeight={750}>{editingId ? 'Edit purchase' : 'Record a purchase'}</Typography>
                <Typography variant="body2" color="text.secondary">Add a new equipment purchase to the selected base.</Typography>
              </Box>
              {editingId && <Button onClick={resetForm}>Cancel edit</Button>}
            </Stack>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: '1.2fr 1.2fr 0.8fr 1fr auto' }, gap: 1.5, alignItems: 'start' }}>
              <FormControl required fullWidth>
                <InputLabel id="purchase-base-label">Base</InputLabel>
                <Select labelId="purchase-base-label" label="Base" value={form.baseId} onChange={(event) => setForm({ ...form, baseId: event.target.value })}>
                  {bases.map((base) => <MenuItem key={base.id} value={String(base.id)}>{base.name}</MenuItem>)}
                </Select>
              </FormControl>
              <TextField label="Equipment type" placeholder="e.g. VEHICLE" required value={form.equipmentType}
                inputProps={{ maxLength: 80 }} onChange={(event) => setForm({ ...form, equipmentType: event.target.value })} />
              <TextField label="Quantity" type="number" required inputProps={{ min: 1, step: 1 }} value={form.quantity}
                onChange={(event) => setForm({ ...form, quantity: event.target.value })} />
              <TextField label="Purchase date" type="date" required InputLabelProps={{ shrink: true }} value={form.date}
                onChange={(event) => setForm({ ...form, date: event.target.value })} />
              <Button type="submit" variant="contained" size="large" disabled={saving || !bases.length} sx={{ minHeight: 56 }}>
                {saving ? <CircularProgress size={22} color="inherit" /> : editingId ? 'Save changes' : 'Record purchase'}
              </Button>
            </Box>
          </Paper>

          <Paper elevation={0} sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider' }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 2 }}>
              <Box>
                <Typography variant="h6" fontWeight={750}>Purchase history</Typography>
                <Typography variant="body2" color="text.secondary">Filter records by base, date range, or equipment type.</Typography>
              </Box>
              <Chip label={`${purchases.length} record${purchases.length === 1 ? '' : 's'}`} color="primary" variant="outlined" />
            </Stack>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: user.role === 'ADMIN' ? '1fr 1fr 1fr 1.2fr auto auto' : '1fr 1fr 1.2fr auto auto' }, gap: 1.25, mb: 2 }}>
              {user.role === 'ADMIN' && (
                <FormControl fullWidth>
                  <InputLabel id="filter-base-label">All bases</InputLabel>
                  <Select labelId="filter-base-label" label="All bases" value={filters.baseId} onChange={(event) => setFilters({ ...filters, baseId: event.target.value })}>
                    <MenuItem value="">All bases</MenuItem>
                    {bases.map((base) => <MenuItem key={base.id} value={String(base.id)}>{base.name}</MenuItem>)}
                  </Select>
                </FormControl>
              )}
              <TextField label="From date" type="date" InputLabelProps={{ shrink: true }} value={filters.fromDate}
                onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })} />
              <TextField label="To date" type="date" InputLabelProps={{ shrink: true }} value={filters.toDate}
                onChange={(event) => setFilters({ ...filters, toDate: event.target.value })} />
              <TextField label="Equipment type" placeholder="Any type" value={filters.equipmentType}
                onChange={(event) => setFilters({ ...filters, equipmentType: event.target.value })} />
              <Button variant="contained" onClick={() => loadData(filters)} disabled={loading} sx={{ minHeight: 56 }}>Apply</Button>
              <Button variant="text" onClick={() => { setFilters(emptyFilters); loadData(emptyFilters); }} sx={{ minHeight: 56 }}>Clear</Button>
            </Box>

            <TableContainer className="table-scroll">
              <Table size="small" aria-label="Purchase history">
                <TableHead><TableRow>
                  <TableCell>Date</TableCell><TableCell>Base</TableCell><TableCell>Equipment type</TableCell>
                  <TableCell align="right">Quantity</TableCell><TableCell>Recorded by</TableCell>
                  {canManage && <TableCell align="right">Actions</TableCell>}
                </TableRow></TableHead>
                <TableBody>
                  {loading ? <TableRow><TableCell colSpan={canManage ? 6 : 5} align="center" sx={{ py: 5 }}><CircularProgress size={26} /></TableCell></TableRow>
                    : purchases.length === 0 ? <TableRow><TableCell colSpan={canManage ? 6 : 5} align="center" sx={{ py: 5, color: 'text.secondary' }}>No purchases match these filters.</TableCell></TableRow>
                      : purchases.map((purchase) => <TableRow key={purchase.id} hover>
                        <TableCell>{purchase.date}</TableCell>
                        <TableCell>{purchase.baseName}</TableCell>
                        <TableCell><Chip size="small" label={purchase.equipmentType} variant="outlined" /></TableCell>
                        <TableCell align="right" sx={{ fontWeight: 700 }}>{purchase.quantity.toLocaleString()}</TableCell>
                        <TableCell>{purchase.createdBy}</TableCell>
                        {canManage && <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                          <Button size="small" onClick={() => editPurchase(purchase)}>Edit</Button>
                          <Button size="small" color="error" onClick={() => deletePurchase(purchase)}>Delete</Button>
                        </TableCell>}
                      </TableRow>)}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
          <Snackbar open={Boolean(notice)} autoHideDuration={3500} onClose={() => setNotice('')} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
            <Alert severity="success" variant="filled" onClose={() => setNotice('')}>{notice}</Alert>
          </Snackbar>
        </Stack>}
      </Container>
    </>
  );
}
