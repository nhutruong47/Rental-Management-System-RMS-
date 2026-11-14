export type Property = { id: string; name: string; address: string };
export type Room = {
  id: string;
  propertyId: string;
  roomCode: string;
  areaSquareMeters: number;
  capacity: number;
  monthlyRent: number;
  depositAmount: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'INACTIVE';
};
export type Tenant = { id: string; fullName: string; phone?: string; email?: string };
export type Contract = {
  id: string;
  roomId: string;
  tenantId: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  status: 'DRAFT' | 'ACTIVE' | 'TERMINATED';
};
export type Invoice = {
  id: string;
  contractId: string;
  roomId: string;
  tenantId: string;
  billingPeriod: string;
  rentAmount: number;
  electricityAmount: number;
  waterAmount: number;
  totalAmount: number;
  amountPaid: number;
  balanceDue: number;
  status: 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'VOID';
};
export type UtilityType = 'ELECTRICITY' | 'WATER';
export type Tariff = { id: string; utilityType: UtilityType; pricePerUnit: number; effectiveFrom: string };
export type MeterReading = {
  id: string;
  roomId: string;
  utilityType: UtilityType;
  billingPeriod: string;
  previousReading: number;
  currentReading: number;
  usage: number;
  unitPrice: number;
  charge: number;
};
export type SessionAccount = {
  userId: string;
  organizationId: string;
  organizationName: string;
  email: string;
  role: string;
};

let csrfToken = '';
let csrfHeaderName = 'X-XSRF-TOKEN';

function readCookie(name: string): string {
  if (typeof document === 'undefined') return '';
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : '';
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  const method = (init.method ?? 'GET').toUpperCase();
  const activeToken = readCookie('XSRF-TOKEN') || csrfToken;
  if (activeToken && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    headers.set(csrfHeaderName, activeToken);
  }
  const response = await fetch(path, { ...init, headers, credentials: 'same-origin' });
  if (response.status === 204) return undefined as T;
  const contentType = response.headers.get('content-type') ?? '';
  const payload = contentType.includes('application/json') ? await response.json() : await response.text();
  if (!response.ok) {
    const detail = typeof payload === 'string' ? payload : payload.error ?? payload.message ?? payload.detail;
    throw new Error(detail || `Yêu cầu thất bại (${response.status})`);
  }
  return payload as T;
}

export async function bootstrapCsrf(): Promise<void> {
  const payload = await request<{ token: string; headerName?: string }>('/api/auth/csrf');
  csrfToken = payload.token;
  if (payload.headerName) csrfHeaderName = payload.headerName;
}

export async function currentSession(): Promise<SessionAccount | null> {
  try {
    return await request<SessionAccount>('/api/auth/me');
  } catch {
    return null;
  }
}

export async function signIn(email: string, password: string): Promise<SessionAccount> {
  const account = await request<SessionAccount>('/api/auth/login', {
    method: 'POST', body: JSON.stringify({ email, password }),
  });
  await bootstrapCsrf();
  return account;
}

export async function registerAccount(email: string, password: string, fullName: string, organizationName: string) {
  return request<void>('/api/auth/register', {
    method: 'POST', body: JSON.stringify({ email, password, fullName, organizationName }),
  });
}

export async function signOut(): Promise<void> {
  await request<void>('/api/auth/logout', { method: 'POST' });
  await bootstrapCsrf();
}

export const api = {
  properties: () => request<Property[]>('/api/properties'),
  contracts: () => request<Contract[]>('/api/contracts'),
  createContract: (body: { roomId: string; tenantId: string; startDate: string; endDate: string }) => request<Contract>('/api/contracts', { method: 'POST', body: JSON.stringify(body) }),
  activateContract: (id: string) => request<Contract>(`/api/contracts/${id}/activate`, { method: 'POST' }),
  rooms: (propertyId: string) => request<Room[]>(`/api/properties/${propertyId}/rooms`),
  createProperty: (body: { name: string; address: string }) => request<Property>('/api/properties', { method: 'POST', body: JSON.stringify(body) }),
  createRoom: (propertyId: string, body: { roomCode: string; areaSquareMeters: number; capacity: number; monthlyRent: number; depositAmount: number }) => request<Room>(`/api/properties/${propertyId}/rooms`, { method: 'POST', body: JSON.stringify(body) }),
  tenants: () => request<Tenant[]>('/api/tenants'),
  createTenant: (body: { fullName: string; phone?: string; email?: string }) => request<Tenant>('/api/tenants', { method: 'POST', body: JSON.stringify(body) }),
  tariffs: () => request<Tariff[]>('/api/utility-tariffs'),
  createTariff: (body: { utilityType: UtilityType; pricePerUnit: number; effectiveFrom: string }) => request<Tariff>('/api/utility-tariffs', { method: 'POST', body: JSON.stringify(body) }),
  readings: (billingPeriod: string) => request<MeterReading[]>(`/api/meter-readings?billingPeriod=${billingPeriod}`),
  createReading: (body: { roomId: string; utilityType: UtilityType; billingPeriod: string; currentReading: number }) => request<MeterReading>('/api/meter-readings', { method: 'POST', body: JSON.stringify(body) }),
  invoices: (billingPeriod: string) => request<Invoice[]>(`/api/invoices?billingPeriod=${billingPeriod}`),
  generateInvoices: (billingPeriod: string) => request<Invoice[]>('/api/invoices/generate', { method: 'POST', body: JSON.stringify({ billingPeriod }) }),
  issueInvoice: (id: string) => request<Invoice>(`/api/invoices/${id}/issue`, { method: 'POST' }),
  recordPayment: (id: string, body: { amount: number; method: 'CASH' | 'BANK_TRANSFER'; reference: string; idempotencyKey: string }) => request(`/api/invoices/${id}/payments`, { method: 'POST', body: JSON.stringify(body) }),
};
