const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

async function request(path, { method = 'GET', body } = {}) {
  const token = localStorage.getItem('token');
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    throw Object.assign(new Error('Cannot reach the server. Is the backend running?'), { status: 0 });
  }

  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }

  if (!res.ok) {
    // An expired/invalid token on a protected call: tell the app to sign the user out.
    if (res.status === 401 && token && !path.startsWith('/auth/login')) {
      window.dispatchEvent(new Event('auth:expired'));
    }
    const message = (data && data.message) || (typeof data === 'string' && data) || `Request failed (${res.status})`;
    throw Object.assign(new Error(message), { status: res.status, code: data && data.code });
  }
  return data;
}

const get = (p) => request(p);
const post = (p, body = {}) => request(p, { method: 'POST', body });
const put = (p, body = {}) => request(p, { method: 'PUT', body });
const del = (p) => request(p, { method: 'DELETE' });

// ---- Auth ----
export const login = (email, password) => post('/auth/login', { email, password });
export const registerPatient = (data) => post('/auth/patient-register', data);
export const verifyEmail = (email, code) => post('/auth/verify-email', { email, code });
export const resendCode = (email) => post('/auth/resend-code', { email });

// ---- Staff & directory ----
export const getStaff = () => get('/staff');
export const createStaff = (data) => post('/staff', data);
export const setStaffEnabled = (id, enabled) => put(`/staff/${id}/enabled`, { enabled });
export const getDoctors = (type) => get(`/doctors${type ? `?type=${type}` : ''}`);
export const getDoctorTypes = () => get('/doctors/types');
export const getNurses = () => get('/nurses');
export const createNurse = (data) => post('/nurses', data);
export const setNurseActive = (id, enabled) => put(`/nurses/${id}/active`, { enabled });
export const deleteNurse = (id) => del(`/nurses/${id}`);

// ---- Patients ----
export const getPatients = () => get('/patients');
export const createPatient = (data) => post('/patients', data);
export const updatePatient = (id, data) => put(`/patients/${id}`, data);
export const getMyCareTeam = () => get('/patients/me/care-team');

// ---- Appointments ----
export const getAppointments = (params = {}) => {
  const q = new URLSearchParams(Object.entries(params).filter(([, v]) => v)).toString();
  return get(`/appointments${q ? `?${q}` : ''}`);
};
export const getMyAppointments = () => get('/appointments/my');
export const getAppointmentTypes = () => get('/appointments/types');
export const createAppointment = (data) => post('/appointments', data);
export const updateAppointmentStatus = (id, status) => put(`/appointments/${id}/status`, { status });
export const cancelMyAppointment = (id) => put(`/appointments/${id}/cancel`);

// ---- Medical records ----
export const getRecordPatients = () => get('/medical-records/patients');
export const getPatientSummary = (id) => get(`/medical-records/patient/${id}/summary`);
export const createMedicalRecord = (data) => post('/medical-records', data);

// ---- Pharmacy ----
export const getCategories = () => get('/pharmacy/categories');
export const createCategory = (data) => post('/pharmacy/categories', data);
export const updateCategory = (id, data) => put(`/pharmacy/categories/${id}`, data);
export const deleteCategory = (id) => del(`/pharmacy/categories/${id}`);
export const getItems = (all = false) => get(`/pharmacy/items${all ? '?all=true' : ''}`);
export const createItem = (data) => post('/pharmacy/items', data);
export const updateItem = (id, data) => put(`/pharmacy/items/${id}`, data);
export const retireItem = (id) => del(`/pharmacy/items/${id}`);
export const restockItem = (id, quantity) => post(`/pharmacy/items/${id}/restock`, { quantity });
export const administerItem = (data) => post('/pharmacy/administrations', data);
export const getAdministrations = () => get('/pharmacy/administrations');

// ---- Wards ----
export const getWards = () => get('/wards');
export const createWard = (data) => post('/wards', data);
export const updateWard = (id, data) => put(`/wards/${id}`, data);
export const deleteWard = (id) => del(`/wards/${id}`);
export const getAdmissions = (status) => get(`/admissions${status ? `?status=${status}` : ''}`);
export const admitPatient = (data) => post('/admissions', data);
export const dischargePatient = (id, summary) => post(`/admissions/${id}/discharge`, { summary });

// ---- Payments ----
export const getPayments = () => get('/payments');
export const getMyPayments = () => get('/payments/my');
export const getBanks = () => get('/payments/banks');
export const createCharge = (data) => post('/payments', data);
export const payPayment = (id, data) => post(`/payments/${id}/pay`, data);
export const payCash = (id) => post(`/payments/${id}/cash`);
export const refundPayment = (id) => post(`/payments/${id}/refund`);
export const voidPayment = (id) => post(`/payments/${id}/void`);
export const getReceipt = (id) => get(`/payments/${id}/receipt`);

// ---- Reports ----
export const getReportSummary = () => get('/reports/summary');
export const getDoctorSummary = () => get('/reports/doctor');
