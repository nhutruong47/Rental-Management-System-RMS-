import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from './App';
import { api, bootstrapCsrf, changePassword, currentSession, signIn } from './api';
import type {
  AuditLog,
  Contract,
  Invoice,
  MaintenanceTicket,
  Member,
  MeterReading,
  OrganizationSettings,
  PortalOverview,
  Property,
  Room,
  Tariff,
  Tenant,
} from './api';

vi.mock('./api', () => ({
  api: {
    properties: vi.fn(),
    contracts: vi.fn(),
    createContract: vi.fn(),
    activateContract: vi.fn(),
    terminateContract: vi.fn(),
    handover: vi.fn(),
    updateRoomStatus: vi.fn(),
    rooms: vi.fn(),
    tenants: vi.fn(),
    provisionTenantPortal: vi.fn(),
    invoices: vi.fn(),
    adjustInvoice: vi.fn(),
    issueInvoice: vi.fn(),
    tariffs: vi.fn(),
    readings: vi.fn(),
    maintenanceTickets: vi.fn(),
    createTicket: vi.fn(),
    assignTicket: vi.fn(),
    updateTicketStatus: vi.fn(),
    notifications: vi.fn(),
    markNotificationRead: vi.fn(),
    members: vi.fn(),
    inviteMember: vi.fn(),
    updateMember: vi.fn(),
    settings: vi.fn(),
    updateSettings: vi.fn(),
    auditLogs: vi.fn(),
    tenantPortalOverview: vi.fn(),
    tenantPortalInvoices: vi.fn(),
    tenantPortalTickets: vi.fn(),
    createTenantPortalTicket: vi.fn(),
  },
  bootstrapCsrf: vi.fn(),
  currentSession: vi.fn(),
  registerAccount: vi.fn(),
  signIn: vi.fn(),
  signOut: vi.fn(),
  changePassword: vi.fn(),
}));

const contract: Contract = {
  id: 'contract-1',
  roomId: 'room-1',
  tenantId: 'tenant-1',
  startDate: '2026-11-01',
  endDate: '2027-10-31',
  monthlyRent: 3500000,
  depositAmount: 3500000,
  status: 'DRAFT',
};
const properties: Property[] = [{ id: 'property-1', name: 'Nhà An Bình', address: 'Quận 3, TP. Hồ Chí Minh' }];
const rooms: Room[] = [
  {
    id: 'room-1',
    propertyId: 'property-1',
    roomCode: 'P101',
    areaSquareMeters: 20,
    capacity: 2,
    monthlyRent: 3500000,
    depositAmount: 3500000,
    status: 'AVAILABLE',
  },
];
const tenants: Tenant[] = [
  {
    id: 'tenant-1',
    fullName: 'Nguyễn Minh Anh',
    phone: '0900000000',
    email: 'anh@example.test',
    hasPortalAccount: false,
  },
];
const tariffs: Tariff[] = [
  { id: 'tariff-1', utilityType: 'ELECTRICITY', pricePerUnit: 3500, effectiveFrom: '2026-11-01' },
];
const readings: MeterReading[] = [
  {
    id: 'reading-1',
    roomId: 'room-1',
    utilityType: 'ELECTRICITY',
    billingPeriod: '2026-11',
    previousReading: 100,
    currentReading: 150,
    usage: 50,
    unitPrice: 3500,
    charge: 175000,
  },
];
const ticket: MaintenanceTicket = {
  id: 'ticket-1',
  roomId: 'room-1',
  title: 'Rò rỉ vòi nước',
  description: 'Vòi lavabo nhỏ giọt',
  priority: 'HIGH',
  status: 'OPEN',
  estimatedCost: 150000,
  actualCost: 0,
  createdAt: '2026-11-14T00:00:00Z',
  updatedAt: '2026-11-14T00:00:00Z',
};
const draftInvoice: Invoice = {
  id: 'inv-1',
  contractId: 'contract-1',
  roomId: 'room-1',
  tenantId: 'tenant-1',
  billingPeriod: '2026-11',
  rentAmount: 3500000,
  electricityAmount: 175000,
  waterAmount: 0,
  serviceAmount: 150000,
  discountAmount: 0,
  adjustmentNotes: 'Phí dịch vụ định kỳ',
  totalAmount: 3825000,
  amountPaid: 0,
  balanceDue: 3825000,
  status: 'DRAFT',
};
const defaultSettings: OrganizationSettings = {
  billingDueDay: 5,
  internetFee: 100000,
  garbageFee: 50000,
  parkingFee: 0,
};
const membersList: Member[] = [
  {
    membershipId: 'm-1',
    userId: 'user-1',
    fullName: 'Chủ nhà',
    email: 'owner@example.test',
    role: 'OWNER',
    active: true,
    createdAt: '2026-11-01T00:00:00Z',
  },
];
const auditList: AuditLog[] = [
  {
    id: 'audit-1',
    actorUserId: 'user-1',
    actorEmail: 'owner@example.test',
    action: 'LOGIN_SUCCESS',
    targetType: 'AUTH',
    targetId: 'user-1',
    detail: 'Role=OWNER',
    createdAt: '2026-11-14T00:00:00Z',
  },
];

