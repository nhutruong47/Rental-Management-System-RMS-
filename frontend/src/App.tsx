import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Activity, ArrowUpRight, Bell, Building2, Check, CircleHelp, CreditCard, DoorOpen, Droplets,
  FileText, Home, KeyRound, LogOut, Menu, Plus, Search, Settings, ShieldCheck, UserPlus, Users, Wallet, Wrench, X, Zap,
} from 'lucide-react';
import {
  api, bootstrapCsrf, changePassword, currentSession, registerAccount, signIn, signOut,
  type AppNotification, type AuditLog, type Contract, type HandoverRecord, type Invoice, type MaintenanceTicket,
  type Member, type MembershipRole, type MeterReading, type OrganizationSettings, type PortalOverview,
  type Property, type Room, type RoomStatusValue, type SessionAccount,
  type Tariff, type Tenant, type TicketPriority, type TicketStatus, type UtilityType,
} from './api';

type PageKey =
  | 'dashboard'
  | 'properties'
  | 'tenants'
  | 'contracts'
  | 'utilities'
  | 'invoices'
  | 'maintenance'
  | 'members'
  | 'settings'
  | 'audit';

type PortalTab = 'overview' | 'invoices' | 'tickets';

type ModalKind =
  | 'property'
  | 'room'
  | 'tenant'
  | 'provision-tenant'
  | 'contract'
  | 'terminate'
  | 'handover'
  | 'tariff'
  | 'reading'
  | 'adjust-invoice'
  | 'payment'
  | 'ticket'
  | 'assign'
  | 'resolve'
  | 'invite-member'
  | 'change-password'
  | 'tenant-ticket';

const money = (value: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0);
const currentPeriod = new Date().toISOString().slice(0, 7);

const label: Record<PageKey, string> = {
  dashboard: 'Tổng quan',
  properties: 'Nhà & phòng',
  tenants: 'Người thuê',
  contracts: 'Hợp đồng',
  utilities: 'Điện & nước',
  invoices: 'Hóa đơn',
  maintenance: 'Bảo trì',
  members: 'Thành viên',
  settings: 'Cài đặt',
  audit: 'Nhật ký',
};

const roleLabel: Record<string, string> = {
  OWNER: 'Chủ sở hữu',
  MANAGER: 'Quản lý',
  STAFF: 'Nhân viên',
  TENANT: 'Người thuê',
};

const navItems: { key: PageKey; icon: typeof Home; roles: MembershipRole[] }[] = [
  { key: 'dashboard', icon: Home, roles: ['OWNER', 'MANAGER', 'STAFF'] },
  { key: 'properties', icon: Building2, roles: ['OWNER', 'MANAGER', 'STAFF'] },
  { key: 'tenants', icon: Users, roles: ['OWNER', 'MANAGER', 'STAFF'] },
  { key: 'contracts', icon: FileText, roles: ['OWNER', 'MANAGER', 'STAFF'] },
  { key: 'utilities', icon: Zap, roles: ['OWNER', 'MANAGER', 'STAFF'] },
  { key: 'invoices', icon: CreditCard, roles: ['OWNER', 'MANAGER', 'STAFF'] },
  { key: 'maintenance', icon: Wrench, roles: ['OWNER', 'MANAGER', 'STAFF'] },
  { key: 'members', icon: UserPlus, roles: ['OWNER', 'MANAGER'] },
  { key: 'settings', icon: Settings, roles: ['OWNER', 'MANAGER', 'STAFF'] },
  { key: 'audit', icon: Activity, roles: ['OWNER', 'MANAGER'] },
];

const DEFAULT_SETTINGS: OrganizationSettings = {
  billingDueDay: 5,
  internetFee: 0,
  garbageFee: 0,
  parkingFee: 0,
};

