import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from './App';
import { api, bootstrapCsrf, currentSession, signIn } from './api';
import type { Contract, Invoice, MaintenanceTicket, MeterReading, Property, Room, Tariff, Tenant } from './api';

vi.mock('./api', () => ({
  api: {
    properties: vi.fn(), contracts: vi.fn(), createContract: vi.fn(), activateContract: vi.fn(),
    terminateContract: vi.fn(), handover: vi.fn(), updateRoomStatus: vi.fn(),
    rooms: vi.fn(), tenants: vi.fn(), invoices: vi.fn(), tariffs: vi.fn(), readings: vi.fn(),
    maintenanceTickets: vi.fn(), createTicket: vi.fn(), assignTicket: vi.fn(), updateTicketStatus: vi.fn(),
    notifications: vi.fn(), markNotificationRead: vi.fn(),
  },
  bootstrapCsrf: vi.fn(), currentSession: vi.fn(), registerAccount: vi.fn(), signIn: vi.fn(), signOut: vi.fn(),
}));

const contract: Contract = {
  id: 'contract-1', roomId: 'room-1', tenantId: 'tenant-1', startDate: '2026-11-01',
  endDate: '2027-10-31', monthlyRent: 3500000, depositAmount: 3500000, status: 'DRAFT',
};
const properties: Property[] = [{ id: 'property-1', name: 'Nhà An Bình', address: 'Quận 3, TP. Hồ Chí Minh' }];
const rooms: Room[] = [{ id: 'room-1', propertyId: 'property-1', roomCode: 'P101', areaSquareMeters: 20, capacity: 2, monthlyRent: 3500000, depositAmount: 3500000, status: 'AVAILABLE' }];
const tenants: Tenant[] = [{ id: 'tenant-1', fullName: 'Nguyễn Minh Anh', phone: '0900000000', email: 'anh@example.test' }];
const tariffs: Tariff[] = [{ id: 'tariff-1', utilityType: 'ELECTRICITY', pricePerUnit: 3500, effectiveFrom: '2026-11-01' }];
const readings: MeterReading[] = [{
  id: 'reading-1', roomId: 'room-1', utilityType: 'ELECTRICITY', billingPeriod: '2026-11',
  previousReading: 100, currentReading: 150, usage: 50, unitPrice: 3500, charge: 175000,
}];
const ticket: MaintenanceTicket = {
  id: 'ticket-1', roomId: 'room-1', title: 'Rò rỉ vòi nước', description: 'Vòi lavabo nhỏ giọt',
  priority: 'HIGH', status: 'OPEN', estimatedCost: 150000, actualCost: 0,
  createdAt: '2026-11-14T00:00:00Z', updatedAt: '2026-11-14T00:00:00Z',
};

async function openWorkspace(initialContracts: Contract[] = [contract]) {
  vi.mocked(bootstrapCsrf).mockResolvedValue(undefined);
  vi.mocked(currentSession).mockResolvedValue(null);
  vi.mocked(signIn).mockResolvedValue({
    userId: 'user-1', organizationId: 'org-1', organizationName: 'Nhà Trọ Minh Anh',
    email: 'owner@example.test', role: 'OWNER',
  });
  vi.mocked(api.properties).mockResolvedValue(properties);
  vi.mocked(api.rooms).mockResolvedValue(rooms);
  vi.mocked(api.tenants).mockResolvedValue(tenants);
  vi.mocked(api.invoices).mockResolvedValue([] as Invoice[]);
  vi.mocked(api.tariffs).mockResolvedValue(tariffs);
  vi.mocked(api.readings).mockResolvedValue(readings);
  vi.mocked(api.contracts).mockResolvedValue(initialContracts);
  vi.mocked(api.maintenanceTickets).mockResolvedValue([ticket]);
  vi.mocked(api.notifications).mockResolvedValue([]);
  render(<App />);
  await waitFor(() => expect(bootstrapCsrf).toHaveBeenCalled());
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'owner@example.test' } });
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'a-secure-password' } });
  fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
  await screen.findByRole('heading', { name: 'Chào buổi sáng 👋' });
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('RMS owner workspace', () => {
  it('shows the organization name, contracts, and lets the owner activate a draft', async () => {
    await openWorkspace();
    expect(screen.getAllByText('Nhà Trọ Minh Anh').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: 'Hợp đồng' }));
    expect(await screen.findByText('Nguyễn Minh Anh')).toBeInTheDocument();
    expect(screen.getByText('P101')).toBeInTheDocument();
    expect(screen.getByText('Bản nháp')).toBeInTheDocument();

    vi.mocked(api.activateContract).mockResolvedValue({ ...contract, status: 'ACTIVE' });
    vi.mocked(api.contracts).mockResolvedValue([{ ...contract, status: 'ACTIVE' }]);
    fireEvent.click(screen.getByRole('button', { name: 'Kích hoạt' }));

    await waitFor(() => expect(api.activateContract).toHaveBeenCalledWith('contract-1'));
    expect(await screen.findByText('Đang hiệu lực')).toBeInTheDocument();
  });

  it('displays utility tariffs and recorded meter readings for the selected period', async () => {
    await openWorkspace();
    fireEvent.click(screen.getByRole('button', { name: 'Điện & nước' }));
    expect(await screen.findByText('Đơn giá đang áp dụng')).toBeInTheDocument();
    expect(screen.getByText('P101')).toBeInTheDocument();
    expect(screen.getByText('150')).toBeInTheDocument();
    expect(screen.getByText('50')).toBeInTheDocument();
  });

  it('displays maintenance tickets and lets the owner assign a technician', async () => {
    await openWorkspace();
    fireEvent.click(screen.getByRole('button', { name: /Bảo trì/ }));
    expect(await screen.findByText('Rò rỉ vòi nước')).toBeInTheDocument();

    vi.mocked(api.assignTicket).mockResolvedValue({ ...ticket, status: 'ASSIGNED', assigneeName: 'Anh Tùng' });
    vi.mocked(api.maintenanceTickets).mockResolvedValue([{ ...ticket, status: 'ASSIGNED', assigneeName: 'Anh Tùng' }]);
    fireEvent.click(screen.getByRole('button', { name: 'Phân công' }));
    fireEvent.change(screen.getByLabelText('Nhân viên / đơn vị phụ trách'), { target: { value: 'Anh Tùng' } });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận phân công' }));

    await waitFor(() => expect(api.assignTicket).toHaveBeenCalledWith('ticket-1', 'Anh Tùng'));
    expect(await screen.findByText('Anh Tùng')).toBeInTheDocument();
  });
});