async function openWorkspace(
  role: 'OWNER' | 'MANAGER' | 'STAFF' = 'OWNER',
  initialContracts: Contract[] = [contract],
  initialInvoices: Invoice[] = []
) {
  vi.mocked(bootstrapCsrf).mockResolvedValue(undefined);
  vi.mocked(currentSession).mockResolvedValue(null);
  vi.mocked(signIn).mockResolvedValue({
    userId: role === 'OWNER' ? 'user-1' : 'user-2',
    organizationId: 'org-1',
    organizationName: 'Nhà Trọ Minh Anh',
    email: `${role.toLowerCase()}@example.test`,
    role,
  });
  vi.mocked(api.properties).mockResolvedValue(properties);
  vi.mocked(api.rooms).mockResolvedValue(rooms);
  vi.mocked(api.tenants).mockResolvedValue(tenants);
  vi.mocked(api.invoices).mockResolvedValue(initialInvoices);
  vi.mocked(api.tariffs).mockResolvedValue(tariffs);
  vi.mocked(api.readings).mockResolvedValue(readings);
  vi.mocked(api.contracts).mockResolvedValue(initialContracts);
  vi.mocked(api.maintenanceTickets).mockResolvedValue([ticket]);
  vi.mocked(api.notifications).mockResolvedValue([]);
  vi.mocked(api.members).mockResolvedValue(membersList);
  vi.mocked(api.settings).mockResolvedValue(defaultSettings);
  vi.mocked(api.auditLogs).mockResolvedValue(auditList);
  render(<App />);
  await waitFor(() => expect(bootstrapCsrf).toHaveBeenCalled());
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: `${role.toLowerCase()}@example.test` } });
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'a-secure-password' } });
  fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
  await screen.findByRole('heading', { name: 'Chào buổi sáng 👋' });
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('RMS owner workspace & RBAC + Tenant Portal', () => {
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

  it('allows owner to adjust draft invoices, inspect audit logs, and invite staff', async () => {
    await openWorkspace('OWNER', [contract], [draftInvoice]);

    fireEvent.click(screen.getAllByRole('button', { name: /Hóa đơn/ })[0]);
    expect(await screen.findByText('Điều chỉnh')).toBeInTheDocument();
    vi.mocked(api.adjustInvoice).mockResolvedValue({
      ...draftInvoice,
      serviceAmount: 200000,
      discountAmount: 50000,
      totalAmount: 3825000,
      balanceDue: 3825000,
    });
    fireEvent.click(screen.getByRole('button', { name: 'Điều chỉnh' }));
    fireEvent.change(screen.getByLabelText('Phí dịch vụ / phụ thu (₫)'), { target: { value: '200000' } });
    fireEvent.change(screen.getByLabelText('Giảm trừ / khuyến mãi (₫)'), { target: { value: '50000' } });
    fireEvent.click(screen.getByRole('button', { name: 'Lưu điều chỉnh' }));
    await waitFor(() =>
      expect(api.adjustInvoice).toHaveBeenCalledWith('inv-1', {
        serviceAmount: 200000,
        discountAmount: 50000,
        adjustmentNotes: 'Phí dịch vụ định kỳ',
      })
    );

    fireEvent.click(screen.getByRole('button', { name: 'Nhật ký' }));
    expect(await screen.findByText('LOGIN_SUCCESS')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Thành viên' }));
    expect(await screen.findAllByText('owner@example.test')).toHaveLength(2);
  });

  it('hides owner-only menus for STAFF role', async () => {
    await openWorkspace('STAFF');
    expect(screen.queryByRole('button', { name: 'Thành viên' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Nhật ký' })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Hợp đồng' }));
    expect(screen.queryByRole('button', { name: /Tạo hợp đồng/ })).toBeNull();
  });

  it('renders dedicated Tenant Portal for TENANT users and submits maintenance ticket', async () => {
    vi.mocked(bootstrapCsrf).mockResolvedValue(undefined);
    vi.mocked(currentSession).mockResolvedValue(null);
    vi.mocked(signIn).mockResolvedValue({
      userId: 'tenant-u1',
      organizationId: 'org-1',
      organizationName: 'Nhà Trọ Minh Anh',
      email: 'tenant@example.test',
      role: 'TENANT',
    });
    const overview: PortalOverview = {
      tenantId: 'tenant-1',
      fullName: 'Nguyễn Minh Anh',
      phone: '0900000000',
      email: 'tenant@example.test',
      contracts: [
        {
          id: 'contract-1',
          roomId: 'room-1',
          roomCode: 'P101',
          startDate: '2026-11-01',
          endDate: '2027-10-31',
          monthlyRent: 3500000,
          depositAmount: 3500000,
          status: 'ACTIVE',
        },
      ],
      rooms,
    };
    vi.mocked(api.tenantPortalOverview).mockResolvedValue(overview);
    vi.mocked(api.tenantPortalInvoices).mockResolvedValue([draftInvoice]);
    vi.mocked(api.tenantPortalTickets).mockResolvedValue([]);
    vi.mocked(api.createTenantPortalTicket).mockResolvedValue(ticket);

    render(<App />);
    await waitFor(() => expect(bootstrapCsrf).toHaveBeenCalled());
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'tenant@example.test' } });
    fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'a-secure-password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByText('CỔNG NGƯỜI THUÊ')).toBeInTheDocument();
    expect(await screen.findByText('P101')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Hóa đơn của tôi/ }));
    expect(await screen.findByText('2026-11')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Yêu cầu bảo trì/ }));
    fireEvent.click(screen.getAllByRole('button', { name: /Gửi yêu cầu bảo trì/ })[0]);
    fireEvent.change(screen.getByLabelText('Tiêu đề sự cố'), { target: { value: 'Hỏng đèn' } });
    fireEvent.change(screen.getByLabelText('Mô tả chi tiết'), { target: { value: 'Đèn không sáng' } });
    fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu' }));

    await waitFor(() =>
      expect(api.createTenantPortalTicket).toHaveBeenCalledWith({
        roomId: 'room-1',
        title: 'Hỏng đèn',
        description: 'Đèn không sáng',
        priority: 'MEDIUM',
      })
    );
  });

  it('keeps changePassword import wired', () => {
    expect(changePassword).toBeDefined();
  });
});