function App() {
  const [user, setUser] = useState<SessionAccount | null>(null);
  const [csrfReady, setCsrfReady] = useState(false);
  const [page, setPage] = useState<PageKey>('dashboard');
  const [portalTab, setPortalTab] = useState<PortalTab>('overview');
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [period, setPeriod] = useState(currentPeriod);
  const [properties, setProperties] = useState<Property[]>([]);
  const [allRooms, setAllRooms] = useState<Room[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [tariffs, setTariffs] = useState<Tariff[]>([]);
  const [readings, setReadings] = useState<MeterReading[]>([]);
  const [tickets, setTickets] = useState<MaintenanceTicket[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [settings, setSettings] = useState<OrganizationSettings>(DEFAULT_SETTINGS);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [portalOverview, setPortalOverview] = useState<PortalOverview | null>(null);
  const [portalInvoices, setPortalInvoices] = useState<Invoice[]>([]);
  const [portalTickets, setPortalTickets] = useState<MaintenanceTicket[]>([]);
  const [notifOpen, setNotifOpen] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [modal, setModal] = useState<ModalKind | null>(null);
  const [selectedProperty, setSelectedProperty] = useState('');
  const [selectedTenant, setSelectedTenant] = useState<Tenant | null>(null);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [selectedContract, setSelectedContract] = useState<Contract | null>(null);
  const [selectedHandover, setSelectedHandover] = useState<HandoverRecord | null>(null);
  const [selectedTicket, setSelectedTicket] = useState<MaintenanceTicket | null>(null);
  const [search, setSearch] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const sessionEpoch = useRef(0);

  const isTenant = user?.role === 'TENANT';
  const canManage = user?.role === 'OWNER' || user?.role === 'MANAGER';

  const loadWorkspace = useCallback(async () => {
    if (!user) return;
    if (user.role === 'TENANT') {
      const [overview, myInvoices, myTickets] = await Promise.all([
        api.tenantPortalOverview(),
        api.tenantPortalInvoices(),
        api.tenantPortalTickets(),
      ]);
      setPortalOverview(overview);
      setPortalInvoices(myInvoices);
      setPortalTickets(myTickets);
      return;
    }

    const isManager = user.role === 'OWNER' || user.role === 'MANAGER';
    const [
      nextProperties,
      nextTenants,
      nextInvoices,
      nextTariffs,
      nextContracts,
      nextReadings,
      nextTickets,
      nextNotifications,
      nextMembers,
      nextSettings,
      nextAudit,
    ] = await Promise.all([
      api.properties(),
      api.tenants(),
      api.invoices(period),
      api.tariffs(),
      api.contracts(),
      api.readings(period),
      api.maintenanceTickets().catch(() => [] as MaintenanceTicket[]),
      api.notifications().catch(() => [] as AppNotification[]),
      isManager && api.members ? api.members().catch(() => [] as Member[]) : Promise.resolve([] as Member[]),
      api.settings ? api.settings().catch(() => DEFAULT_SETTINGS) : Promise.resolve(DEFAULT_SETTINGS),
      isManager && api.auditLogs ? api.auditLogs().catch(() => [] as AuditLog[]) : Promise.resolve([] as AuditLog[]),
    ]);
    const roomLists = await Promise.all(nextProperties.map((property) => api.rooms(property.id)));
    setProperties(nextProperties);
    setAllRooms(roomLists.flat());
    setTenants(nextTenants);
    setInvoices(nextInvoices);
    setTariffs(nextTariffs);
    setContracts(nextContracts);
    setReadings(nextReadings);
    setTickets(nextTickets);
    setNotifications(nextNotifications);
    setMembers(nextMembers);
    setSettings(nextSettings);
    setAuditLogs(nextAudit);
    const activeIndex = selectedProperty ? nextProperties.findIndex((item) => item.id === selectedProperty) : -1;
    const resolvedIndex = activeIndex >= 0 ? activeIndex : 0;
    const propertyId = nextProperties[resolvedIndex]?.id ?? '';
    setSelectedProperty(propertyId);
    setRooms(propertyId ? roomLists[resolvedIndex] ?? [] : []);
  }, [period, selectedProperty, user]);

  useEffect(() => {
    let active = true;
    bootstrapCsrf()
      .then(async () => {
        if (!active) return;
        setCsrfReady(true);
        const existing = await currentSession();
        if (active && existing) setUser(existing);
      })
      .catch(() => {
        if (active) setError('Không thể khởi tạo bảo mật cho phiên làm việc. Tải lại trang để thử lại.');
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!user) return;
    setError('');
    loadWorkspace().catch((reason: unknown) =>
      setError(reason instanceof Error ? reason.message : 'Không tải được dữ liệu.')
    );
  }, [user, loadWorkspace]);

  const filteredRooms = useMemo(
    () => rooms.filter((room) => room.roomCode.toLowerCase().includes(search.toLowerCase())),
    [rooms, search]
  );
  const occupied = allRooms.filter((room) => room.status === 'OCCUPIED').length;
  const occupancy = allRooms.length ? Math.round((occupied / allRooms.length) * 100) : 0;
  const totalDue = invoices.reduce((total, invoice) => total + invoice.balanceDue, 0);
  const collected = invoices.reduce((total, invoice) => total + invoice.amountPaid, 0);
  const billed = invoices.reduce((total, invoice) => total + invoice.totalAmount, 0);

  async function perform<T>(action: () => Promise<T>, done?: (result: T) => void, successText?: string) {
    const epoch = sessionEpoch.current;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await action();
      if (epoch !== sessionEpoch.current) return;
      done?.(result);
      await loadWorkspace();
      if (epoch === sessionEpoch.current && successText) setNotice(successText);
    } catch (reason) {
      if (epoch === sessionEpoch.current) {
        setError(reason instanceof Error ? reason.message : 'Đã có lỗi xảy ra. Vui lòng thử lại.');
      }
    } finally {
      if (epoch === sessionEpoch.current) setBusy(false);
    }
  }

  async function handleLogin(
    email: string,
    password: string,
    registration?: { fullName: string; organizationName: string }
  ) {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      if (registration) await registerAccount(email, password, registration.fullName, registration.organizationName);
      const account = await signIn(email, password);
      setUser(account);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể đăng nhập.');
    } finally {
      setBusy(false);
    }
  }

  async function refreshRooms(propertyId: string) {
    setSelectedProperty(propertyId);
    if (propertyId) setRooms(await api.rooms(propertyId));
    else setRooms([]);
  }

  async function logout() {
    sessionEpoch.current += 1;
    setUser(null);
    setPage('dashboard');
    setPortalTab('overview');
    setProperties([]);
    setAllRooms([]);
    setRooms([]);
    setTenants([]);
    setInvoices([]);
    setTariffs([]);
    setReadings([]);
    setContracts([]);
    setTickets([]);
    setNotifications([]);
    setMembers([]);
    setSettings(DEFAULT_SETTINGS);
    setAuditLogs([]);
    setPortalOverview(null);
    setPortalInvoices([]);
    setPortalTickets([]);
    setNotifOpen(false);
    setNotice('');
    setError('');
    setBusy(false);
    try {
      await signOut();
    } catch {
      /* local UI still clears if a session already expired */
    }
  }

  async function openHandover(contract: Contract) {
    setBusy(true);
    setError('');
    try {
      const record = await api.handover(contract.id);
      setSelectedContract(contract);
      setSelectedHandover(record);
      setModal('handover');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không tải được biên bản bàn giao.');
    } finally {
      setBusy(false);
    }
  }

  if (!user) return <AuthScreen onSubmit={handleLogin} busy={busy || !csrfReady} error={error} />;

  const orgName = user.organizationName || 'Tổ chức của bạn';
  const todayLabel = new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
    .format(new Date())
    .toUpperCase();

  if (isTenant) {
    const portalDue = portalInvoices.reduce((acc, inv) => acc + inv.balanceDue, 0);
    const activeContracts = (portalOverview?.contracts ?? []).filter((c) => c.status === 'ACTIVE');
    return (
      <div className="app-shell">
        <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
          <a className="brand" href="#top" onClick={(event) => event.preventDefault()}>
            <span className="brand-mark">
              <Building2 size={19} />
            </span>
            <span>
              rms<span className="brand-dot">.</span>
            </span>
            <small>CỔNG NGƯỜI THUÊ</small>
          </a>
          <div className="side-caption">KHÔNG GIAN CỦA BẠN</div>
          <div className="workspace-switch" aria-label="Tổ chức hiện tại">
            <span className="workspace-avatar">{orgName.slice(0, 1).toUpperCase()}</span>
            <span className="workspace-copy">
              <strong>{orgName}</strong>
              <small>Vai trò: Người thuê</small>
            </span>
          </div>
          <div className="side-caption nav-caption">DANH MỤC</div>
          <nav className="main-nav">
            <button
              className={`nav-link ${portalTab === 'overview' ? 'active' : ''}`}
              onClick={() => {
                setPortalTab('overview');
                setMenuOpen(false);
              }}
            >
              <Home size={18} strokeWidth={1.8} />
              <span>Phòng & Hợp đồng</span>
            </button>
            <button
              className={`nav-link ${portalTab === 'invoices' ? 'active' : ''}`}
              onClick={() => {
                setPortalTab('invoices');
                setMenuOpen(false);
              }}
            >
              <CreditCard size={18} strokeWidth={1.8} />
              <span>Hóa đơn của tôi</span>
              {portalDue > 0 && <span className="nav-pill">{portalInvoices.filter((i) => i.balanceDue > 0).length}</span>}
            </button>
            <button
              className={`nav-link ${portalTab === 'tickets' ? 'active' : ''}`}
              onClick={() => {
                setPortalTab('tickets');
                setMenuOpen(false);
              }}
            >
              <Wrench size={18} strokeWidth={1.8} />
              <span>Yêu cầu bảo trì</span>
            </button>
          </nav>
          <div className="sidebar-bottom">
            <div className="upgrade-card">
              <span className="upgrade-icon">
                <ShieldCheck size={16} />
              </span>
              <strong>Quyền truy cập riêng tư</strong>
              <p>Bạn chỉ xem được hợp đồng, hóa đơn và bảo trì gắn với tài khoản của mình.</p>
            </div>
            <div className="profile-row">
              <div className="avatar">{user.email.slice(0, 1).toUpperCase()}</div>
              <div className="profile-copy">
                <strong>{portalOverview?.fullName || user.email.split('@')[0]}</strong>
                <small>{user.email}</small>
              </div>
              <button
                className="icon-button"
                title="Đổi mật khẩu"
                aria-label="Đổi mật khẩu"
                onClick={() => setModal('change-password')}
              >
                <KeyRound size={15} />
              </button>
              <button className="icon-button profile-logout" title="Đăng xuất" aria-label="Đăng xuất" onClick={logout}>
                <LogOut size={16} />
              </button>
            </div>
          </div>
        </aside>

        <main className="main-area" id="top">
          <header className="topbar">
            <button className="mobile-menu icon-button" onClick={() => setMenuOpen(!menuOpen)} aria-label="Mở menu">
              <Menu size={19} />
            </button>
            <div className="breadcrumbs">
              <span>{orgName}</span>
              <span className="crumb-separator">/</span>
              <strong>
                {portalTab === 'overview'
                  ? 'Phòng & Hợp đồng'
                  : portalTab === 'invoices'
                    ? 'Hóa đơn của tôi'
                    : 'Yêu cầu bảo trì'}
              </strong>
            </div>
            <div className="top-actions">
              <span className="secure-label">
                <ShieldCheck size={15} /> Cổng người thuê
              </span>
              <div className="top-divider" />
              <div className="avatar top-avatar">{user.email.slice(0, 1).toUpperCase()}</div>
            </div>
          </header>
          <section className="page-content">
            <div className="page-heading">
              <div>
                <div className="eyebrow">{todayLabel}</div>
                <h1>Xin chào, {portalOverview?.fullName || user.email.split('@')[0]}</h1>
                <p>Theo dõi hợp đồng thuê, hóa đơn hàng tháng và gửi yêu cầu sửa chữa.</p>
              </div>
              <div className="heading-actions">
                {portalTab === 'tickets' && activeContracts.length > 0 && (
                  <button className="button button-primary" onClick={() => setModal('tenant-ticket')}>
                    <Plus size={17} /> Gửi yêu cầu bảo trì
                  </button>
                )}
              </div>
            </div>
            {error && (
              <div className="alert error-alert" role="alert">
                <CircleHelp size={17} />
                <span>{error}</span>
                <button onClick={() => setError('')} aria-label="Đóng">
                  <X size={16} />
                </button>
              </div>
            )}
            {notice && (
              <div className="alert" style={{ background: '#edf8f3', color: '#1b6e52', border: '1px solid #ccead9' }}>
                <span>{notice}</span>
                <button onClick={() => setNotice('')} aria-label="Đóng thông báo">
                  <X size={16} />
                </button>
              </div>
            )}
            {portalTab === 'overview' && (
              <>
                <div className="metrics-grid">
                  <Metric
                    icon={DoorOpen}
                    label="Phòng đang thuê"
                    value={`${activeContracts.length}`}
                    caption="Hợp đồng đang hiệu lực"
                    tone="blue"
                  />
                  <Metric
                    icon={Wallet}
                    label="Cần thanh toán"
                    value={money(portalDue)}
                    caption={`${portalInvoices.filter((i) => i.balanceDue > 0).length} hóa đơn chưa tất toán`}
                    tone="amber"
                  />
                  <Metric
                    icon={Wrench}
                    label="Yêu cầu bảo trì"
                    value={`${portalTickets.length}`}
                    caption="Tổng số phiếu đã ghi nhận"
                    tone="violet"
                  />
                  <Metric
                    icon={FileText}
                    label="Tổng hợp đồng"
                    value={`${portalOverview?.contracts.length ?? 0}`}
                    caption={portalOverview?.phone || portalOverview?.email || 'Hồ sơ cá nhân'}
                    tone="green"
                  />
                </div>
                <section className="panel data-panel">
                  <div className="data-toolbar">
                    <div>
                      <span className="toolbar-label">HỢP ĐỒNG CỦA TÔI</span>
                      <div className="toolbar-subtitle">Thông tin phòng, thời hạn và tiền cọc đang lưu giữ.</div>
                    </div>
                  </div>
                  {portalOverview?.contracts.length ? (
                    <div className="table-wrap">
                      <table>
                        <thead>
                          <tr>
                            <th>PHÒNG</th>
                            <th>THỜI HẠN</th>
                            <th>TIỀN THUÊ / THÁNG</th>
                            <th>TIỀN CỌC</th>
                            <th>TRẠNG THÁI</th>
                          </tr>
                        </thead>
                        <tbody>
                          {portalOverview.contracts.map((c) => (
                            <tr key={c.id}>
                              <td>
                                <strong className="room-code">{c.roomCode}</strong>
                              </td>
                              <td>
                                {new Intl.DateTimeFormat('vi-VN').format(new Date(`${c.startDate}T00:00:00`))} –{' '}
                                {new Intl.DateTimeFormat('vi-VN').format(new Date(`${c.endDate}T00:00:00`))}
                              </td>
                              <td className="rent-cell">{money(c.monthlyRent)}</td>
                              <td>{money(c.depositAmount)}</td>
                              <td>
                                <span className={`status-badge status-${c.status.toLowerCase()}`}>
                                  {c.status === 'ACTIVE'
                                    ? 'Đang hiệu lực'
                                    : c.status === 'DRAFT'
                                      ? 'Bản nháp'
                                      : 'Đã thanh lý'}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="table-empty">Bạn chưa có hợp đồng thuê nào được gắn với tài khoản này.</div>
                  )}
                </section>
              </>
            )}
            {portalTab === 'invoices' && (
              <section className="panel data-panel">
                <div className="data-toolbar">
                  <div>
                    <span className="toolbar-label">HÓA ĐƠN CỦA TÔI</span>
                    <div className="toolbar-subtitle">Chi tiết tiền phòng, điện nước, phí dịch vụ và số tiền còn lại.</div>
                  </div>
                </div>
                {portalInvoices.length ? (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>KỲ</th>
                          <th>TIỀN PHÒNG</th>
                          <th>ĐIỆN + NƯỚC</th>
                          <th>DỊCH VỤ / GIẢM TRỪ</th>
                          <th>TỔNG CỘNG</th>
                          <th>CÒN PHẢI TRẢ</th>
                          <th>TRẠNG THÁI</th>
                        </tr>
                      </thead>
                      <tbody>
                        {portalInvoices.map((inv) => (
                          <tr key={inv.id}>
                            <td>
                              <strong>{inv.billingPeriod}</strong>
                            </td>
                            <td>{money(inv.rentAmount)}</td>
                            <td>{money(inv.electricityAmount + inv.waterAmount)}</td>
                            <td>
                              +{money(inv.serviceAmount ?? 0)} / -{money(inv.discountAmount ?? 0)}
                              {inv.adjustmentNotes && <div style={{ fontSize: 11, color: '#68707e' }}>{inv.adjustmentNotes}</div>}
                            </td>
                            <td className="rent-cell">{money(inv.totalAmount)}</td>
                            <td className={inv.balanceDue > 0 ? 'due-cell' : 'paid-cell'}>{money(inv.balanceDue)}</td>
                            <td>
                              <StatusBadge status={inv.status} />
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="table-empty">Chưa có hóa đơn nào được phát hành cho bạn.</div>
                )}
              </section>
            )}
            {portalTab === 'tickets' && (
              <section className="panel data-panel">
                <div className="data-toolbar">
                  <div>
                    <span className="toolbar-label">YÊU CẦU BẢO TRÌ</span>
                    <div className="toolbar-subtitle">Báo sự cố thiết bị để ban quản lý phân công xử lý.</div>
                  </div>
                  {activeContracts.length > 0 && (
                    <button className="button button-primary" onClick={() => setModal('tenant-ticket')}>
                      <Plus size={16} /> Gửi yêu cầu bảo trì
                    </button>
                  )}
                </div>
                {portalTickets.length ? (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>PHÒNG</th>
                          <th>TIÊU ĐỀ</th>
                          <th>MỨC ĐỘ</th>
                          <th>PHỤ TRÁCH</th>
                          <th>TRẠNG THÁI</th>
                        </tr>
                      </thead>
                      <tbody>
                        {portalTickets.map((t) => (
                          <tr key={t.id}>
                            <td>
                              <strong className="room-code">
                                {portalOverview?.rooms.find((r) => r.id === t.roomId)?.roomCode ?? '—'}
                              </strong>
                            </td>
                            <td>
                              <strong>{t.title}</strong>
                              <div style={{ fontSize: 11, color: '#68707e' }}>{t.description}</div>
                            </td>
                            <td>{priorityText(t.priority)}</td>
                            <td>{t.assigneeName || 'Chờ phân công'}</td>
                            <td>
                              <span className={`status-badge status-${t.status.toLowerCase()}`}>
                                {ticketStatusText(t.status)}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="table-empty">Bạn chưa gửi yêu cầu bảo trì nào.</div>
                )}
              </section>
            )}
            <footer className="page-footer">
              <span>© 2026 RMS. Quản lý rõ ràng, vận hành nhẹ nhàng.</span>
              <span>Phiên bản 1.0 (Cổng người thuê)</span>
            </footer>
          </section>
        </main>
        {modal === 'change-password' && (
          <Modal title="Đổi mật khẩu tài khoản" onClose={() => setModal(null)}>
            <ChangePasswordForm
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(cur, next) =>
                perform(() => changePassword(cur, next), () => setModal(null), 'Đã cập nhật mật khẩu mới.')
              }
            />
          </Modal>
        )}
        {modal === 'tenant-ticket' && (
          <Modal title="Gửi yêu cầu bảo trì" onClose={() => setModal(null)}>
            <TenantTicketForm
              contracts={activeContracts}
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) =>
                perform(
                  () => api.createTenantPortalTicket(body),
                  () => setModal(null),
                  'Đã gửi yêu cầu bảo trì tới ban quản lý.'
                )
              }
            />
          </Modal>
        )}
        {menuOpen && <button className="mobile-backdrop" onClick={() => setMenuOpen(false)} aria-label="Đóng menu" />}
      </div>
    );
  }

  const openTicketsCount = tickets.filter((t) => t.status !== 'CLOSED' && t.status !== 'RESOLVED').length;
  const unreadNotifs = notifications.filter((n) => !n.read).length;
  const visibleNavItems = navItems.filter((item) => item.roles.includes((user.role as MembershipRole) || 'OWNER'));

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
        <a className="brand" href="#top" onClick={(event) => event.preventDefault()}>
          <span className="brand-mark">
            <Building2 size={19} />
          </span>
          <span>
            rms<span className="brand-dot">.</span>
          </span>
          <small>NHÀ CHO THUÊ</small>
        </a>
        <div className="side-caption">KHÔNG GIAN LÀM VIỆC</div>
        <div className="workspace-switch" aria-label="Tổ chức hiện tại">
          <span className="workspace-avatar">{orgName.slice(0, 1).toUpperCase()}</span>
          <span className="workspace-copy">
            <strong>{orgName}</strong>
            <small>Vai trò: {roleLabel[user.role] ?? user.role}</small>
          </span>
        </div>
        <div className="side-caption nav-caption">MENU CHÍNH</div>
        <nav className="main-nav">
          {visibleNavItems.map(({ key, icon: Icon }) => (
            <button
              key={key}
              className={`nav-link ${page === key ? 'active' : ''}`}
              onClick={() => {
                setPage(key);
                setMenuOpen(false);
              }}
            >
              <Icon size={18} strokeWidth={1.8} />
              <span>{label[key]}</span>
              {key === 'invoices' && totalDue > 0 && (
                <span className="nav-pill">{invoices.filter((invoice) => invoice.balanceDue > 0).length}</span>
              )}
              {key === 'maintenance' && openTicketsCount > 0 && <span className="nav-pill">{openTicketsCount}</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="upgrade-card">
            <span className="upgrade-icon">
              <ShieldCheck size={16} />
            </span>
            <strong>Dữ liệu tách biệt theo tổ chức</strong>
            <p>Phòng, hợp đồng, chỉ số và thu chi được gắn với {orgName}.</p>
          </div>
          <div className="profile-row">
            <div className="avatar">{user.email.slice(0, 1).toUpperCase()}</div>
            <div className="profile-copy">
              <strong>{user.email.split('@')[0]}</strong>
              <small>{user.email}</small>
            </div>
            <button
              className="icon-button"
              title="Đổi mật khẩu"
              aria-label="Đổi mật khẩu"
              onClick={() => setModal('change-password')}
            >
              <KeyRound size={15} />
            </button>
            <button className="icon-button profile-logout" title="Đăng xuất" aria-label="Đăng xuất" onClick={logout}>
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </aside>

      <main className="main-area" id="top">
        <header className="topbar">
          <button className="mobile-menu icon-button" onClick={() => setMenuOpen(!menuOpen)} aria-label="Mở menu">
            <Menu size={19} />
          </button>
          <div className="breadcrumbs">
            <span>{orgName}</span>
            <span className="crumb-separator">/</span>
            <strong>{label[page]}</strong>
          </div>
          <div className="top-actions">
            <span className="secure-label">
              <ShieldCheck size={15} /> {roleLabel[user.role] ?? user.role}
            </span>
            <button
              className="icon-button notification-button"
              title="Thông báo"
              aria-label="Thông báo"
              onClick={() => setNotifOpen(!notifOpen)}
            >
              <Bell size={18} />
              {unreadNotifs > 0 && <i />}
            </button>
            <div className="top-divider" />
            <div className="avatar top-avatar">{user.email.slice(0, 1).toUpperCase()}</div>
          </div>
        </header>
        <section className="page-content">
          {notifOpen && (
            <section className="panel data-panel" style={{ marginBottom: 18 }}>
              <div className="panel-head">
                <div>
                  <span className="panel-kicker">THÔNG BÁO NỘI BỘ</span>
                  <h3>Hoạt động gần đây ({unreadNotifs} chưa đọc)</h3>
                </div>
                <button className="icon-button" onClick={() => setNotifOpen(false)} aria-label="Đóng thông báo">
                  <X size={16} />
                </button>
              </div>
              {notifications.length ? (
                <div style={{ display: 'grid', gap: 8 }}>
                  {notifications.slice(0, 8).map((item) => (
                    <div
                      key={item.id}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        gap: 12,
                        padding: '10px 12px',
                        borderRadius: 8,
                        background: item.read ? '#fafbfc' : '#f1f4fb',
                        border: '1px solid #e9ebef',
                      }}
                    >
                      <div>
                        <strong>{item.title}</strong>
                        <div style={{ fontSize: 11, color: '#68707e' }}>{item.message}</div>
                      </div>
                      {!item.read && (
                        <button
                          className="button button-small button-secondary"
                          disabled={busy}
                          onClick={() => perform(() => api.markNotificationRead(item.id))}
                        >
                          Đã đọc
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="table-empty">Chưa có thông báo mới.</div>
              )}
            </section>
          )}
          <div className="page-heading">
            <div>
              <div className="eyebrow">{todayLabel}</div>
              <h1>{page === 'dashboard' ? 'Chào buổi sáng 👋' : label[page]}</h1>
              <p>{page === 'dashboard' ? 'Đây là tình hình vận hành danh mục nhà của bạn.' : pageSubtitle(page)}</p>
            </div>
            <div className="heading-actions">
              <label className="period-picker">
                <span>Kỳ báo cáo</span>
                <input type="month" value={period} onChange={(event) => setPeriod(event.target.value)} />
              </label>
              {page === 'properties' && canManage && (
                <button className="button button-primary" onClick={() => setModal('property')}>
                  <Plus size={17} /> Thêm nhà
                </button>
              )}
              {page === 'tenants' && canManage && (
                <button className="button button-primary" onClick={() => setModal('tenant')}>
                  <Plus size={17} /> Thêm người thuê
                </button>
              )}
              {page === 'contracts' && canManage && (
                <button
                  className="button button-primary"
                  onClick={() => setModal('contract')}
                  disabled={!allRooms.length || !tenants.length}
                >
                  <Plus size={17} /> Tạo hợp đồng
                </button>
              )}
              {page === 'utilities' && (
                <button
                  className="button button-primary"
                  onClick={() => setModal('reading')}
                  disabled={!allRooms.length}
                >
                  <Plus size={17} /> Ghi chỉ số
                </button>
              )}
              {page === 'invoices' && canManage && (
                <button
                  className="button button-primary"
                  disabled={busy}
                  onClick={() =>
                    perform(() => api.generateInvoices(period), undefined, `Đã cập nhật danh sách hóa đơn kỳ ${period}.`)
                  }
                >
                  <Zap size={16} /> Tạo hóa đơn
                </button>
              )}
              {page === 'maintenance' && (
                <button className="button button-primary" onClick={() => setModal('ticket')} disabled={!allRooms.length}>
                  <Plus size={17} /> Tạo yêu cầu bảo trì
                </button>
              )}
              {page === 'members' && canManage && (
                <button className="button button-primary" onClick={() => setModal('invite-member')}>
                  <Plus size={17} /> Mời thành viên
                </button>
              )}
            </div>
          </div>
          {error && (
            <div className="alert error-alert" role="alert">
              <CircleHelp size={17} />
              <span>{error}</span>
              <button onClick={() => setError('')} aria-label="Đóng">
                <X size={16} />
              </button>
            </div>
          )}
          {notice && (
            <div className="alert" style={{ background: '#edf8f3', color: '#1b6e52', border: '1px solid #ccead9' }}>
              <span>{notice}</span>
              <button onClick={() => setNotice('')} aria-label="Đóng thông báo">
                <X size={16} />
              </button>
            </div>
          )}
          {page === 'dashboard' && (
            <Dashboard
              properties={properties}
              rooms={allRooms}
              tenants={tenants}
              invoices={invoices}
              occupancy={occupancy}
              billed={billed}
              collected={collected}
              totalDue={totalDue}
              period={period}
              canManage={canManage}
              onNavigate={setPage}
              onGenerate={() => perform(() => api.generateInvoices(period), undefined, `Đã tạo hóa đơn kỳ ${period}.`)}
            />
          )}
          {page === 'properties' && (
            <PropertiesPage
              properties={properties}
              rooms={filteredRooms}
              selected={selectedProperty}
              search={search}
              busy={busy}
              canManage={canManage}
              onSearch={setSearch}
              onSelect={refreshRooms}
              onAddRoom={() => setModal('room')}
              onAddProperty={() => setModal('property')}
              onChangeRoomStatus={(room, status) =>
                perform(
                  () => api.updateRoomStatus(selectedProperty, room.id, status),
                  undefined,
                  `Đã cập nhật phòng ${room.roomCode}.`
                )
              }
            />
          )}
          {page === 'tenants' && (
            <TenantsPage
              tenants={tenants}
              canManage={canManage}
              onAdd={() => setModal('tenant')}
              onProvisionPortal={(tenant) => {
                setSelectedTenant(tenant);
                setModal('provision-tenant');
              }}
            />
          )}
          {page === 'contracts' && (
            <ContractsPage
              contracts={contracts}
              rooms={allRooms}
              tenants={tenants}
              busy={busy}
              canManage={canManage}
              onAdd={() => setModal('contract')}
              onActivate={(contract) =>
                perform(
                  () => api.activateContract(contract.id),
                  undefined,
                  'Đã kích hoạt hợp đồng và cập nhật phòng sang Đang thuê.'
                )
              }
              onTerminate={(contract) => {
                setSelectedContract(contract);
                setModal('terminate');
              }}
              onViewHandover={openHandover}
            />
          )}
          {page === 'utilities' && (
            <UtilitiesPage
              properties={properties}
              rooms={allRooms}
              tariffs={tariffs}
              readings={readings}
              period={period}
              canManage={canManage}
              onAddTariff={() => setModal('tariff')}
              onAddReading={() => setModal('reading')}
            />
          )}
          {page === 'invoices' && (
            <InvoicesPage
              invoices={invoices}
              tenants={tenants}
              rooms={allRooms}
              busy={busy}
              canManage={canManage}
              onGenerate={() => perform(() => api.generateInvoices(period), undefined, `Đã tạo hóa đơn kỳ ${period}.`)}
              onAdjust={(invoice) => {
                setSelectedInvoice(invoice);
                setModal('adjust-invoice');
              }}
              onIssue={(invoice) => perform(() => api.issueInvoice(invoice.id), undefined, 'Đã phát hành hóa đơn.')}
              onPay={(invoice) => {
                setSelectedInvoice(invoice);
                setModal('payment');
              }}
            />
          )}
          {page === 'maintenance' && (
            <MaintenancePage
              tickets={tickets}
              rooms={allRooms}
              busy={busy}
              onAdd={() => setModal('ticket')}
              onAssign={(ticket) => {
                setSelectedTicket(ticket);
                setModal('assign');
              }}
              onTransition={(ticket, status) => {
                if (status === 'RESOLVED') {
                  setSelectedTicket(ticket);
                  setModal('resolve');
                } else {
                  perform(() => api.updateTicketStatus(ticket.id, { status }), undefined, 'Đã cập nhật trạng thái bảo trì.');
                }
              }}
            />
          )}
          {page === 'members' && (
            <MembersPage
              members={members}
              currentUserId={user.userId}
              currentUserRole={user.role as MembershipRole}
              busy={busy}
              onInvite={() => setModal('invite-member')}
              onUpdateMember={(member, role, active) =>
                perform(
                  () => api.updateMember(member.membershipId, { role, active }),
                  undefined,
                  `Đã cập nhật quyền cho ${member.fullName}.`
                )
              }
            />
          )}
          {page === 'settings' && (
            <SettingsPage
              settings={settings}
              canManage={canManage}
              busy={busy}
              onSave={(nextSettings) =>
                perform(() => api.updateSettings(nextSettings), undefined, 'Đã lưu cấu hình phí định kỳ và hạn thu.')
              }
            />
          )}
          {page === 'audit' && <AuditLogsPage logs={auditLogs} />}
          <footer className="page-footer">
            <span>© 2026 RMS. Quản lý rõ ràng, vận hành nhẹ nhàng.</span>
            <span>Phiên bản 1.0 (RBAC + Tenant Portal + Audit)</span>
          </footer>
        </section>
      </main>
      {modal && (
        <Modal
          title={modalTitle(modal)}
          onClose={() => {
            setModal(null);
            setSelectedTenant(null);
            setSelectedInvoice(null);
            setSelectedContract(null);
            setSelectedHandover(null);
            setSelectedTicket(null);
          }}
        >
          {modal === 'property' && (
            <PropertyForm
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) => perform(() => api.createProperty(body), () => setModal(null), 'Đã thêm bất động sản mới.')}
            />
          )}
          {modal === 'room' && (
            <RoomForm
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) =>
                perform(() => api.createRoom(selectedProperty, body), () => setModal(null), 'Đã thêm phòng mới.')
              }
            />
          )}
          {modal === 'tenant' && (
            <TenantForm
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) => perform(() => api.createTenant(body), () => setModal(null), 'Đã lưu hồ sơ người thuê.')}
            />
          )}
          {modal === 'provision-tenant' && selectedTenant && (
            <ProvisionTenantForm
              tenant={selectedTenant}
              busy={busy}
              onCancel={() => {
                setModal(null);
                setSelectedTenant(null);
              }}
              onSubmit={(body) =>
                perform(
                  () => api.provisionTenantPortal(selectedTenant.id, body),
                  () => {
                    setModal(null);
                    setSelectedTenant(null);
                  },
                  `Đã cấp tài khoản Cổng người thuê cho ${selectedTenant.fullName}.`
                )
              }
            />
          )}
          {modal === 'contract' && (
            <ContractForm
              rooms={allRooms}
              tenants={tenants}
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) =>
                perform(() => api.createContract(body), () => setModal(null), 'Đã tạo hợp đồng bản nháp.')
              }
            />
          )}
          {modal === 'terminate' && selectedContract && (
            <TerminateContractForm
              contract={selectedContract}
              busy={busy}
              onCancel={() => {
                setModal(null);
                setSelectedContract(null);
              }}
              onSubmit={(body) =>
                perform(
                  () => api.terminateContract(selectedContract.id, body),
                  (handover) => {
                    setSelectedHandover(handover);
                    setModal('handover');
                  },
                  'Đã thanh lý hợp đồng, quyết toán tiền cọc và trả phòng về Trống.'
                )
              }
            />
          )}
          {modal === 'handover' && selectedHandover && (
            <HandoverSummary
              handover={selectedHandover}
              onClose={() => {
                setModal(null);
                setSelectedContract(null);
                setSelectedHandover(null);
              }}
            />
          )}
          {modal === 'tariff' && (
            <TariffForm
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) => perform(() => api.createTariff(body), () => setModal(null), 'Đã lưu đơn giá dịch vụ.')}
            />
          )}
          {modal === 'reading' && (
            <ReadingForm
              rooms={allRooms}
              period={period}
              tariffs={tariffs}
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) => perform(() => api.createReading(body), () => setModal(null), 'Đã ghi nhận chỉ số công tơ.')}
              onAddTariff={() => setModal('tariff')}
            />
          )}
          {modal === 'adjust-invoice' && selectedInvoice && (
            <AdjustInvoiceForm
              invoice={selectedInvoice}
              busy={busy}
              onCancel={() => {
                setModal(null);
                setSelectedInvoice(null);
              }}
              onSubmit={(body) =>
                perform(
                  () => api.adjustInvoice(selectedInvoice.id, body),
                  () => {
                    setModal(null);
                    setSelectedInvoice(null);
                  },
                  'Đã cập nhật phụ phí & giảm trừ cho hóa đơn nháp.'
                )
              }
            />
          )}
          {modal === 'payment' && selectedInvoice && (
            <PaymentForm
              invoice={selectedInvoice}
              busy={busy}
              onCancel={() => {
                setModal(null);
                setSelectedInvoice(null);
              }}
              onSubmit={(body) =>
                perform(
                  () => api.recordPayment(selectedInvoice.id, body),
                  () => {
                    setModal(null);
                    setSelectedInvoice(null);
                  },
                  'Đã ghi nhận thanh toán thủ công.'
                )
              }
            />
          )}
          {modal === 'ticket' && (
            <TicketForm
              rooms={allRooms}
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) => perform(() => api.createTicket(body), () => setModal(null), 'Đã tạo yêu cầu bảo trì.')}
            />
          )}
          {modal === 'assign' && selectedTicket && (
            <AssignTicketForm
              ticket={selectedTicket}
              busy={busy}
              onCancel={() => {
                setModal(null);
                setSelectedTicket(null);
              }}
              onSubmit={(assigneeName) =>
                perform(
                  () => api.assignTicket(selectedTicket.id, assigneeName),
                  () => {
                    setModal(null);
                    setSelectedTicket(null);
                  },
                  'Đã phân công xử lý bảo trì.'
                )
              }
            />
          )}
          {modal === 'resolve' && selectedTicket && (
            <ResolveTicketForm
              ticket={selectedTicket}
              busy={busy}
              onCancel={() => {
                setModal(null);
                setSelectedTicket(null);
              }}
              onSubmit={(body) =>
                perform(
                  () => api.updateTicketStatus(selectedTicket.id, { status: 'RESOLVED', ...body }),
                  () => {
                    setModal(null);
                    setSelectedTicket(null);
                  },
                  'Đã hoàn tất sửa chữa.'
                )
              }
            />
          )}
          {modal === 'invite-member' && (
            <InviteMemberForm
              currentUserRole={user.role as MembershipRole}
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(body) =>
                perform(
                  () => api.inviteMember(body),
                  () => setModal(null),
                  'Đã thêm thành viên vào tổ chức.'
                )
              }
            />
          )}
          {modal === 'change-password' && (
            <ChangePasswordForm
              busy={busy}
              onCancel={() => setModal(null)}
              onSubmit={(cur, next) =>
                perform(() => changePassword(cur, next), () => setModal(null), 'Đã cập nhật mật khẩu mới.')
              }
            />
          )}
        </Modal>
      )}
      {menuOpen && <button className="mobile-backdrop" onClick={() => setMenuOpen(false)} aria-label="Đóng menu" />}
    </div>
  );
}

