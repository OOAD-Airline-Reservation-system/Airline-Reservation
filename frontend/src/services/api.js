import axios from 'axios';

const api = axios.create({ baseURL: '' });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const auth = {
  register: (data) => api.post('/auth/register', data),
  login:    (data) => api.post('/auth/login', data),
};

export const flights = {
  search: (params) => api.get('/api/flights/search', { params }),
};

export const seats = {
  getByFlight: (flightId) => api.get(`/api/seats/flight/${flightId}`),
};

export const bookings = {
  create:  (data) => api.post('/api/bookings', data),
  getOne:  (id)   => api.get(`/api/bookings/${id}`),
  getMine: ()     => api.get('/api/bookings/me'),
};

export const passengers = {
  save: (bookingId, list) => api.post(`/api/bookings/${bookingId}/passengers`, list),
  get:  (bookingId)       => api.get(`/api/bookings/${bookingId}/passengers`),
};

export const payments = {
  process: (data) => api.post('/api/payments', data),
};

export const tracking = {
  track: (flightNumber) => api.get(`/api/tracking/${flightNumber}`),
};

export const loyalty = {
  getAccount: ()            => api.get('/api/loyalty/me'),
  redeem:     (points, ref) => api.post('/api/loyalty/redeem', { points, bookingReference: ref }),
};

export const trips = {
  suggest: (params) => api.get('/api/trips/suggest', { params }),
};

export default api;