function pageSubtitle(page: PageKey) {
  return {
    dashboard: 'Tình hình vận hành danh mục nhà của bạn.',
    properties: 'Theo dõi danh mục bất động sản và tình trạng từng phòng.',
    tenants: 'Thông tin liên hệ, hồ sơ và tài khoản cổng người thuê.',
    contracts: 'Quản lý thời hạn thuê, kích hoạt và thanh lý bàn giao.',
    utilities: 'Cấu hình đơn giá, cập nhật chỉ số điện nước theo kỳ.',
    invoices: 'Tổng hợp tiền phòng, dịch vụ, điều chỉnh và khoản đã thu.',
    maintenance: 'Tiếp nhận sự cố, phân công và theo dõi chi phí bảo trì.',
    members: 'Quản lý quản lý/nhân viên và phân quyền truy cập trong tổ chức.',
    settings: 'Thiết lập ngày đến hạn hóa đơn và các khoản phí định kỳ mặc định.',
    audit: 'Theo dõi nhật ký đăng nhập, thay đổi quyền, hóa đơn và cấu hình.',
  }[page];
}

function modalTitle(kind: ModalKind) {
  return {
    property: 'Thêm nhà mới',
    room: 'Thêm phòng',
    tenant: 'Thêm người thuê',
    'provision-tenant': 'Cấp tài khoản Cổng người thuê',
    contract: 'Tạo hợp đồng',
    terminate: 'Thanh lý hợp đồng & bàn giao',
    handover: 'Biên bản bàn giao & quyết toán cọc',
    tariff: 'Cấu hình đơn giá',
    reading: 'Ghi chỉ số công tơ',
    'adjust-invoice': 'Điều chỉnh hóa đơn nháp',
    payment: 'Ghi nhận thanh toán',
    ticket: 'Tạo yêu cầu bảo trì',
    assign: 'Phân công nhân viên sửa chữa',
    resolve: 'Hoàn tất xử lý bảo trì',
    'invite-member': 'Mời thành viên mới',
    'change-password': 'Đổi mật khẩu',
    'tenant-ticket': 'Gửi yêu cầu bảo trì',
  }[kind];
}

function ticketStatusText(status: TicketStatus) {
  const map: Record<TicketStatus, string> = {
    OPEN: 'Mới tạo',
    ASSIGNED: 'Đã phân công',
    IN_PROGRESS: 'Đang sửa',
    ON_HOLD: 'Tạm hoãn',
    RESOLVED: 'Đã xử lý',
    CLOSED: 'Đã đóng',
    REOPENED: 'Mở lại',
  };
  return map[status];
}

function priorityText(priority: TicketPriority) {
  const map: Record<TicketPriority, string> = {
    LOW: 'Thấp',
    MEDIUM: 'Trung bình',
    HIGH: 'Cao',
    URGENT: 'Khẩn cấp',
  };
  return map[priority];
}

function Dashboard({
  properties,
  rooms,
  tenants,
  invoices,
  occupancy,
  billed,
  collected,
  totalDue,
  period,
  canManage,
  onNavigate,
  onGenerate,
}: {
  properties: Property[];
  rooms: Room[];
  tenants: Tenant[];
  invoices: Invoice[];
  occupancy: number;
  billed: number;
  collected: number;
  totalDue: number;
  period: string;
  canManage: boolean;
  onNavigate: (page: PageKey) => void;
  onGenerate: () => void;
}) {
  const noData = properties.length === 0;
  const maxInvoiceAmount = Math.max(...invoices.map((item) => item.totalAmount), 1);
  return (
    <>
      {noData && (
        <div className="welcome-banner">
          <div className="welcome-copy">
            <span className="banner-kicker">BẮT ĐẦU TỪ ĐÂY</span>
            <h2>Mọi ngôi nhà, một góc nhìn.</h2>
            <p>Tạo bất động sản đầu tiên để bắt đầu quản lý phòng, người thuê và dòng tiền.</p>
            <button className="button button-light" onClick={() => onNavigate('properties')}>
              Thêm bất động sản <ArrowUpRight size={16} />
            </button>
          </div>
          <div className="banner-art">
            <div className="art-sun" />
            <div className="art-tower tower-a">
              <span />
              <span />
              <span />
              <span />
            </div>
            <div className="art-tower tower-b">
              <span />
              <span />
              <span />
            </div>
            <div className="art-house">
              <span className="art-door" />
              <span className="art-window one" />
              <span className="art-window two" />
            </div>
            <div className="art-ground" />
          </div>
        </div>
      )}
      <div className="metrics-grid">
        <Metric icon={Building2} label="Tổng bất động sản" value={`${properties.length}`} caption="Đang quản lý" tone="violet" />
        <Metric
          icon={DoorOpen}
          label="Phòng đang cho thuê"
          value={`${rooms.filter((room) => room.status === 'OCCUPIED').length} / ${rooms.length}`}
          caption={`${occupancy}% công suất lấp đầy`}
          tone="blue"
        />
        <Metric icon={Users} label="Người thuê" value={`${tenants.length}`} caption="Hồ sơ đang lưu trữ" tone="green" />
        <Metric
          icon={Wallet}
          label="Cần thu kỳ này"
          value={money(totalDue)}
          caption={
            totalDue ? `${invoices.filter((item) => item.balanceDue > 0).length} hóa đơn còn dư nợ` : 'Chưa có công nợ'
          }
          tone="amber"
        />
      </div>
      <div className="dashboard-grid">
        <section className="panel revenue-panel">
          <div className="panel-head">
            <div>
              <span className="panel-kicker">TÀI CHÍNH</span>
              <h3>Dòng tiền kỳ {period}</h3>
            </div>
            <button className="text-button" onClick={() => onNavigate('invoices')}>
              Chi tiết <ArrowUpRight size={14} />
            </button>
          </div>
          <div className="revenue-summary">
            <div>
              <span>Đã lập hóa đơn</span>
              <strong>{money(billed)}</strong>
            </div>
            <div>
              <span>Đã thu tiền</span>
              <strong className="collected-amount">{money(collected)}</strong>
            </div>
            <div>
              <span>Còn phải thu</span>
              <strong>{money(totalDue)}</strong>
            </div>
          </div>
          {invoices.length ? (
            <>
              <div className="chart-area">
                <div className="chart-y">
                  <span>{money(maxInvoiceAmount)}</span>
                  <span>{money(Math.round(maxInvoiceAmount / 2))}</span>
                  <span>0</span>
                </div>
                <div className="chart-columns">
                  {invoices.slice(0, 12).map((invoice) => {
                    const roomCode = rooms.find((room) => room.id === invoice.roomId)?.roomCode ?? '—';
                    const billedHeight = Math.max(8, Math.round((invoice.totalAmount / maxInvoiceAmount) * 100));
                    const paidHeight =
                      invoice.amountPaid > 0 ? Math.max(8, Math.round((invoice.amountPaid / maxInvoiceAmount) * 100)) : 0;
                    return (
                      <div className="chart-column" key={invoice.id}>
                        <div className="bar-pair current">
                          <i style={{ height: `${billedHeight}%` }} title={`Phải thu: ${money(invoice.totalAmount)}`} />
                          <i style={{ height: `${paidHeight}%` }} title={`Đã thu: ${money(invoice.amountPaid)}`} />
                        </div>
                        <span>{roomCode}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
              <div className="chart-legend">
                <span>
                  <i className="legend-blue" />
                  Đã lập hóa đơn
                </span>
                <span>
                  <i className="legend-light" />
                  Đã thu tiền
                </span>
                <small>Kỳ {period}</small>
              </div>
            </>
          ) : (
            <EmptyInline
              icon={Wallet}
              title={`Chưa có hóa đơn kỳ ${period}`}
              text="Lập hóa đơn từ các hợp đồng đang hiệu lực để theo dõi dòng tiền thực tế."
              action={canManage ? 'Tạo hóa đơn' : 'Xem hóa đơn'}
              onClick={canManage ? onGenerate : () => onNavigate('invoices')}
            />
          )}
        </section>
        <section className="panel occupancy-panel">
          <div className="panel-head">
            <div>
              <span className="panel-kicker">VẬN HÀNH</span>
              <h3>Tình trạng phòng</h3>
            </div>
            <button className="icon-button small-icon" onClick={() => onNavigate('properties')} title="Xem danh sách">
              <ArrowUpRight size={16} />
            </button>
          </div>
          <div className="occupancy-body">
            <div className="donut" style={{ '--occupancy': `${occupancy}%` } as React.CSSProperties}>
              <div>
                <strong>
                  {occupancy}
                  <small>%</small>
                </strong>
                <span>Lấp đầy</span>
              </div>
            </div>
            <div className="occupancy-legend">
              <LegendRow
                label="Đang cho thuê"
                value={rooms.filter((room) => room.status === 'OCCUPIED').length}
                color="blue"
              />
              <LegendRow label="Đang trống" value={rooms.filter((room) => room.status === 'AVAILABLE').length} color="gray" />
              <LegendRow label="Bảo trì" value={rooms.filter((room) => room.status === 'MAINTENANCE').length} color="amber" />
            </div>
          </div>
          <div className="occupancy-note">
            <span className="status-dot green-dot" />
            {rooms.length
              ? `${rooms.filter((room) => room.status === 'AVAILABLE').length} phòng đang sẵn sàng cho thuê`
              : 'Thêm nhà và phòng để xem số liệu'}
          </div>
        </section>
      </div>
      <div className="dashboard-grid lower-grid">
        <section className="panel invoices-panel">
          <div className="panel-head">
            <div>
              <span className="panel-kicker">CẬP NHẬT MỚI</span>
              <h3>Hóa đơn gần đây</h3>
            </div>
            <button className="text-button" onClick={() => onNavigate('invoices')}>
              Tất cả hóa đơn <ArrowUpRight size={14} />
            </button>
          </div>
          {invoices.length ? (
            <div className="mini-invoices">
              {invoices.slice(0, 4).map((invoice) => (
                <div className="mini-invoice" key={invoice.id}>
                  <div className="invoice-glyph">
                    <FileText size={17} />
                  </div>
                  <div className="mini-invoice-copy">
                    <strong>
                      Phòng {rooms.find((room) => room.id === invoice.roomId)?.roomCode ?? '—'} · {invoice.billingPeriod}
                    </strong>
                    <span>{tenants.find((tenant) => tenant.id === invoice.tenantId)?.fullName ?? 'Người thuê'}</span>
                  </div>
                  <strong className="mini-invoice-amount">{money(invoice.totalAmount)}</strong>
                  <StatusBadge status={invoice.status} />
                </div>
              ))}
            </div>
          ) : (
            <EmptyInline
              icon={FileText}
              title="Chưa có hóa đơn"
              text="Tạo hóa đơn kỳ này khi hợp đồng và chỉ số đã sẵn sàng."
              action={canManage ? 'Tạo hóa đơn' : 'Xem hóa đơn'}
              onClick={canManage ? onGenerate : () => onNavigate('invoices')}
            />
          )}
        </section>
        <section className="panel quick-panel">
          <div className="panel-head">
            <div>
              <span className="panel-kicker">LỐI TẮT</span>
              <h3>Thao tác nhanh</h3>
            </div>
          </div>
          <div className="quick-actions">
            <QuickAction
              icon={Building2}
              title="Nhà & phòng"
              caption="Quản lý danh mục phòng"
              onClick={() => onNavigate('properties')}
              tone="violet"
            />
            <QuickAction
              icon={Users}
              title="Người thuê"
              caption="Hồ sơ & cổng người thuê"
              onClick={() => onNavigate('tenants')}
              tone="green"
            />
            <QuickAction
              icon={CreditCard}
              title="Hóa đơn"
              caption={`Kỳ ${period}`}
              onClick={canManage ? onGenerate : () => onNavigate('invoices')}
              tone="amber"
            />
            <QuickAction
              icon={Zap}
              title="Ghi chỉ số"
              caption="Cập nhật điện, nước"
              onClick={() => onNavigate('utilities')}
              tone="blue"
            />
          </div>
        </section>
      </div>
    </>
  );
}

function Metric({
  icon: Icon,
  label: title,
  value,
  caption,
  tone,
}: {
  icon: typeof Home;
  label: string;
  value: string;
  caption: string;
  tone: string;
}) {
  return (
    <div className="metric-card">
      <div className={`metric-icon tone-${tone}`}>
        <Icon size={18} strokeWidth={1.8} />
      </div>
      <div className="metric-title">{title}</div>
      <strong className="metric-value">{value}</strong>
      <div className="metric-caption">{caption}</div>
    </div>
  );
}

function LegendRow({ label: text, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className="legend-row">
      <i className={`legend-dot ${color}`} />
      <span>{text}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StatusBadge({ status }: { status: Invoice['status'] }) {
  const labels: Record<Invoice['status'], string> = {
    DRAFT: 'Bản nháp',
    ISSUED: 'Chưa thanh toán',
    PARTIALLY_PAID: 'Thanh toán một phần',
    PAID: 'Đã thanh toán',
    OVERDUE: 'Quá hạn',
    VOID: 'Đã hủy',
  };
  return <span className={`status-badge status-${status.toLowerCase()}`}>{labels[status] ?? status}</span>;
}

function QuickAction({
  icon: Icon,
  title,
  caption,
  onClick,
  tone,
}: {
  icon: typeof Home;
  title: string;
  caption: string;
  onClick: () => void;
  tone: string;
}) {
  return (
    <button className="quick-action" onClick={onClick}>
      <span className={`quick-icon tone-${tone}`}>
        <Icon size={17} />
      </span>
      <span>
        <strong>{title}</strong>
        <small>{caption}</small>
      </span>
      <ArrowUpRight size={15} />
    </button>
  );
}

function EmptyInline({
  icon: Icon,
  title,
  text,
  action,
  onClick,
}: {
  icon: typeof Home;
  title: string;
  text: string;
  action: string;
  onClick: () => void;
}) {
  return (
    <div className="empty-inline">
      <span className="empty-icon">
        <Icon size={20} />
      </span>
      <strong>{title}</strong>
      <p>{text}</p>
      <button className="text-button" onClick={onClick}>
        {action} <ArrowUpRight size={14} />
      </button>
    </div>
  );
}

function PropertiesPage({
  properties,
  rooms,
  selected,
  search,
  busy,
  canManage,
  onSearch,
  onSelect,
  onAddRoom,
  onAddProperty,
  onChangeRoomStatus,
}: {
  properties: Property[];
  rooms: Room[];
  selected: string;
  search: string;
  busy: boolean;
  canManage: boolean;
  onSearch: (value: string) => void;
  onSelect: (id: string) => void;
  onAddRoom: () => void;
  onAddProperty: () => void;
  onChangeRoomStatus: (room: Room, status: RoomStatusValue) => void;
}) {
  return (
    <section className="panel data-panel">
      <div className="data-toolbar">
        <div className="property-select-wrap">
          <span className="toolbar-label">BẤT ĐỘNG SẢN</span>
          <select className="property-select" value={selected} onChange={(event) => onSelect(event.target.value)}>
            <option value="">Chọn bất động sản</option>
            {properties.map((property) => (
              <option key={property.id} value={property.id}>
                {property.name}
              </option>
            ))}
          </select>
        </div>
        <div className="table-actions">
          <label className="search-box">
            <Search size={16} />
            <input placeholder="Tìm phòng..." value={search} onChange={(event) => onSearch(event.target.value)} />
          </label>
          {canManage && (
            <button className="button button-secondary" onClick={onAddRoom} disabled={!selected}>
              <Plus size={15} /> Thêm phòng
            </button>
          )}
        </div>
      </div>
      {!properties.length ? (
        <EmptyInline
          icon={Building2}
          title="Chưa có bất động sản nào"
          text="Thêm nhà trọ hoặc căn hộ để bắt đầu quản lý phòng."
          action={canManage ? 'Thêm nhà' : 'Liên hệ quản lý'}
          onClick={canManage ? onAddProperty : () => undefined}
        />
      ) : (
        <>
          <div className="selected-property-line">
            <span className="property-pin">
              <Building2 size={17} />
            </span>
            <span>
              <strong>{properties.find((property) => property.id === selected)?.name}</strong>
              <small>{properties.find((property) => property.id === selected)?.address}</small>
            </span>
            <span className="room-count">
              <strong>{rooms.length}</strong> phòng
            </span>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>MÃ PHÒNG</th>
                  <th>DIỆN TÍCH</th>
                  <th>SỨC CHỨA</th>
                  <th>GIÁ THUÊ / THÁNG</th>
                  <th>TIỀN CỌC</th>
                  <th>TÌNH TRẠNG</th>
                  <th>THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {rooms.map((room) => (
                  <tr key={room.id}>
                    <td>
                      <strong className="room-code">{room.roomCode}</strong>
                    </td>
                    <td>{room.areaSquareMeters} m²</td>
                    <td>{room.capacity} người</td>
                    <td className="rent-cell">{money(room.monthlyRent)}</td>
                    <td>{money(room.depositAmount)}</td>
                    <td>
                      <RoomStatus status={room.status} />
                    </td>
                    <td>
                      {canManage && room.status !== 'OCCUPIED' && (
                        <button
                          className="button button-small button-secondary"
                          disabled={busy}
                          onClick={() =>
                            onChangeRoomStatus(room, room.status === 'AVAILABLE' ? 'MAINTENANCE' : 'AVAILABLE')
                          }
                        >
                          {room.status === 'AVAILABLE' ? 'Bảo trì' : 'Mở lại'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!rooms.length && (
              <div className="table-empty">
                Chưa có phòng tại bất động sản này. Dùng nút “Thêm phòng” để bắt đầu.
              </div>
            )}
          </div>
          <div className="table-foot">
            <span>
              Hiển thị <strong>{rooms.length}</strong> phòng
            </span>
            <span>
              Dữ liệu thuộc tổ chức hiện tại <ShieldCheck size={14} />
            </span>
          </div>
        </>
      )}
    </section>
  );
}

function RoomStatus({ status }: { status: Room['status'] }) {
  const copy = {
    AVAILABLE: ['Trống', 'green'],
    OCCUPIED: ['Đang thuê', 'blue'],
    MAINTENANCE: ['Bảo trì', 'amber'],
    INACTIVE: ['Ngừng hoạt động', 'gray'],
  } as const;
  const [text, tone] = copy[status];
  return (
    <span className={`room-status ${tone}`}>
      <i />
      {text}
    </span>
  );
}

function TenantsPage({
  tenants,
  canManage,
  onAdd,
  onProvisionPortal,
}: {
  tenants: Tenant[];
  canManage: boolean;
  onAdd: () => void;
  onProvisionPortal: (tenant: Tenant) => void;
}) {
  return (
    <section className="panel data-panel">
      <div className="data-toolbar">
        <div>
          <span className="toolbar-label">DANH BẠ NGƯỜI THUÊ</span>
          <div className="toolbar-subtitle">Thông tin và tài khoản cổng người thuê trong tổ chức của bạn.</div>
        </div>
        {canManage && (
          <button className="button button-primary" onClick={onAdd}>
            <Plus size={16} /> Thêm người thuê
          </button>
        )}
      </div>
      {tenants.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>HỌ VÀ TÊN</th>
                <th>SỐ ĐIỆN THOẠI</th>
                <th>EMAIL</th>
                <th>CỔNG NGƯỜI THUÊ</th>
                <th>THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {tenants.map((tenant, index) => (
                <tr key={tenant.id}>
                  <td>
                    <span className="tenant-name">
                      <span className={`tenant-avatar avatar-color-${index % 4}`}>{tenant.fullName.slice(-1)}</span>
                      <strong>{tenant.fullName}</strong>
                    </span>
                  </td>
                  <td>{tenant.phone || '—'}</td>
                  <td>{tenant.email || '—'}</td>
                  <td>
                    {tenant.hasPortalAccount ? (
                      <span className="room-status blue">
                        <i />
                        Đã kích hoạt
                      </span>
                    ) : (
                      <span className="room-status green">
                        <i />
                        Chưa có tài khoản
                      </span>
                    )}
                  </td>
                  <td>
                    {canManage && !tenant.hasPortalAccount && (
                      <button className="button button-small button-secondary" onClick={() => onProvisionPortal(tenant)}>
                        Cấp tài khoản
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <EmptyInline
          icon={Users}
          title="Danh bạ còn trống"
          text="Tạo hồ sơ người thuê trước khi thêm hợp đồng."
          action={canManage ? 'Thêm người thuê' : 'Chưa có dữ liệu'}
          onClick={canManage ? onAdd : () => undefined}
        />
      )}
    </section>
  );
}

function ContractsPage({
  contracts,
  rooms,
  tenants,
  busy,
  canManage,
  onAdd,
  onActivate,
  onTerminate,
  onViewHandover,
}: {
  contracts: Contract[];
  rooms: Room[];
  tenants: Tenant[];
  busy: boolean;
  canManage: boolean;
  onAdd: () => void;
  onActivate: (contract: Contract) => void;
  onTerminate: (contract: Contract) => void;
  onViewHandover: (contract: Contract) => void;
}) {
  return (
    <section className="panel data-panel">
      <div className="data-toolbar">
        <div>
          <span className="toolbar-label">HỒ SƠ THUÊ NHÀ</span>
          <div className="toolbar-subtitle">Hợp đồng chỉ có hiệu lực sau khi kích hoạt và giải phóng phòng khi thanh lý.</div>
        </div>
        {canManage && (
          <button className="button button-primary" onClick={onAdd} disabled={!rooms.length || !tenants.length}>
            <Plus size={16} /> Tạo hợp đồng
          </button>
        )}
      </div>
      {contracts.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>NGƯỜI THUÊ</th>
                <th>PHÒNG</th>
                <th>THỜI HẠN</th>
                <th>TIỀN THUÊ / THÁNG</th>
                <th>TIỀN CỌC</th>
                <th>TRẠNG THÁI</th>
                <th>THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {contracts.map((contract) => (
                <tr key={contract.id}>
                  <td>
                    <strong>{tenants.find((tenant) => tenant.id === contract.tenantId)?.fullName ?? 'Người thuê'}</strong>
                  </td>
                  <td>{rooms.find((room) => room.id === contract.roomId)?.roomCode ?? '—'}</td>
                  <td>
                    {new Intl.DateTimeFormat('vi-VN').format(new Date(`${contract.startDate}T00:00:00`))} –{' '}
                    {new Intl.DateTimeFormat('vi-VN').format(new Date(`${contract.endDate}T00:00:00`))}
                  </td>
                  <td className="rent-cell">{money(contract.monthlyRent)}</td>
                  <td>{money(contract.depositAmount)}</td>
                  <td>
                    <span className={`status-badge status-${contract.status.toLowerCase()}`}>
                      {contract.status === 'ACTIVE'
                        ? 'Đang hiệu lực'
                        : contract.status === 'DRAFT'
                          ? 'Bản nháp'
                          : 'Đã thanh lý'}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions">
                      {canManage && contract.status === 'DRAFT' && (
                        <button
                          className="button button-small button-primary"
                          disabled={busy}
                          onClick={() => onActivate(contract)}
                        >
                          Kích hoạt
                        </button>
                      )}
                      {canManage && contract.status === 'ACTIVE' && (
                        <button
                          className="button button-small button-secondary"
                          disabled={busy}
                          onClick={() => onTerminate(contract)}
                        >
                          Thanh lý
                        </button>
                      )}
                      {contract.status === 'TERMINATED' && (
                        <button
                          className="button button-small button-secondary"
                          disabled={busy}
                          onClick={() => onViewHandover(contract)}
                        >
                          Biên bản
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <EmptyInline
          icon={FileText}
          title="Chưa có hợp đồng"
          text={
            rooms.length && tenants.length
              ? 'Tạo hợp đồng để gắn người thuê với phòng.'
              : 'Cần tạo ít nhất một phòng và một hồ sơ người thuê trước.'
          }
          action={canManage ? 'Tạo hợp đồng' : 'Chưa có dữ liệu'}
          onClick={canManage ? onAdd : () => undefined}
        />
      )}
    </section>
  );
}

function MaintenancePage({
  tickets,
  rooms,
  busy,
  onAdd,
  onAssign,
  onTransition,
}: {
  tickets: MaintenanceTicket[];
  rooms: Room[];
  busy: boolean;
  onAdd: () => void;
  onAssign: (ticket: MaintenanceTicket) => void;
  onTransition: (ticket: MaintenanceTicket, status: TicketStatus) => void;
}) {
  return (
    <section className="panel data-panel">
      <div className="data-toolbar">
        <div>
          <span className="toolbar-label">YÊU CẦU BẢO TRÌ</span>
          <div className="toolbar-subtitle">Theo dõi trạng thái sửa chữa, người phụ trách và chi phí thực tế.</div>
        </div>
        <button className="button button-primary" onClick={onAdd} disabled={!rooms.length}>
          <Plus size={16} /> Tạo yêu cầu bảo trì
        </button>
      </div>
      {tickets.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>PHÒNG</th>
                <th>TIÊU ĐỀ</th>
                <th>MỨC ĐỘ</th>
                <th>PHỤ TRÁCH</th>
                <th>CHI PHÍ DỰ KIẾN / THỰC TẾ</th>
                <th>TRẠNG THÁI</th>
                <th>THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((ticket) => (
                <tr key={ticket.id}>
                  <td>
                    <strong className="room-code">{rooms.find((room) => room.id === ticket.roomId)?.roomCode ?? '—'}</strong>
                  </td>
                  <td>
                    <strong>{ticket.title}</strong>
                    <div style={{ fontSize: 11, color: '#68707e' }}>{ticket.description}</div>
                  </td>
                  <td>{priorityText(ticket.priority)}</td>
                  <td>{ticket.assigneeName || 'Chưa phân công'}</td>
                  <td>
                    {money(ticket.estimatedCost)} / <strong>{money(ticket.actualCost)}</strong>
                  </td>
                  <td>
                    <span className={`status-badge status-${ticket.status.toLowerCase()}`}>
                      {ticketStatusText(ticket.status)}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions">
                      {(ticket.status === 'OPEN' || ticket.status === 'REOPENED') && (
                        <button
                          className="button button-small button-secondary"
                          disabled={busy}
                          onClick={() => onAssign(ticket)}
                        >
                          Phân công
                        </button>
                      )}
                      {(ticket.status === 'ASSIGNED' || ticket.status === 'ON_HOLD' || ticket.status === 'OPEN') && (
                        <button
                          className="button button-small button-secondary"
                          disabled={busy}
                          onClick={() => onTransition(ticket, 'IN_PROGRESS')}
                        >
                          Xử lý
                        </button>
                      )}
                      {(ticket.status === 'ASSIGNED' ||
                        ticket.status === 'IN_PROGRESS' ||
                        ticket.status === 'REOPENED') && (
                        <button
                          className="button button-small button-primary"
                          disabled={busy}
                          onClick={() => onTransition(ticket, 'RESOLVED')}
                        >
                          Hoàn tất
                        </button>
                      )}
                      {ticket.status === 'RESOLVED' && (
                        <button
                          className="button button-small button-secondary"
                          disabled={busy}
                          onClick={() => onTransition(ticket, 'CLOSED')}
                        >
                          Đóng
                        </button>
                      )}
                      {ticket.status === 'CLOSED' && (
                        <button
                          className="button button-small button-secondary"
                          disabled={busy}
                          onClick={() => onTransition(ticket, 'REOPENED')}
                        >
                          Mở lại
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <EmptyInline
          icon={Wrench}
          title="Chưa có yêu cầu bảo trì"
          text={rooms.length ? 'Ghi nhận sự cố phòng để phân công và theo dõi chi phí.' : 'Cần tạo phòng trước khi ghi nhận bảo trì.'}
          action="Tạo yêu cầu bảo trì"
          onClick={onAdd}
        />
      )}
    </section>
  );
}

function UtilitiesPage({
  properties,
  rooms,
  tariffs,
  readings,
  period,
  canManage,
  onAddTariff,
  onAddReading,
}: {
  properties: Property[];
  rooms: Room[];
  tariffs: Tariff[];
  readings: MeterReading[];
  period: string;
  canManage: boolean;
  onAddTariff: () => void;
  onAddReading: () => void;
}) {
  return (
    <div className="utilities-layout">
      <section className="panel data-panel">
        <div className="panel-head utility-heading">
          <div>
            <span className="panel-kicker">GIÁ DỊCH VỤ</span>
            <h3>Đơn giá đang áp dụng</h3>
          </div>
          {canManage && (
            <button className="button button-secondary" onClick={onAddTariff}>
              <Plus size={15} /> Thêm đơn giá
            </button>
          )}
        </div>
        {tariffs.length ? (
          <div className="tariff-grid">
            {tariffs.map((tariff) => (
              <div className="tariff-card" key={tariff.id}>
                <span className={`tariff-symbol ${tariff.utilityType === 'WATER' ? 'water' : ''}`}>
                  {tariff.utilityType === 'WATER' ? <Droplets size={19} /> : <Zap size={19} />}
                </span>
                <span className="tariff-info">
                  <small>{tariff.utilityType === 'WATER' ? 'NƯỚC' : 'ĐIỆN'}</small>
                  <strong>
                    {money(tariff.pricePerUnit)} <small>/ đơn vị</small>
                  </strong>
                  <span>Hiệu lực từ {new Intl.DateTimeFormat('vi-VN').format(new Date(`${tariff.effectiveFrom}T00:00:00`))}</span>
                </span>
              </div>
            ))}
          </div>
        ) : (
          <EmptyInline
            icon={Zap}
            title="Chưa cấu hình đơn giá"
            text="Thiết lập đơn giá thủ công theo hợp đồng hoặc chính sách của bạn."
            action={canManage ? 'Cấu hình đơn giá' : 'Chưa có dữ liệu'}
            onClick={canManage ? onAddTariff : () => undefined}
          />
        )}
      </section>
      <section className="panel data-panel">
        <div className="panel-head utility-heading">
          <div>
            <span className="panel-kicker">CHỈ SỐ CÔNG TƠ</span>
            <h3>Kỳ {period}</h3>
          </div>
          <button className="button button-primary" onClick={onAddReading} disabled={!rooms.length}>
            <Plus size={15} /> Ghi chỉ số
          </button>
        </div>
        <div className="utility-callout">
          <span className="callout-icon">
            <ShieldCheck size={18} />
          </span>
          <p>Chỉ số được nhập theo từng phòng và kỳ. Hệ thống từ chối bản ghi trùng hoặc nhập lùi kỳ để bảo toàn số liệu.</p>
        </div>
        {!properties.length ? (
          <div className="table-empty">Thêm bất động sản và phòng trước khi ghi chỉ số.</div>
        ) : readings.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>PHÒNG</th>
                  <th>DỊCH VỤ</th>
                  <th>CHỈ SỐ CŨ</th>
                  <th>CHỈ SỐ MỚI</th>
                  <th>TIÊU THỤ</th>
                  <th>ĐƠN GIÁ</th>
                  <th>THÀNH TIỀN</th>
                </tr>
              </thead>
              <tbody>
                {readings.map((reading) => (
                  <tr key={reading.id}>
                    <td>
                      <strong className="room-code">{rooms.find((room) => room.id === reading.roomId)?.roomCode ?? '—'}</strong>
                    </td>
                    <td>{reading.utilityType === 'ELECTRICITY' ? 'Điện' : 'Nước'}</td>
                    <td>{reading.previousReading}</td>
                    <td>{reading.currentReading}</td>
                    <td>{reading.usage}</td>
                    <td>{money(reading.unitPrice)}</td>
                    <td className="rent-cell">{money(reading.charge)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="table-empty">Chưa có chỉ số công tơ được ghi nhận trong kỳ {period}.</div>
        )}
      </section>
    </div>
  );
}

function InvoicesPage({
  invoices,
  tenants,
  rooms,
  busy,
  canManage,
  onGenerate,
  onAdjust,
  onIssue,
  onPay,
}: {
  invoices: Invoice[];
  tenants: Tenant[];
  rooms: Room[];
  busy: boolean;
  canManage: boolean;
  onGenerate: () => void;
  onAdjust: (invoice: Invoice) => void;
  onIssue: (invoice: Invoice) => void;
  onPay: (invoice: Invoice) => void;
}) {
  return (
    <section className="panel data-panel">
      <div className="data-toolbar">
        <div>
          <span className="toolbar-label">BẢNG KÊ THANH TOÁN</span>
          <div className="toolbar-subtitle">Bảng kê nội bộ, hỗ trợ phí định kỳ & điều chỉnh trước khi phát hành.</div>
        </div>
        <span className="secure-label">
          <ShieldCheck size={15} /> Ghi nhận thủ công
        </span>
      </div>
      {invoices.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>NGƯỜI THUÊ / PHÒNG</th>
                <th>TIỀN PHÒNG</th>
                <th>ĐIỆN + NƯỚC</th>
                <th>DỊCH VỤ / GIẢM TRỪ</th>
                <th>TỔNG CỘNG</th>
                <th>CÒN PHẢI THU</th>
                <th>TRẠNG THÁI</th>
                <th>THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map((invoice) => (
                <tr key={invoice.id}>
                  <td>
                    <span className="invoice-person">
                      <strong>{tenants.find((tenant) => tenant.id === invoice.tenantId)?.fullName ?? 'Người thuê'}</strong>
                      <small>
                        Phòng {rooms.find((room) => room.id === invoice.roomId)?.roomCode ?? '—'} · {invoice.billingPeriod}
                      </small>
                    </span>
                  </td>
                  <td>{money(invoice.rentAmount)}</td>
                  <td>{money(invoice.electricityAmount + invoice.waterAmount)}</td>
                  <td>
                    +{money(invoice.serviceAmount ?? 0)} / -{money(invoice.discountAmount ?? 0)}
                    {invoice.adjustmentNotes && (
                      <div style={{ fontSize: 11, color: '#68707e' }}>{invoice.adjustmentNotes}</div>
                    )}
                  </td>
                  <td className="rent-cell">{money(invoice.totalAmount)}</td>
                  <td className={invoice.balanceDue > 0 ? 'due-cell' : 'paid-cell'}>{money(invoice.balanceDue)}</td>
                  <td>
                    <StatusBadge status={invoice.status} />
                  </td>
                  <td>
                    <div className="row-actions">
                      {canManage && invoice.status === 'DRAFT' && (
                        <>
                          <button
                            className="button button-small button-secondary"
                            disabled={busy}
                            onClick={() => onAdjust(invoice)}
                          >
                            Điều chỉnh
                          </button>
                          <button
                            className="button button-small button-secondary"
                            disabled={busy}
                            onClick={() => onIssue(invoice)}
                          >
                            Phát hành
                          </button>
                        </>
                      )}
                      {invoice.balanceDue > 0 && invoice.status !== 'DRAFT' && invoice.status !== 'VOID' && (
                        <button
                          className="button button-small button-primary"
                          disabled={busy}
                          onClick={() => onPay(invoice)}
                        >
                          Ghi thu
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <EmptyInline
          icon={FileText}
          title="Chưa có hóa đơn trong kỳ"
          text="Tạo hóa đơn từ các hợp đồng đang hiệu lực, sau khi hoàn tất chỉ số."
          action={canManage ? 'Tạo hóa đơn' : 'Chưa có dữ liệu'}
          onClick={canManage ? onGenerate : () => undefined}
        />
      )}
    </section>
  );
}

function MembersPage({
  members,
  currentUserId,
  currentUserRole,
  busy,
  onInvite,
  onUpdateMember,
}: {
  members: Member[];
  currentUserId: string;
  currentUserRole: MembershipRole;
  busy: boolean;
  onInvite: () => void;
  onUpdateMember: (member: Member, role: MembershipRole, active: boolean) => void;
}) {
  return (
    <section className="panel data-panel">
      <div className="data-toolbar">
        <div>
          <span className="toolbar-label">THÀNH VIÊN & PHÂN QUYỀN</span>
          <div className="toolbar-subtitle">
            OWNER toàn quyền; MANAGER vận hành & mời nhân viên; STAFF ghi chỉ số/thu tiền/bảo trì; TENANT chỉ xem Cổng người thuê.
          </div>
        </div>
        <button className="button button-primary" onClick={onInvite}>
          <Plus size={16} /> Mời thành viên
        </button>
      </div>
      {members.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>HỌ VÀ TÊN</th>
                <th>EMAIL</th>
                <th>VAI TRÒ</th>
                <th>TRẠNG THÁI</th>
                <th>THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {members.map((member) => {
                const isSelf = member.userId === currentUserId;
                const lockedForManager = currentUserRole !== 'OWNER' && (member.role === 'OWNER' || member.role === 'MANAGER');
                const canEdit = !isSelf && !lockedForManager && member.role !== 'OWNER';
                return (
                  <tr key={member.membershipId}>
                    <td>
                      <strong>{member.fullName}</strong>
                    </td>
                    <td>{member.email}</td>
                    <td>
                      <span className="status-badge status-issued">{roleLabel[member.role] ?? member.role}</span>
                    </td>
                    <td>
                      <span className={`room-status ${member.active ? 'green' : 'amber'}`}>
                        <i />
                        {member.active ? 'Đang hoạt động' : 'Tạm khóa'}
                      </span>
                    </td>
                    <td>
                      {canEdit && (
                        <div className="row-actions">
                          {member.role !== 'TENANT' && (
                            <button
                              className="button button-small button-secondary"
                              disabled={busy}
                              onClick={() =>
                                onUpdateMember(member, member.role === 'STAFF' ? 'MANAGER' : 'STAFF', member.active)
                              }
                            >
                              {member.role === 'STAFF' ? 'Nâng lên Quản lý' : 'Chuyển Nhân viên'}
                            </button>
                          )}
                          <button
                            className="button button-small button-secondary"
                            disabled={busy}
                            onClick={() => onUpdateMember(member, member.role, !member.active)}
                          >
                            {member.active ? 'Tạm khóa' : 'Mở khóa'}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="table-empty">Chưa có danh sách thành viên.</div>
      )}
    </section>
  );
}

function SettingsPage({
  settings,
  canManage,
  busy,
  onSave,
}: {
  settings: OrganizationSettings;
  canManage: boolean;
  busy: boolean;
  onSave: (next: OrganizationSettings) => void;
}) {
  const [billingDueDay, setBillingDueDay] = useState(`${settings.billingDueDay}`);
  const [internetFee, setInternetFee] = useState(`${settings.internetFee}`);
  const [garbageFee, setGarbageFee] = useState(`${settings.garbageFee}`);
  const [parkingFee, setParkingFee] = useState(`${settings.parkingFee}`);

  useEffect(() => {
    setBillingDueDay(`${settings.billingDueDay}`);
    setInternetFee(`${settings.internetFee}`);
    setGarbageFee(`${settings.garbageFee}`);
    setParkingFee(`${settings.parkingFee}`);
  }, [settings]);

  const totalDefaultService = Number(internetFee || 0) + Number(garbageFee || 0) + Number(parkingFee || 0);

  return (
    <section className="panel data-panel">
      <div className="data-toolbar">
        <div>
          <span className="toolbar-label">CÀI ĐẶT TỔ CHỨC</span>
          <div className="toolbar-subtitle">
            Các khoản phí định kỳ này tự động cộng vào Phí dịch vụ khi tạo hóa đơn nháp hàng tháng.
          </div>
        </div>
        <span className="secure-label">
          <ShieldCheck size={15} /> Tổng phí dịch vụ mặc định: {money(totalDefaultService)}
        </span>
      </div>
      <form
        className="modal-form"
        style={{ maxWidth: 560 }}
        onSubmit={(event) => {
          event.preventDefault();
          onSave({
            billingDueDay: Number(billingDueDay),
            internetFee: Number(internetFee || 0),
            garbageFee: Number(garbageFee || 0),
            parkingFee: Number(parkingFee || 0),
          });
        }}
      >
        <label>
          Ngày đến hạn thu tiền hàng tháng (1–28)
          <input
            required
            type="number"
            min={1}
            max={28}
            disabled={!canManage}
            value={billingDueDay}
            onChange={(event) => setBillingDueDay(event.target.value)}
          />
        </label>
        <div className="form-row">
          <label>
            Phí Internet mặc định (₫)
            <input
              required
              type="number"
              min={0}
              step={1000}
              disabled={!canManage}
              value={internetFee}
              onChange={(event) => setInternetFee(event.target.value)}
            />
          </label>
          <label>
            Phí vệ sinh / rác mặc định (₫)
            <input
              required
              type="number"
              min={0}
              step={1000}
              disabled={!canManage}
              value={garbageFee}
              onChange={(event) => setGarbageFee(event.target.value)}
            />
          </label>
        </div>
        <label>
          Phí gửi xe mặc định (₫)
          <input
            required
            type="number"
            min={0}
            step={1000}
            disabled={!canManage}
            value={parkingFee}
            onChange={(event) => setParkingFee(event.target.value)}
          />
        </label>
        {canManage ? (
          <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
            <button className="button button-primary" type="submit" disabled={busy}>
              {busy ? 'Đang lưu...' : 'Lưu cài đặt'} <Check size={15} />
            </button>
          </div>
        ) : (
          <div className="form-note">Chỉ Chủ sở hữu hoặc Quản lý mới có quyền chỉnh sửa cấu hình này.</div>
        )}
      </form>
    </section>
  );
}

function AuditLogsPage({ logs }: { logs: AuditLog[] }) {
  return (
    <section className="panel data-panel">
      <div className="data-toolbar">
        <div>
          <span className="toolbar-label">NHẬT KÝ KIỂM TOÁN</span>
          <div className="toolbar-subtitle">Hiển thị 50 thao tác bảo mật và kế toán gần nhất trong tổ chức.</div>
        </div>
        <span className="secure-label">
          <ShieldCheck size={15} /> Chỉ đọc (Immutable)
        </span>
      </div>
      {logs.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>THỜI GIAN</th>
                <th>NGƯỜI THỰC HIỆN</th>
                <th>HÀNH ĐỘNG</th>
                <th>ĐỐI TƯỢNG</th>
                <th>CHI TIẾT</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((entry) => (
                <tr key={entry.id}>
                  <td>{new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(entry.createdAt))}</td>
                  <td>{entry.actorEmail}</td>
                  <td>
                    <span className="status-badge status-issued">{entry.action}</span>
                  </td>
                  <td>{entry.targetType}</td>
                  <td>{entry.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="table-empty">Chưa có sự kiện nào được ghi nhận.</div>
      )}
    </section>
  );
}

function AuthScreen({
  onSubmit,
  busy,
  error,
}: {
  onSubmit: (email: string, password: string, registration?: { fullName: string; organizationName: string }) => void;
  busy: boolean;
  error: string;
}) {
  const [registerMode, setRegisterMode] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [authNote, setAuthNote] = useState('');
  return (
    <div className="auth-screen">
      <div className="auth-left">
        <a className="brand auth-brand" href="#top" onClick={(event) => event.preventDefault()}>
          <span className="brand-mark">
            <Building2 size={19} />
          </span>
          <span>
            rms<span className="brand-dot">.</span>
          </span>
          <small>NHÀ CHO THUÊ</small>
        </a>
        <div className="auth-content">
          <div className="auth-kicker">
            <span />
            QUẢN LÝ NHÀ CHO THUÊ ĐƠN GIẢN HƠN
          </div>
          <h1>
            Một nơi cho
            <br />
            mọi <em>ngôi nhà.</em>
          </h1>
          <p>Theo dõi phòng, người thuê và hóa đơn—rõ ràng từ ngày đầu tiên.</p>
          <div className="auth-visual">
            <div className="auth-visual-top">
              <span>KHÔNG GIAN CỦA BẠN</span>
              <span className="visual-live">
                <i /> TRỰC TUYẾN
              </span>
            </div>
            <div className="visual-home">
              <div className="visual-skyline">
                <span />
                <span />
                <span />
                <span />
                <i />
              </div>
              <div className="visual-ground" />
            </div>
            <div className="visual-stat">
              <span className="visual-stat-icon">
                <DoorOpen size={15} />
              </span>
              <div>
                <small>TÌNH TRẠNG PHÒNG</small>
                <strong>Mọi thứ trong tầm mắt.</strong>
              </div>
              <span className="visual-stat-right">↗</span>
            </div>
          </div>
        </div>
        <div className="auth-legal">
          © 2026 RMS <span>•</span> Dữ liệu của bạn chỉ thuộc về bạn.
        </div>
      </div>
      <div className="auth-right">
        <div className="auth-form-wrap">
          <div className="auth-form-heading">
            <span className="form-kicker">{registerMode ? 'BẮT ĐẦU MIỄN PHÍ' : 'CHÀO MỪNG TRỞ LẠI'}</span>
            <h2>{registerMode ? 'Tạo không gian của bạn' : 'Đăng nhập vào RMS'}</h2>
            <p>{registerMode ? 'Tạo tài khoản chủ nhà để quản lý danh mục.' : 'Nhập thông tin tài khoản để tiếp tục.'}</p>
          </div>
          {error && (
            <div className="alert error-alert" role="alert">
              <CircleHelp size={16} />
              <span>{error}</span>
            </div>
          )}
          {authNote && (
            <div className="alert" style={{ background: '#f1f4fb', color: '#3955a3', border: '1px solid #d5dff6' }}>
              <span>{authNote}</span>
            </div>
          )}
          <form
            onSubmit={(event) => {
              event.preventDefault();
              setAuthNote('');
              onSubmit(email, password, registerMode ? { fullName, organizationName } : undefined);
            }}
            className="auth-form"
          >
            {registerMode && (
              <>
                <label>
                  Họ và tên
                  <input
                    required
                    maxLength={120}
                    autoComplete="name"
                    value={fullName}
                    onChange={(event) => setFullName(event.target.value)}
                    placeholder="Ví dụ: Nguyễn Minh Anh"
                  />
                </label>
                <label>
                  Tên tổ chức / nhà trọ
                  <input
                    required
                    maxLength={120}
                    value={organizationName}
                    onChange={(event) => setOrganizationName(event.target.value)}
                    placeholder="Ví dụ: An House"
                  />
                </label>
              </>
            )}
            <label>
              Email
              <input
                required
                type="email"
                maxLength={254}
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="ten@email.com"
              />
            </label>
            <label>
              Mật khẩu
              <input
                required
                type="password"
                minLength={12}
                maxLength={72}
                autoComplete={registerMode ? 'new-password' : 'current-password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder={registerMode ? 'Tối thiểu 12 ký tự' : 'Nhập mật khẩu của bạn'}
              />
            </label>
            {!registerMode && (
              <button
                type="button"
                className="forgot-link"
                onClick={() =>
                  setAuthNote(
                    'Liên hệ Chủ sở hữu / Quản lý tổ chức để được hỗ trợ tài khoản, hoặc dùng Đổi mật khẩu sau khi đăng nhập.'
                  )
                }
              >
                Quên mật khẩu?
              </button>
            )}
            <button className="button button-primary auth-submit" type="submit" disabled={busy}>
              {busy ? 'Đang xử lý...' : registerMode ? 'Tạo tài khoản' : 'Đăng nhập'} <ArrowUpRight size={16} />
            </button>
          </form>
          <div className="auth-switch">
            {registerMode ? 'Đã có tài khoản?' : 'Chưa có tài khoản?'}{' '}
            <button
              type="button"
              onClick={() => {
                setRegisterMode(!registerMode);
                setAuthNote('');
              }}
            >
              {registerMode ? 'Đăng nhập' : 'Tạo tài khoản'}
            </button>
          </div>
          <div className="auth-secure">
            <ShieldCheck size={15} /> Phiên đăng nhập được bảo vệ giới hạn thử sai, dữ liệu tách biệt theo tổ chức.
          </div>
        </div>
        <div className="auth-right-footer">
          <span>Dữ liệu tách biệt theo từng tổ chức trên máy chủ nội bộ.</span>
        </div>
      </div>
    </div>
  );
}

function Modal({ title, children, onClose }: { title: string; children: React.ReactNode; onClose: () => void }) {
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div className="modal-head">
          <div>
            <span className="panel-kicker">RMS · QUẢN LÝ NHÀ</span>
            <h2 id="modal-title">{title}</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Đóng">
            <X size={18} />
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}

function FormActions({ busy, onCancel, submitLabel }: { busy: boolean; onCancel: () => void; submitLabel: string }) {
  return (
    <div className="form-actions">
      <button className="button button-secondary" type="button" onClick={onCancel}>
        Hủy
      </button>
      <button className="button button-primary" type="submit" disabled={busy}>
        {busy ? 'Đang lưu...' : submitLabel}
        <Check size={15} />
      </button>
    </div>
  );
}

function PropertyForm({
  busy,
  onCancel,
  onSubmit,
}: {
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { name: string; address: string }) => void;
}) {
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ name, address });
      }}
    >
      <label>
        Tên bất động sản
        <input
          required
          maxLength={120}
          autoFocus
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="Ví dụ: Nhà trọ An Bình"
        />
      </label>
      <label>
        Địa chỉ
        <input
          required
          maxLength={300}
          value={address}
          onChange={(event) => setAddress(event.target.value)}
          placeholder="Số nhà, đường, phường/xã, tỉnh/thành"
        />
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Tạo bất động sản" />
    </form>
  );
}

function RoomForm({
  busy,
  onCancel,
  onSubmit,
}: {
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: {
    roomCode: string;
    areaSquareMeters: number;
    capacity: number;
    monthlyRent: number;
    depositAmount: number;
  }) => void;
}) {
  const [roomCode, setRoomCode] = useState('');
  const [area, setArea] = useState('18');
  const [capacity, setCapacity] = useState('1');
  const [rent, setRent] = useState('');
  const [deposit, setDeposit] = useState('');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({
          roomCode,
          areaSquareMeters: Number(area),
          capacity: Number(capacity),
          monthlyRent: Number(rent),
          depositAmount: Number(deposit || 0),
        });
      }}
    >
      <label>
        Mã phòng
        <input
          required
          maxLength={40}
          autoFocus
          value={roomCode}
          onChange={(event) => setRoomCode(event.target.value)}
          placeholder="Ví dụ: P101"
        />
      </label>
      <div className="form-row">
        <label>
          Diện tích (m²)
          <input required type="number" min="0.01" step="0.01" value={area} onChange={(event) => setArea(event.target.value)} />
        </label>
        <label>
          Sức chứa
          <input required type="number" min="1" step="1" value={capacity} onChange={(event) => setCapacity(event.target.value)} />
        </label>
      </div>
      <label>
        Giá thuê hàng tháng (₫)
        <input required type="number" min="0" step="1000" value={rent} onChange={(event) => setRent(event.target.value)} placeholder="3500000" />
      </label>
      <label>
        Tiền cọc (₫)
        <input required type="number" min="0" step="1000" value={deposit} onChange={(event) => setDeposit(event.target.value)} placeholder="3500000" />
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Lưu phòng" />
    </form>
  );
}

function TenantForm({
  busy,
  onCancel,
  onSubmit,
}: {
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { fullName: string; phone?: string; email?: string }) => void;
}) {
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ fullName, phone: phone || undefined, email: email || undefined });
      }}
    >
      <label>
        Họ và tên
        <input
          required
          autoFocus
          maxLength={120}
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          placeholder="Nguyễn Minh Anh"
        />
      </label>
      <label>
        Số điện thoại
        <input type="tel" maxLength={40} value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="09xx xxx xxx" />
      </label>
      <label>
        Email (không bắt buộc)
        <input type="email" maxLength={254} value={email} onChange={(event) => setEmail(event.target.value)} placeholder="ten@email.com" />
      </label>
      <div className="form-note">
        <ShieldCheck size={15} /> Chỉ lưu thông tin liên hệ cần thiết. Không yêu cầu giấy tờ định danh.
      </div>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Lưu người thuê" />
    </form>
  );
}

function ProvisionTenantForm({
  tenant,
  busy,
  onCancel,
  onSubmit,
}: {
  tenant: Tenant;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { email: string; password: string }) => void;
}) {
  const [email, setEmail] = useState(tenant.email ?? '');
  const [password, setPassword] = useState('');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ email, password });
      }}
    >
      <div className="form-note">
        <ShieldCheck size={15} /> Tài khoản đăng nhập vào Cổng người thuê cho <strong>{tenant.fullName}</strong>.
      </div>
      <label>
        Email đăng nhập
        <input required type="email" maxLength={254} autoFocus value={email} onChange={(event) => setEmail(event.target.value)} />
      </label>
      <label>
        Mật khẩu tạm (tối thiểu 12 ký tự)
        <input required type="password" minLength={12} maxLength={72} value={password} onChange={(event) => setPassword(event.target.value)} />
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Cấp tài khoản" />
    </form>
  );
}

function InviteMemberForm({
  currentUserRole,
  busy,
  onCancel,
  onSubmit,
}: {
  currentUserRole: MembershipRole;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { fullName: string; email: string; password: string; role: MembershipRole }) => void;
}) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<MembershipRole>('STAFF');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ fullName, email, password, role });
      }}
    >
      <label>
        Họ và tên
        <input required maxLength={120} autoFocus value={fullName} onChange={(event) => setFullName(event.target.value)} />
      </label>
      <label>
        Email đăng nhập
        <input required type="email" maxLength={254} value={email} onChange={(event) => setEmail(event.target.value)} />
      </label>
      <label>
        Vai trò
        <select value={role} onChange={(event) => setRole(event.target.value as MembershipRole)}>
          <option value="STAFF">Nhân viên (STAFF)</option>
          {currentUserRole === 'OWNER' && <option value="MANAGER">Quản lý (MANAGER)</option>}
        </select>
      </label>
      <label>
        Mật khẩu khởi tạo (tối thiểu 12 ký tự)
        <input required type="password" minLength={12} maxLength={72} value={password} onChange={(event) => setPassword(event.target.value)} />
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Thêm thành viên" />
    </form>
  );
}

function ChangePasswordForm({
  busy,
  onCancel,
  onSubmit,
}: {
  busy: boolean;
  onCancel: () => void;
  onSubmit: (currentPassword: string, newPassword: string) => void;
}) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit(currentPassword, newPassword);
      }}
    >
      <label>
        Mật khẩu hiện tại
        <input required type="password" autoFocus maxLength={72} value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} />
      </label>
      <label>
        Mật khẩu mới (tối thiểu 12 ký tự)
        <input required type="password" minLength={12} maxLength={72} value={newPassword} onChange={(event) => setNewPassword(event.target.value)} />
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Cập nhật mật khẩu" />
    </form>
  );
}

function ContractForm({
  rooms,
  tenants,
  busy,
  onCancel,
  onSubmit,
}: {
  rooms: Room[];
  tenants: Tenant[];
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { roomId: string; tenantId: string; startDate: string; endDate: string }) => void;
}) {
  const [roomId, setRoomId] = useState(rooms.find((room) => room.status === 'AVAILABLE')?.id ?? rooms[0]?.id ?? '');
  const [tenantId, setTenantId] = useState(tenants[0]?.id ?? '');
  const [startDate, setStartDate] = useState(new Date().toISOString().slice(0, 10));
  const [endDate, setEndDate] = useState('');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ roomId, tenantId, startDate, endDate });
      }}
    >
      <label>
        Phòng
        <select required value={roomId} onChange={(event) => setRoomId(event.target.value)}>
          {rooms.map((room) => (
            <option key={room.id} value={room.id}>
              {room.roomCode} · {money(room.monthlyRent)} / tháng · {room.status === 'AVAILABLE' ? 'Trống' : 'Không trống'}
            </option>
          ))}
        </select>
      </label>
      <label>
        Người thuê
        <select required value={tenantId} onChange={(event) => setTenantId(event.target.value)}>
          {tenants.map((tenant) => (
            <option key={tenant.id} value={tenant.id}>
              {tenant.fullName}
            </option>
          ))}
        </select>
      </label>
      <div className="form-row">
        <label>
          Ngày bắt đầu
          <input required type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
        </label>
        <label>
          Ngày kết thúc
          <input required type="date" min={startDate} value={endDate} onChange={(event) => setEndDate(event.target.value)} />
        </label>
      </div>
      <div className="form-note">
        <ShieldCheck size={15} /> Tiền thuê được chụp theo giá phòng tại thời điểm lập. Phòng chỉ chuyển sang “Đang thuê” sau khi kích hoạt.
      </div>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Tạo bản nháp" />
    </form>
  );
}

function TariffForm({
  busy,
  onCancel,
  onSubmit,
}: {
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { utilityType: UtilityType; pricePerUnit: number; effectiveFrom: string }) => void;
}) {
  const [utilityType, setUtilityType] = useState<UtilityType>('ELECTRICITY');
  const [price, setPrice] = useState('');
  const [date, setDate] = useState(`${periodStart(currentPeriod)}`);
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ utilityType, pricePerUnit: Number(price), effectiveFrom: date });
      }}
    >
      <label>
        Loại dịch vụ
        <select value={utilityType} onChange={(event) => setUtilityType(event.target.value as UtilityType)}>
          <option value="ELECTRICITY">Điện</option>
          <option value="WATER">Nước</option>
        </select>
      </label>
      <label>
        Đơn giá (₫ / đơn vị)
        <input required type="number" min="0" step="0.01" autoFocus value={price} onChange={(event) => setPrice(event.target.value)} placeholder="3500" />
      </label>
      <label>
        Ngày bắt đầu áp dụng
        <input required type="date" value={date} onChange={(event) => setDate(event.target.value)} />
      </label>
      <div className="form-note">
        <CircleHelp size={15} /> Nhập đơn giá theo thỏa thuận. RMS không áp đặt biểu giá chính thức.
      </div>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Lưu đơn giá" />
    </form>
  );
}

function periodStart(period: string) {
  return `${period}-01`;
}

function ReadingForm({
  rooms,
  period,
  tariffs,
  busy,
  onCancel,
  onSubmit,
  onAddTariff,
}: {
  rooms: Room[];
  period: string;
  tariffs: Tariff[];
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { roomId: string; utilityType: UtilityType; billingPeriod: string; currentReading: number }) => void;
  onAddTariff: () => void;
}) {
  const [roomId, setRoomId] = useState(rooms[0]?.id ?? '');
  const [utilityType, setUtilityType] = useState<UtilityType>('ELECTRICITY');
  const [currentReading, setCurrentReading] = useState('');
  const hasTariff = tariffs.some((tariff) => tariff.utilityType === utilityType && tariff.effectiveFrom <= `${period}-31`);
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ roomId, utilityType, billingPeriod: period, currentReading: Number(currentReading) });
      }}
    >
      <label>
        Phòng
        <select required value={roomId} onChange={(event) => setRoomId(event.target.value)}>
          {rooms.map((room) => (
            <option key={room.id} value={room.id}>
              {room.roomCode}
            </option>
          ))}
        </select>
      </label>
      <div className="form-row">
        <label>
          Dịch vụ
          <select value={utilityType} onChange={(event) => setUtilityType(event.target.value as UtilityType)}>
            <option value="ELECTRICITY">Điện</option>
            <option value="WATER">Nước</option>
          </select>
        </label>
        <label>
          Kỳ hóa đơn
          <input type="month" required value={period} readOnly />
        </label>
      </div>
      <label>
        Chỉ số mới
        <input
          required
          type="number"
          min="0"
          step="0.001"
          autoFocus
          value={currentReading}
          onChange={(event) => setCurrentReading(event.target.value)}
          placeholder="Ví dụ: 1280"
        />
      </label>
      {!hasTariff && (
        <div className="form-note warning-note">
          <CircleHelp size={15} /> Chưa có đơn giá cho dịch vụ/kỳ này.{' '}
          <button type="button" onClick={onAddTariff}>
            Cấu hình ngay
          </button>
        </div>
      )}
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Lưu chỉ số" />
    </form>
  );
}

function AdjustInvoiceForm({
  invoice,
  busy,
  onCancel,
  onSubmit,
}: {
  invoice: Invoice;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { serviceAmount: number; discountAmount: number; adjustmentNotes?: string }) => void;
}) {
  const [serviceAmount, setServiceAmount] = useState(`${invoice.serviceAmount ?? 0}`);
  const [discountAmount, setDiscountAmount] = useState(`${invoice.discountAmount ?? 0}`);
  const [adjustmentNotes, setAdjustmentNotes] = useState(invoice.adjustmentNotes ?? '');
  const previewTotal = Math.max(
    0,
    invoice.rentAmount +
      invoice.electricityAmount +
      invoice.waterAmount +
      Number(serviceAmount || 0) -
      Number(discountAmount || 0)
  );
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({
          serviceAmount: Number(serviceAmount || 0),
          discountAmount: Number(discountAmount || 0),
          adjustmentNotes: adjustmentNotes || undefined,
        });
      }}
    >
      <div className="payment-summary">
        <span>
          Tiền phòng + điện nước<strong>{money(invoice.rentAmount + invoice.electricityAmount + invoice.waterAmount)}</strong>
        </span>
        <span>
          Tổng sau điều chỉnh<strong>{money(previewTotal)}</strong>
        </span>
      </div>
      <div className="form-row">
        <label>
          Phí dịch vụ / phụ thu (₫)
          <input
            required
            type="number"
            min="0"
            step="1000"
            value={serviceAmount}
            onChange={(event) => setServiceAmount(event.target.value)}
          />
        </label>
        <label>
          Giảm trừ / khuyến mãi (₫)
          <input
            required
            type="number"
            min="0"
            step="1000"
            value={discountAmount}
            onChange={(event) => setDiscountAmount(event.target.value)}
          />
        </label>
      </div>
      <label>
        Ghi chú điều chỉnh
        <input
          maxLength={300}
          value={adjustmentNotes}
          onChange={(event) => setAdjustmentNotes(event.target.value)}
          placeholder="Ví dụ: Internet + Rác + Giảm trừ bảo trì"
        />
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Lưu điều chỉnh" />
    </form>
  );
}

function PaymentForm({
  invoice,
  busy,
  onCancel,
  onSubmit,
}: {
  invoice: Invoice;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { amount: number; method: 'CASH' | 'BANK_TRANSFER'; reference: string; idempotencyKey: string }) => void;
}) {
  const [amount, setAmount] = useState(`${invoice.balanceDue}`);
  const [method, setMethod] = useState<'CASH' | 'BANK_TRANSFER'>('BANK_TRANSFER');
  const [reference, setReference] = useState('');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ amount: Number(amount), method, reference, idempotencyKey: crypto.randomUUID() });
      }}
    >
      <div className="payment-summary">
        <span>
          Khoản còn phải thu<strong>{money(invoice.balanceDue)}</strong>
        </span>
        <span>
          Kỳ hóa đơn<strong>{invoice.billingPeriod}</strong>
        </span>
      </div>
      <label>
        Số tiền nhận (₫)
        <input
          required
          type="number"
          min="1"
          max={invoice.balanceDue}
          step="1"
          autoFocus
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
        />
      </label>
      <label>
        Hình thức
        <select value={method} onChange={(event) => setMethod(event.target.value as 'CASH' | 'BANK_TRANSFER')}>
          <option value="BANK_TRANSFER">Chuyển khoản (ghi nhận thủ công)</option>
          <option value="CASH">Tiền mặt</option>
        </select>
      </label>
      <label>
        Mã tham chiếu / ghi chú
        <input
          required
          maxLength={120}
          value={reference}
          onChange={(event) => setReference(event.target.value)}
          placeholder="Ví dụ: UNC-2026-001 hoặc Thu tiền mặt"
        />
      </label>
      <div className="form-note">
        <CircleHelp size={15} /> Bản ghi này không xác minh giao dịch ngân hàng.
      </div>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Ghi nhận thanh toán" />
    </form>
  );
}

function TerminateContractForm({
  contract,
  busy,
  onCancel,
  onSubmit,
}: {
  contract: Contract;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: {
    terminatedDate: string;
    depositDeducted: number;
    finalElectricityReading?: number;
    finalWaterReading?: number;
    checklistNotes?: string;
  }) => void;
}) {
  const [terminatedDate, setTerminatedDate] = useState(new Date().toISOString().slice(0, 10));
  const [depositDeducted, setDepositDeducted] = useState('0');
  const [finalElec, setFinalElec] = useState('');
  const [finalWater, setFinalWater] = useState('');
  const [notes, setNotes] = useState('');
  const deductedNum = Number(depositDeducted || 0);
  const refund = Math.max(0, contract.depositAmount - deductedNum);
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({
          terminatedDate,
          depositDeducted: deductedNum,
          finalElectricityReading: finalElec ? Number(finalElec) : undefined,
          finalWaterReading: finalWater ? Number(finalWater) : undefined,
          checklistNotes: notes || undefined,
        });
      }}
    >
      <div className="payment-summary">
        <span>
          Tiền cọc đang giữ<strong>{money(contract.depositAmount)}</strong>
        </span>
        <span>
          Hoàn trả dự kiến<strong>{money(refund)}</strong>
        </span>
      </div>
      <label>
        Ngày bàn giao / chấm dứt
        <input
          required
          type="date"
          min={contract.startDate}
          value={terminatedDate}
          onChange={(event) => setTerminatedDate(event.target.value)}
        />
      </label>
      <label>
        Khấu trừ tiền cọc (₫)
        <input
          required
          type="number"
          min="0"
          max={contract.depositAmount}
          step="1000"
          value={depositDeducted}
          onChange={(event) => setDepositDeducted(event.target.value)}
        />
      </label>
      <div className="form-row">
        <label>
          Chốt chỉ số điện cuối
          <input type="number" min="0" step="0.001" value={finalElec} onChange={(event) => setFinalElec(event.target.value)} />
        </label>
        <label>
          Chốt chỉ số nước cuối
          <input type="number" min="0" step="0.001" value={finalWater} onChange={(event) => setFinalWater(event.target.value)} />
        </label>
      </div>
      <label>
        Ghi chú biên bản bàn giao
        <input
          maxLength={1000}
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          placeholder="Đã kiểm tra thiết bị, nhận đủ chìa khóa"
        />
      </label>
      <div className="form-note">
        <ShieldCheck size={15} /> Mọi hóa đơn trong hợp đồng phải được thanh toán trước khi bàn giao trả phòng.
      </div>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Xác nhận thanh lý" />
    </form>
  );
}

function HandoverSummary({ handover, onClose }: { handover: HandoverRecord; onClose: () => void }) {
  return (
    <div className="modal-form">
      <div className="payment-summary">
        <span>
          Cọc giữ<strong>{money(handover.depositHeld)}</strong>
        </span>
        <span>
          Khấu trừ<strong>{money(handover.depositDeducted)}</strong>
        </span>
        <span>
          Hoàn trả<strong>{money(handover.depositRefunded)}</strong>
        </span>
      </div>
      <div style={{ display: 'grid', gap: 6, fontSize: 12 }}>
        <div>
          <strong>Ngày bàn giao:</strong> {handover.terminatedDate}
        </div>
        <div>
          <strong>Dư nợ hóa đơn tại thời điểm bàn giao:</strong> {money(handover.outstandingBalance)}
        </div>
        {handover.finalElectricityReading != null && (
          <div>
            <strong>Chỉ số điện cuối:</strong> {handover.finalElectricityReading}
          </div>
        )}
        {handover.finalWaterReading != null && (
          <div>
            <strong>Chỉ số nước cuối:</strong> {handover.finalWaterReading}
          </div>
        )}
        <div>
          <strong>Ghi chú:</strong> {handover.checklistNotes || '—'}
        </div>
      </div>
      <div className="form-actions">
        <button className="button button-primary" type="button" onClick={onClose}>
          Đóng
        </button>
      </div>
    </div>
  );
}

function TicketForm({
  rooms,
  busy,
  onCancel,
  onSubmit,
}: {
  rooms: Room[];
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { roomId: string; title: string; description: string; priority: TicketPriority; estimatedCost: number }) => void;
}) {
  const [roomId, setRoomId] = useState(rooms[0]?.id ?? '');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [estimatedCost, setEstimatedCost] = useState('0');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ roomId, title, description, priority, estimatedCost: Number(estimatedCost || 0) });
      }}
    >
      <label>
        Phòng
        <select required value={roomId} onChange={(event) => setRoomId(event.target.value)}>
          {rooms.map((room) => (
            <option key={room.id} value={room.id}>
              {room.roomCode}
            </option>
          ))}
        </select>
      </label>
      <label>
        Tiêu đề sự cố
        <input
          required
          maxLength={160}
          autoFocus
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Ví dụ: Hỏng máy lạnh / Rò nước"
        />
      </label>
      <label>
        Mô tả chi tiết
        <input
          required
          maxLength={1000}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="Mô tả tình trạng thiết bị"
        />
      </label>
      <div className="form-row">
        <label>
          Mức độ ưu tiên
          <select value={priority} onChange={(event) => setPriority(event.target.value as TicketPriority)}>
            <option value="LOW">Thấp</option>
            <option value="MEDIUM">Trung bình</option>
            <option value="HIGH">Cao</option>
            <option value="URGENT">Khẩn cấp</option>
          </select>
        </label>
        <label>
          Chi phí dự kiến (₫)
          <input required type="number" min="0" step="1000" value={estimatedCost} onChange={(event) => setEstimatedCost(event.target.value)} />
        </label>
      </div>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Tạo yêu cầu" />
    </form>
  );
}

function TenantTicketForm({
  contracts,
  busy,
  onCancel,
  onSubmit,
}: {
  contracts: { roomId: string; roomCode: string }[];
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { roomId: string; title: string; description: string; priority: TicketPriority }) => void;
}) {
  const [roomId, setRoomId] = useState(contracts[0]?.roomId ?? '');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ roomId, title, description, priority });
      }}
    >
      <label>
        Phòng của bạn
        <select required value={roomId} onChange={(event) => setRoomId(event.target.value)}>
          {contracts.map((c) => (
            <option key={c.roomId} value={c.roomId}>
              {c.roomCode}
            </option>
          ))}
        </select>
      </label>
      <label>
        Tiêu đề sự cố
        <input required maxLength={160} autoFocus value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Ví dụ: Hỏng đèn phòng tắm" />
      </label>
      <label>
        Mô tả chi tiết
        <input required maxLength={1000} value={description} onChange={(event) => setDescription(event.target.value)} />
      </label>
      <label>
        Mức độ ưu tiên
        <select value={priority} onChange={(event) => setPriority(event.target.value as TicketPriority)}>
          <option value="LOW">Thấp</option>
          <option value="MEDIUM">Trung bình</option>
          <option value="HIGH">Cao</option>
          <option value="URGENT">Khẩn cấp</option>
        </select>
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Gửi yêu cầu" />
    </form>
  );
}

function AssignTicketForm({
  ticket,
  busy,
  onCancel,
  onSubmit,
}: {
  ticket: MaintenanceTicket;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (assigneeName: string) => void;
}) {
  const [assigneeName, setAssigneeName] = useState(ticket.assigneeName ?? '');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit(assigneeName);
      }}
    >
      <label>
        Nhân viên / đơn vị phụ trách
        <input
          required
          maxLength={120}
          autoFocus
          value={assigneeName}
          onChange={(event) => setAssigneeName(event.target.value)}
          placeholder="Ví dụ: Anh Tùng Kỹ Thuật"
        />
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Xác nhận phân công" />
    </form>
  );
}

function ResolveTicketForm({
  ticket,
  busy,
  onCancel,
  onSubmit,
}: {
  ticket: MaintenanceTicket;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (body: { actualCost: number; resolutionNotes?: string }) => void;
}) {
  const [actualCost, setActualCost] = useState(`${ticket.estimatedCost || 0}`);
  const [resolutionNotes, setResolutionNotes] = useState('');
  return (
    <form
      className="modal-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ actualCost: Number(actualCost || 0), resolutionNotes: resolutionNotes || undefined });
      }}
    >
      <label>
        Chi phí thực tế (₫)
        <input required type="number" min="0" step="1000" autoFocus value={actualCost} onChange={(event) => setActualCost(event.target.value)} />
      </label>
      <label>
        Ghi chú kết quả sửa chữa
        <input
          maxLength={1000}
          value={resolutionNotes}
          onChange={(event) => setResolutionNotes(event.target.value)}
          placeholder="Đã thay thế linh kiện, kiểm tra vận hành bình thường"
        />
      </label>
      <FormActions busy={busy} onCancel={onCancel} submitLabel="Hoàn tất sửa chữa" />
    </form>
  );
}

export default App;
