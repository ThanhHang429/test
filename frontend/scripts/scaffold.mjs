#!/usr/bin/env node
/**
 * scaffold.mjs — Sinh khung (skeleton) cho project Android ETicket.
 *
 * Quy tắc bắt buộc (xem docs/00-overview.md):
 *  - Mỗi màn hình = 1 Fragment + 1 ViewModel + 1 layout placeholder, đặt trong ui/<role>/<area>/.
 *  - Tên layout:  fragment_<role>_<area>_<snake_case(Class)>.xml (bỏ <area> nếu trùng tiền tố tên màn hình).
 *  - Nav id:       snake_case(Class) + "Fragment"  (trùng tên giữa 2 vai trò → thêm tiền tố role)
 *  - Mọi class stub đều `public`, thân rỗng, KHÔNG có logic nghiệp vụ.
 *  - KHÔNG ghi đè file đã tồn tại — chỉ tạo file còn thiếu. Dùng --force để ghi đè toàn bộ.
 *
 * Dùng:  node scripts/scaffold.mjs           → chỉ tạo file còn thiếu (an toàn)
 *        node scripts/scaffold.mjs --force   → ghi đè toàn bộ (xoá code thật, cẩn thận)
 *        node scripts/scaffold.mjs --tree    → in cây thư mục 3 cấp
 */

import { mkdirSync, writeFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const APP = join(ROOT, 'app', 'src', 'main');
const JAVA = join(APP, 'java', 'com', 'eventapp', 'app');
const TEST = join(ROOT, 'app', 'src', 'test', 'java', 'com', 'eventapp', 'app');
const RES = join(APP, 'res');
const PKG = 'com.eventapp.app';

/** "com.eventapp.app.ui.user.home" -> <JAVA>/ui/user/home (đường dẫn tuyệt đối) */
function javaPath(pkg, ...files) {
  return join(JAVA, ...pkg.slice(PKG.length + 1).split('.'), ...files);
}

/* ------------------------------------------------------------------ */
/* Helpers                                                             */
/* ------------------------------------------------------------------ */

let written = 0;
let skipped = 0;
const created = [];

/** Mặc định KHÔNG ghi đè file đã có (tránh xoá code thật). Dùng --force để ghi đè. */
const FORCE = process.argv.includes('--force');

function write(file, content) {
  if (!FORCE && existsSync(file)) {
    skipped++;
    return;
  }
  mkdirSync(dirname(file), { recursive: true });
  const body = content.endsWith('\n') ? content : content + '\n';
  writeFileSync(file, body, 'utf8');
  written++;
  created.push(file.startsWith(ROOT) ? file.slice(ROOT.length + 1) : file);
}

function snake(name) {
  return name.replace(/([a-z0-9])([A-Z])/g, '$1_$2').replace(/([A-Z]+)([A-Z][a-z])/g, '$1_$2').toLowerCase();
}

function title(desc) {
  return desc.charAt(0).toUpperCase() + desc.slice(1);
}

/* ------------------------------------------------------------------ */
/* 1. MÀN HÌNH (86)                                                     */
/*    role: auth | user | organizer | admin                              */
/*    area: nhóm con dùng cho tên layout (<area>_<snake>)                */
/* ------------------------------------------------------------------ */

const SCREENS = [
  // ---- AUTH (5) ----
  { role: 'auth', area: 'auth', name: 'Login', fr: 'FR-U02', desc: 'Đăng nhập bằng email và mật khẩu, hiển thị lỗi xác thực' },
  { role: 'auth', area: 'auth', name: 'Register', fr: 'FR-U01', desc: 'Đăng ký tài khoản mới bằng email và mật khẩu' },
  { role: 'auth', area: 'auth', name: 'VerifyEmail', fr: 'FR-U01', desc: 'Xác thực email qua mã OTP do backend gửi' },
  { role: 'auth', area: 'auth', name: 'ForgotPassword', fr: 'FR-U02', desc: 'Gửi yêu cầu đặt lại mật khẩu qua email' },
  { role: 'auth', area: 'auth', name: 'ResetPassword', fr: 'FR-U02', desc: 'Đặt mật khẩu mới bằng token trong email' },

  // ---- USER (34) ----
  { role: 'user', area: 'home', name: 'Home', fr: 'FR-U04', desc: 'Trang chủ, gợi ý sự kiện nổi bật và sự kiện sắp diễn ra' },
  { role: 'user', area: 'home', name: 'EventList', fr: 'FR-U04', desc: 'Danh sách sự kiện, lọc theo danh mục, ngày và khoảng giá' },
  { role: 'user', area: 'home', name: 'EventFilter', fr: 'FR-U04', desc: 'Bộ lọc sự kiện theo danh mục, thời gian, giá và khoảng cách' },
  { role: 'user', area: 'home', name: 'EventDetail', fr: 'FR-U05', desc: 'Chi tiết sự kiện, lịch trình, loại vé và nút đăng ký hoặc mua vé' },
  { role: 'user', area: 'home', name: 'Recommend', fr: 'FR-U07', desc: 'Gợi ý sự kiện theo sở thích người dùng' },
  { role: 'user', area: 'favorite', name: 'Favorites', fr: 'FR-U06', desc: 'Danh sách sự kiện người dùng đã yêu thích' },
  { role: 'user', area: 'ticket', name: 'MyTickets', fr: 'FR-U14,FR-U18', desc: 'Vé của tôi: tab Sắp diễn ra (kiêm lịch sự kiện) và Đã qua' },
  { role: 'user', area: 'ticket', name: 'TicketDetail', fr: 'FR-U14', desc: 'Chi tiết một vé, mã QR và lịch sử check-in' },
  { role: 'user', area: 'ticket', name: 'CancelTicket', fr: 'FR-U15', desc: 'Hủy vé và hiển thị chính sách hoàn tiền' },
  { role: 'user', area: 'order', name: 'FreeRegister', fr: 'FR-U09', desc: 'Đăng ký miễn phí vào sự kiện miễn phí' },
  { role: 'user', area: 'order', name: 'Checkout', fr: 'FR-U10', desc: 'Thanh toán, chọn loại vé và số lượng' },
  { role: 'user', area: 'order', name: 'PaymentQr', fr: 'FR-U11', desc: 'Hiển thị QR thanh toán và trạng thái chờ thanh toán' },
  { role: 'user', area: 'order', name: 'OrderStatus', fr: 'FR-U12', desc: 'Theo dõi trạng thái đơn hàng và thanh toán' },
  { role: 'user', area: 'order', name: 'OrderHistory', fr: 'FR-U16', desc: 'Lịch sử đơn hàng và vé của người dùng' },
  { role: 'user', area: 'order', name: 'OrderDetail', fr: 'FR-U16', desc: 'Chi tiết đơn hàng, trạng thái thanh toán và hoàn tiền' },
  { role: 'user', area: 'checkin', name: 'ScanQr', fr: 'FR-U17', desc: 'Quét mã QR vé bằng CameraX và ML Kit' },
  { role: 'user', area: 'checkin', name: 'CheckinResult', fr: 'FR-U17', desc: 'Hiển thị kết quả check-in của vé' },
  { role: 'user', area: 'profile', name: 'Profile', fr: 'FR-U03', desc: 'Thông tin cá nhân, vai trò và tài khoản' },
  { role: 'user', area: 'profile', name: 'EditProfile', fr: 'FR-U03', desc: 'Chỉnh sửa hồ sơ người dùng' },
  { role: 'user', area: 'profile', name: 'Preferences', fr: 'FR-U03', desc: 'Tùy chọn thông báo, ngôn ngữ và chủ đề' },
  { role: 'user', area: 'profile', name: 'PrivacySettings', fr: 'FR-U22', desc: 'Cài đặt quyền riêng tư và chia sẻ dữ liệu' },
  { role: 'user', area: 'profile', name: 'BecomeOrganizer', fr: 'FR-O01', desc: 'Đăng ký trở thành nhà tổ chức sự kiện' },
  { role: 'user', area: 'review', name: 'WriteReview', fr: 'FR-U20', desc: 'Viết đánh giá và chấm điểm sự kiện đã tham dự' },
  { role: 'user', area: 'review', name: 'EventReviews', fr: 'FR-U20', desc: 'Danh sách đánh giá của một sự kiện' },
  { role: 'user', area: 'support', name: 'SupportList', fr: 'FR-U21', desc: 'Danh sách yêu cầu hỗ trợ của người dùng' },
  { role: 'user', area: 'support', name: 'CreateSupport', fr: 'FR-U21', desc: 'Tạo yêu cầu hỗ trợ mới' },
  { role: 'user', area: 'support', name: 'SupportDetail', fr: 'FR-U21', desc: 'Chi tiết và trao đổi tin nhắn trong yêu cầu hỗ trợ' },
  { role: 'user', area: 'notification', name: 'NotificationList', fr: 'FR-U19', desc: 'Danh sách thông báo trong ứng dụng' },

  // ---- ORGANIZER (29) ----
  { role: 'organizer', area: 'onboarding', name: 'OrganizerRegister', fr: 'FR-O01', desc: 'Đơn đăng ký trở thành nhà tổ chức' },
  { role: 'organizer', area: 'onboarding', name: 'OrganizerProfile', fr: 'FR-O02', desc: 'Hồ sơ nhà tổ chức và thông tin pháp lý' },
  { role: 'organizer', area: 'onboarding', name: 'ApplicationStatus', fr: 'FR-O01', desc: 'Trạng thái đơn đăng ký nhà tổ chức' },
  { role: 'organizer', area: 'dashboard', name: 'OrganizerDashboard', fr: 'FR-O03', desc: 'Tổng quan doanh thu, lượt đăng ký và sự kiện sắp tới' },
  { role: 'organizer', area: 'event', name: 'MyEvents', fr: 'FR-O04', desc: 'Danh sách sự kiện do nhà tổ chức quản lý' },
  { role: 'organizer', area: 'event', name: 'EventForm', fr: 'FR-O04,FR-O05,FR-O08,FR-O09', desc: 'Tạo và cập nhật sự kiện, lịch trình và danh mục' },
  { role: 'organizer', area: 'event', name: 'AiDescription', fr: 'FR-O06', desc: 'Gợi ý mô tả sự kiện bằng AI, cần người dùng xác nhận' },
  { role: 'organizer', area: 'event', name: 'MediaManage', fr: 'FR-O07', desc: 'Quản lý ảnh và video của sự kiện' },
  { role: 'organizer', area: 'event', name: 'SubmitReview', fr: 'FR-O13', desc: 'Gửi sự kiện để admin duyệt' },
  { role: 'organizer', area: 'event', name: 'ReviewResult', fr: 'FR-O14', desc: 'Hiển thị kết quả duyệt của sự kiện' },
  { role: 'organizer', area: 'event', name: 'EventHistory', fr: 'FR-O29', desc: 'Lịch sử các sự kiện đã kết thúc' },
  { role: 'organizer', area: 'event', name: 'CancelEvent', fr: 'FR-O24', desc: 'Hủy sự kiện và xử lý hoàn tiền' },
  { role: 'organizer', area: 'event', name: 'SaleControl', fr: 'FR-O15', desc: 'Bật tắt bán vé và đặt giá vé' },
  { role: 'organizer', area: 'ticket', name: 'TicketTypeList', fr: 'FR-O10', desc: 'Danh sách loại vé của sự kiện' },
  { role: 'organizer', area: 'ticket', name: 'TicketTypeForm', fr: 'FR-O10,FR-O11,FR-O12', desc: 'Tạo loại vé, giá, số lượng và cửa sổ bán' },
  { role: 'organizer', area: 'attendee', name: 'RegistrationList', fr: 'FR-O16', desc: 'Danh sách người đăng ký miễn phí' },
  { role: 'organizer', area: 'attendee', name: 'OrderList', fr: 'FR-O17', desc: 'Danh sách đơn hàng của sự kiện' },
  { role: 'organizer', area: 'attendee', name: 'OrderDetail', fr: 'FR-O17', desc: 'Chi tiết đơn hàng từ góc nhìn nhà tổ chức' },
  { role: 'organizer', area: 'checkin', name: 'ShowCheckinQr', fr: 'FR-O19', desc: 'Hiển thị QR check-in của sự kiện cho ban tổ chức' },
  { role: 'organizer', area: 'checkin', name: 'CheckinMonitor', fr: 'FR-O21', desc: 'Theo dõi số liệu check-in theo thời gian thực' },
  { role: 'organizer', area: 'checkin', name: 'TicketLookup', fr: 'FR-O22', desc: 'Tra cứu vé bằng mã hoặc thông tin người mua' },
  { role: 'organizer', area: 'checkin', name: 'ManualCheckin', fr: 'FR-O22', desc: 'Check-in thủ công khi không quét được QR' },
  { role: 'organizer', area: 'checkin', name: 'StaffManage', fr: 'FR-O20', desc: 'Quản lý nhân sự được phép check-in' },
  { role: 'organizer', area: 'notification', name: 'SendAttendeeNotice', fr: 'FR-O23', desc: 'Gửi thông báo tới người đăng ký sự kiện' },
  { role: 'organizer', area: 'refund', name: 'OrganizerRefunds', fr: 'FR-O25', desc: 'Danh sách yêu cầu hoàn tiền của sự kiện' },
  { role: 'organizer', area: 'report', name: 'EventReport', fr: 'FR-O26', desc: 'Báo cáo doanh thu và lượt đăng ký theo sự kiện' },
  { role: 'organizer', area: 'report', name: 'AiInsight', fr: 'FR-O27', desc: 'Gợi ý kinh doanh do AI phân tích' },
  { role: 'organizer', area: 'report', name: 'ExportData', fr: 'FR-O28', desc: 'Xuất báo cáo sự kiện ra tệp' },
  { role: 'organizer', area: 'promotion', name: 'PromoManage', fr: 'FR-O18', desc: 'Tạo và quản lý mã khuyến mãi' },

  // ---- ADMIN (23) ----
  { role: 'admin', area: 'dashboard', name: 'AdminDashboard', fr: 'FR-A01', desc: 'Tổng quan toàn hệ thống, sự kiện, người dùng và doanh thu' },
  { role: 'admin', area: 'user', name: 'UserManage', fr: 'FR-A02', desc: 'Danh sách người dùng, tìm kiếm và khóa tài khoản' },
  { role: 'admin', area: 'user', name: 'UserDetail', fr: 'FR-A02', desc: 'Chi tiết người dùng và lịch sử hoạt động' },
  { role: 'admin', area: 'organizer', name: 'OrganizerApprovalList', fr: 'FR-A03', desc: 'Danh sách đơn đăng ký nhà tổ chức chờ duyệt' },
  { role: 'admin', area: 'organizer', name: 'OrganizerApprovalDetail', fr: 'FR-A03', desc: 'Hồ sơ chi tiết để duyệt nhà tổ chức' },
  { role: 'admin', area: 'organizer', name: 'OrganizerSuspend', fr: 'FR-A04', desc: 'Tạm khóa hoặc khôi phục tài khoản nhà tổ chức' },
  { role: 'admin', area: 'event', name: 'ReviewQueue', fr: 'FR-A05', desc: 'Hàng đợi duyệt sự kiện của nhà tổ chức' },
  { role: 'admin', area: 'event', name: 'ReviewDetail', fr: 'FR-A06', desc: 'Chi tiết sự kiện và quyết định duyệt hoặc từ chối' },
  { role: 'admin', area: 'event', name: 'ContentModeration', fr: 'FR-A07', desc: 'Kiểm duyệt nội dung, đánh giá và bình luận' },
  { role: 'admin', area: 'event', name: 'EventMonitor', fr: 'FR-A08', desc: 'Giám sát sự kiện đang diễn ra và sự cố bất thường' },
  { role: 'admin', area: 'category', name: 'CategoryManage', fr: 'FR-A09', desc: 'Quản lý danh mục sự kiện' },
  { role: 'admin', area: 'finance', name: 'TransactionLookup', fr: 'FR-A10', desc: 'Tra cứu giao dịch theo mã đơn, ví hoặc thời gian' },
  { role: 'admin', area: 'finance', name: 'TransactionDetail', fr: 'FR-A10', desc: 'Chi tiết giao dịch và lịch sử webhook' },
  { role: 'admin', area: 'finance', name: 'WebhookMonitor', fr: 'FR-A11', desc: 'Giám sát và xử lý webhook thanh toán lỗi' },
  { role: 'admin', area: 'finance', name: 'RefundManage', fr: 'FR-A12', desc: 'Duyệt và từ chối yêu cầu hoàn tiền' },
  { role: 'admin', area: 'finance', name: 'FeeConfig', fr: 'FR-A16', desc: 'Cấu hình biểu phí nền tảng' },
  { role: 'admin', area: 'support', name: 'SupportManage', fr: 'FR-A13', desc: 'Quản lý yêu cầu hỗ trợ và phân công' },
  { role: 'admin', area: 'support', name: 'SupportHandle', fr: 'FR-A13', desc: 'Xử lý và trao đổi với yêu cầu hỗ trợ' },
  { role: 'admin', area: 'notification', name: 'BroadcastNotice', fr: 'FR-A15', desc: 'Gửi thông báo đến toàn bộ người dùng' },
  { role: 'admin', area: 'report', name: 'PlatformReport', fr: 'FR-A17', desc: 'Báo cáo doanh thu, người dùng và sự kiện toàn nền tảng' },
  { role: 'admin', area: 'report', name: 'AuditLog', fr: 'FR-A18', desc: 'Nhật ký thao tác quản trị' },
  { role: 'admin', area: 'system', name: 'RoleManage', fr: 'FR-A19', desc: 'Phân vai trò cho tài khoản' },
  { role: 'admin', area: 'system', name: 'IntegrationHealth', fr: 'FR-A20', desc: 'Kiểm tra tình trạng tích hợp thanh toán và dịch vụ ngoài' },
];

/* Nav id: snake(Class) + "Fragment". Trùng tên giữa 2 vai trò → thêm tiền tố role. */
const DUP_CLASS = new Map();
for (const s of SCREENS) DUP_CLASS.set(s.name, (DUP_CLASS.get(s.name) || 0) + 1);

/* Hai màn hình cùng tên ở 2 vai trò KHÔNG được trỏ vào cùng class:
   ViewModel của chúng sẽ trùng tên. Đổi tên class cho màn hình admin.
   - user OrderDetail       → giữ nguyên
   - organizer OrderDetail  → giữ nguyên
   - admin OrderDetail      → AdminOrderDetail
   (nav id vẫn là "order_detailFragment", giữ đúng quy ước snake_case(ClassName).) */
const RENAME = {
  'user:OrderDetail': 'UserOrderDetail',
  'organizer:OrderDetail': 'OrganizerOrderDetail',
  'admin:OrderDetail': 'AdminOrderDetail',
};

function screenName(s) {
  return RENAME[`${s.role}:${s.name}`] ?? s.name;
}

function navId(s) {
  const base = snake(s.name) + 'Fragment';
  return DUP_CLASS.get(s.name) > 1 ? `${s.role}_${base}` : base;
}

function javaPkg(s) {
  return s.role === 'auth' ? `${PKG}.ui.auth` : `${PKG}.ui.${s.role}.${s.area}`;
}

function javaClassName(name) {
  return name;
}

/* fragment_<role>_<area>_<snake>.xml — bỏ <area> khi tên màn hình đã bắt đầu bằng area. */
function layoutName(s) {
  let screen = snake(screenName(s));
  if (screen.startsWith(s.role + '_')) screen = screen.slice(s.role.length + 1);
  const parts = [s.role];
  if (s.area !== s.role && !screen.startsWith(s.area)) parts.push(s.area);
  parts.push(screen);
  return `fragment_${parts.join('_')}`;
}

/* ------------------------------------------------------------------ */
/* 2. CLASS DÙNG CHUNG                                                  */
/* ------------------------------------------------------------------ */

const ACTIVITIES = [
  { name: 'SplashActivity', pkg: `${PKG}.ui.host`, desc: 'Màn hình khởi động, kiểm tra phiên đăng nhập và chuyển hướng', fr: 'FR-U01,FR-U02' },
  { name: 'AuthActivity', pkg: `${PKG}.ui.auth`, desc: 'Host chứa luồng đăng nhập, đăng ký và quên mật khẩu', fr: 'FR-U01,FR-U02' },
  { name: 'UserHostActivity', pkg: `${PKG}.ui.host`, desc: 'Host chứa khu vực người dùng và bottom navigation', fr: 'FR-U03' },
  { name: 'OrganizerHostActivity', pkg: `${PKG}.ui.host`, desc: 'Host chứa khu vực nhà tổ chức và bottom navigation', fr: 'FR-O02' },
  { name: 'AdminHostActivity', pkg: `${PKG}.ui.host`, desc: 'Host chứa khu vực quản trị và bottom navigation', fr: 'FR-A01' },
];

/* Adapter dùng bởi ≥2 feature → ui/common/adapter; chỉ 1 feature dùng → ui/<role>/<area>/adapter. */
const ADAPTERS = [
  ['EventAdapter', 'Danh sách sự kiện', 'FR-U04'],
  ['TicketAdapter', 'Danh sách vé', 'FR-U14'],
  ['OrderAdapter', 'Danh sách đơn hàng', 'FR-U16'],
  ['NotificationAdapter', 'Danh sách thông báo', 'FR-U19'],
  ['ReviewAdapter', 'Danh sách đánh giá', 'FR-U20'],
  ['CategoryAdapter', 'Danh sách danh mục', 'FR-A09'],
  ['TicketTypeAdapter', 'Danh sách loại vé', 'FR-O10'],
  ['TransactionAdapter', 'Danh sách giao dịch', 'FR-A10'],
  ['SupportTicketAdapter', 'Danh sách yêu cầu hỗ trợ', 'FR-U21'],
  ['AuditLogAdapter', 'Danh sách nhật ký quản trị', 'FR-A18'],
  ['CheckinStatsAdapter', 'Thống kê check-in', 'FR-O21'],
  ['AttendeeAdapter', 'Danh sách người tham dự', 'FR-O16'],
  ['RegistrationAdapter', 'Danh sách đăng ký miễn phí', 'FR-O16'],
  ['OrganizerApplicationAdapter', 'Danh sách đơn đăng ký nhà tổ chức', 'FR-A03'],
  ['PromotionAdapter', 'Danh sách khuyến mãi', 'FR-O18'],
  ['ReportAdapter', 'Danh sách báo cáo', 'FR-O26'],
  ['WalletTransactionAdapter', 'Lịch sử giao dịch ví', 'FR-U16'],
  ['PaymentHistoryAdapter', 'Lịch sử thanh toán', 'FR-U12'],
  ['RefundAdapter', 'Danh sách yêu cầu hoàn tiền', 'FR-A12'],
  ['StaffAdapter', 'Danh sách nhân sự check-in', 'FR-O20'],
  ['RecommendationAdapter', 'Danh sách gợi ý sự kiện', 'FR-U07'],
  // Feature-local
  ['FavoriteAdapter', 'Danh sách sự kiện yêu thích', 'FR-U06', `${PKG}.ui.user.favorite.adapter`],
  ['EventGalleryAdapter', 'Thư viện ảnh của sự kiện', 'FR-U05', `${PKG}.ui.user.home.adapter`],
  ['EventTicketAdapter', 'Danh sách loại vé trong chi tiết sự kiện', 'FR-U05', `${PKG}.ui.user.home.adapter`],
  ['ModerationFlagAdapter', 'Danh sách cờ kiểm duyệt nội dung', 'FR-A07', `${PKG}.ui.admin.event.adapter`],
  ['DashboardTaskAdapter', 'Danh sách việc cần làm trên dashboard', 'FR-O03', `${PKG}.ui.organizer.dashboard.adapter`],
  ['TopEventAdapter', 'Bảng xếp hạng sự kiện', 'FR-O03', `${PKG}.ui.organizer.dashboard.adapter`],
  ['AiInsightAdapter', 'Danh sách gợi ý AI', 'FR-O27', `${PKG}.ui.organizer.report.adapter`],
];

const adapterPkg = (row) => row[3] ?? `${PKG}.ui.common.adapter`;


const VIEWS = [
  ['StatusChip', 'Chip hiển thị trạng thái sự kiện hoặc đơn hàng', 'FR-U05'],
  ['EmptyStateView', 'View hiển thị trạng thái rỗng kèm nút hành động', 'FR-U04'],
  ['ErrorStateView', 'View hiển thị lỗi kèm nút thử lại', 'FR-U04'],
  ['QrImageView', 'View hiển thị mã QR vé hoặc mã sự kiện', 'FR-U14'],
  ['PriceTextView', 'View hiển thị giá tiền theo chuẩn VND', 'FR-U10'],
  ['RoleGuardLayout', 'Layout chỉ cho phép hiển thị với đúng vai trò', 'FR-U03'],
];


/* ------------------------------------------------------------------ */
/* 3. TẦNG DATA                                                        */
/* ------------------------------------------------------------------ */

/** 23 module backend → Api + Repository + DTO (giữ nguyên tên §11.1). */
const MODULES = [
  { m: 'auth', desc: 'Đăng ký, đăng nhập, làm mới token, quên mật khẩu', dto: ['RegisterRequest', 'VerifyEmailRequest', 'LoginRequest', 'LoginResponse', 'RefreshTokenRequest', 'ForgotPasswordRequest', 'ResetPasswordRequest'] },
  { m: 'user', desc: 'Hồ sơ người dùng, tùy chọn, riêng tư, quản lý người dùng', dto: ['UserProfileResponse', 'UpdateProfileRequest', 'UpdatePreferenceRequest', 'PrivacySettingDto', 'RegisterDeviceTokenRequest', 'LockUserRequest', 'AdminUserResponse'] },
  { m: 'organizer', desc: 'Đăng ký nhà tổ chức, hồ sơ, duyệt và đình chỉ', dto: ['OrganizerApplicationRequest', 'OrganizerProfileResponse', 'UpdateOrganizerProfileRequest', 'OrganizerDashboardResponse', 'OrganizerDecisionRequest', 'SuspendOrganizerRequest'] },
  { m: 'event', desc: 'Sự kiện, lịch trình, danh mục, duyệt và phê duyệt', dto: ['EventSummaryResponse', 'EventDetailResponse', 'EventScheduleResponse', 'EventSearchRequest', 'CreateEventRequest', 'UpdateEventRequest', 'UpdateScheduleRequest', 'UpdateCategoriesRequest', 'ReviewDecisionRequest', 'CancelEventRequest', 'EventVersionResponse'] },
  { m: 'category', desc: 'Danh mục sự kiện', dto: ['CategoryResponse', 'CategoryRequest'] },
  { m: 'media', desc: 'Ảnh và video của sự kiện', dto: ['MediaResponse', 'ReorderMediaRequest'] },
  { m: 'tickettype', desc: 'Loại vé, giá, số lượng và cửa sổ bán', dto: ['TicketTypeRequest', 'TicketTypeResponse', 'UpdateQuantityRequest', 'SaleWindowRequest'] },
  { m: 'registration', desc: 'Đăng ký miễn phí', dto: ['FreeRegisterRequest', 'RegistrationResponse', 'ManualAdjustRequest'] },
  { m: 'order', desc: 'Đơn hàng và thanh toán', dto: ['CreateOrderRequest', 'OrderResponse', 'OrderItemResponse', 'OrderSummaryResponse'] },
  { m: 'payment', desc: 'Thanh toán, QR, webhook và đối soát', dto: ['PaymentQrResponse', 'PaymentStatusResponse', 'WebhookPayload', 'ReconcileRequest', 'AdminPaymentResponse'] },
  { m: 'refund', desc: 'Hoàn tiền', dto: ['CreateRefundRequest', 'RefundResponse', 'RefundDecisionRequest'] },
  { m: 'ticket', desc: 'Vé của người dùng', dto: ['TicketResponse', 'TicketDetailResponse'] },
  { m: 'checkin', desc: 'Check-in bằng QR hoặc thủ công, thống kê', dto: ['ScanCheckinRequest', 'CheckinResponse', 'EventQrResponse', 'ManualCheckinRequest', 'CheckinStatsResponse', 'TicketLookupResponse', 'StaffRequest'] },
  { m: 'favorite', desc: 'Sự kiện yêu thích', dto: ['FavoriteResponse'] },
  { m: 'review', desc: 'Đánh giá và kiểm duyệt đánh giá', dto: ['ReviewRequest', 'ReviewResponse', 'ModerationRequest'] },
  { m: 'notification', desc: 'Thông báo trong ứng dụng và gửi thông báo', dto: ['NotificationResponse', 'SendNoticeRequest', 'BroadcastRequest', 'CalendarItemResponse'] },
  { m: 'support', desc: 'Yêu cầu hỗ trợ', dto: ['CreateSupportRequest', 'SupportTicketResponse', 'SupportMessageRequest', 'AssignSupportRequest'] },
  { m: 'report', desc: 'Báo cáo và xuất dữ liệu', dto: ['EventSummaryReport', 'SalesReport', 'AttendanceReport', 'PlatformOverviewReport', 'FinanceReport', 'ExportRequest'] },
  { m: 'audit', desc: 'Nhật ký quản trị', dto: ['AuditLogResponse', 'AuditSearchRequest'] },
  { m: 'ai', desc: 'Gợi ý, mô tả sự kiện và phân tích kinh doanh', dto: ['RecommendationRequest', 'RecommendationResponse', 'DescribeEventRequest', 'DescribeEventResponse', 'InsightResponse'] },
  { m: 'systemconfig', desc: 'Cấu hình hệ thống và phân vai trò', dto: ['ConfigResponse', 'UpdateConfigRequest', 'AssignRoleRequest'] },
  { m: 'wallet', desc: 'Ví điện tử của người dùng', dto: ['WalletResponse', 'TopupRequest'] },
  { m: 'promotion', desc: 'Khuyến mãi', dto: ['PromotionRequest', 'PromotionResponse'] },
];

const LOCAL_FILES = [
  ['AppDatabase', 'Cơ sở dữ liệu Room, chỉ dùng làm cache cục bộ', 'FR-U04'],
  ['EventDao', 'Truy vấn cache sự kiện', 'FR-U04'],
  ['TicketDao', 'Truy vấn cache vé', 'FR-U14'],
  ['OrderDao', 'Truy vấn cache đơn hàng', 'FR-U16'],
  ['EventEntity', 'Bản ghi cache của sự kiện', 'FR-U04'],
  ['TicketEntity', 'Bản ghi cache của vé', 'FR-U14'],
  ['OrderEntity', 'Bản ghi cache của đơn hàng', 'FR-U16'],
  ['FavoriteEntity', 'Bản ghi cache của sự kiện yêu thích', 'FR-U06'],
  ['CacheCleaner', 'Dọn cache theo hạn mức và theo đăng nhập', 'FR-U16'],
];

const SESSION_FILES = [
  ['SessionManager', 'Quản lý phiên đăng nhập hiện tại', 'FR-U02'],
  ['AuthSession', 'Dữ liệu phiên: access token, refresh token, hạn dùng', 'FR-U02'],
  ['TokenStore', 'Lưu trữ token an toàn bằng EncryptedSharedPreferences', 'FR-U02'],
  ['RoleStore', 'Lưu vai trò hiện tại để điều hướng vùng giao diện', 'FR-U03'],
];

const MAPPER_FILES = [
  ['EventMapper', 'Chuyển đổi DTO sự kiện sang model hiển thị', 'FR-U05'],
  ['TicketMapper', 'Chuyển đổi DTO vé sang model hiển thị', 'FR-U14'],
  ['OrderMapper', 'Chuyển đổi DTO đơn hàng sang model hiển thị', 'FR-U16'],
  ['UserMapper', 'Chuyển đổi DTO người dùng sang model hiển thị', 'FR-U03'],
];

const NETWORK_FILES = [
  ['ApiClient', 'Khởi tạo Retrofit, base URL lấy từ BuildConfig', 'FR-U01'],
  ['AuthInterceptor', 'Gắn access token vào mọi request đã đăng nhập', 'FR-U02'],
  ['TokenProvider', 'Cung cấp access token hợp lệ cho interceptor', 'FR-U02'],
  ['ResponseWrapper', 'Bọc kết quả trả về cùng mã lỗi HTTP', 'FR-U01'],
  ['ApiException', 'Ngoại lệ chuẩn hóa cho lỗi API', 'FR-U01'],
  ['NetworkMonitor', 'Theo dõi trạng thái mạng để hiển thị thông báo', 'FR-U01'],
  ['HttpLogger', 'Ghi log request và response khi bật debug', 'FR-U01'],
  ['RequestIdProvider', 'Sinh request id phục vụ truy vết log', 'FR-A18'],
];

const UTILS_FILES = [
  ['DateTimeUtils', 'Định dạng ngày giờ theo múi giờ thiết bị', 'FR-U18'],
  ['MoneyUtils', 'Định dạng và quy đổi số tiền VND', 'FR-U10'],
  ['Validators', 'Kiểm tra dữ liệu đầu vào của form', 'FR-U01'],
  ['PermissionUtils', 'Xin quyền camera và thông báo', 'FR-U17'],
  ['ErrorMapper', 'Chuyển mã lỗi API thành thông báo tiếng Việt', 'FR-U01'],
];

/* Chỉ AppFirebaseMessagingService ở lại fcm/; phần còn lại tách theo tầng. */
const FCM_FILES = [
  ['AppFirebaseMessagingService', 'Nhận và ghi nhận thông báo đẩy FCM', 'FR-U19'],
];
const FCM_SPLIT = [
  ['NotificationPayload', 'data/remote/dto/notification', 'Dữ liệu thông báo nhận được từ FCM', 'FR-U19'],
  ['DeviceTokenRepository', 'data/repository', 'Đăng ký và thu hồi device token với backend', 'FR-U19'],
  ['LocalNotificationManager', 'ui/common/notification', 'Hiển thị thông báo cục bộ trên thiết bị', 'FR-U19'],
  ['NotificationClickHandler', 'ui/common/nav', 'Điều hướng khi người dùng bấm vào thông báo', 'FR-U19'],
];

/* 17 enum + 1 class holder (EventStatus) — xem docs/00-overview.md giả định A11, A20. */
const ENUMS = [
  ['EventReviewStatus', 'Trạng thái duyệt của sự kiện', ['PENDING', 'APPROVED', 'REJECTED']],
  ['EventLifecycleStatus', 'Trạng thái vận hành của sự kiện', ['DRAFT', 'PUBLISHED', 'SUSPENDED', 'CANCELLED', 'COMPLETED']],
  ['EventCategory', 'Danh mục sự kiện', ['MUSIC', 'SPORTS', 'ART', 'FOOD', 'TECH', 'BUSINESS', 'EDUCATION', 'HEALTH', 'OTHER']],
  ['EventVisibility', 'Mức hiển thị của sự kiện', ['PUBLIC', 'PRIVATE', 'INVITE_ONLY']],
  ['TicketTypeStatus', 'Trạng thái loại vé', ['DRAFT', 'ACTIVE', 'SOLD_OUT', 'PAUSED']],
  ['OrderStatus', 'Trạng thái đơn hàng', ['PENDING', 'PAID', 'CANCELLED', 'REFUNDED', 'EXPIRED']],
  ['PaymentStatus', 'Trạng thái thanh toán', ['PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED', 'REFUNDED']],
  ['PaymentMethod', 'Phương thức thanh toán', ['MOMO', 'VNPAY', 'ZALOPAY', 'BANK_TRANSFER', 'CARD', 'CASH']],
  ['RefundStatus', 'Trạng thái hoàn tiền', ['PENDING', 'APPROVED', 'REJECTED', 'PROCESSING', 'COMPLETED']],
  ['CheckinMethod', 'Cách thức check-in', ['QR_SCAN', 'MANUAL']],
  ['CheckinStatus', 'Kết quả check-in', ['SUCCESS', 'DUPLICATE', 'INVALID', 'NOT_FOUND']],
  ['NotificationType', 'Loại thông báo', ['SYSTEM', 'EVENT', 'TICKET', 'ORDER', 'PAYMENT', 'REFUND', 'PROMOTION', 'SUPPORT', 'ORGANIZER', 'ADMIN']],
  ['ReportType', 'Loại báo cáo', ['EVENT_SUMMARY', 'SALES', 'ATTENDANCE', 'PLATFORM_OVERVIEW', 'FINANCE']],
  ['TransactionStatus', 'Trạng thái giao dịch', ['PENDING', 'SUCCESS', 'FAILED', 'REFUNDED', 'CANCELLED']],
  ['WalletTransactionType', 'Loại giao dịch ví', ['DEPOSIT', 'WITHDRAW', 'REFUND', 'PAYMENT', 'ADJUSTMENT']],
  ['PromotionType', 'Loại khuyến mãi', ['PERCENTAGE', 'FIXED_AMOUNT', 'EARLY_BIRD']],
  ['Role', 'Vai trò người dùng, mirror RoleName của backend', ['USER', 'ORGANIZER', 'ADMIN']],
];

/* ------------------------------------------------------------------ */
/* 4. SINH JAVA                                                        */
/* ------------------------------------------------------------------ */

const KIND = {
  fragment: { extends: 'androidx.fragment.app.Fragment', imports: ['androidx.fragment.app.Fragment'] },
  viewmodel: { extends: 'androidx.lifecycle.ViewModel', imports: ['androidx.lifecycle.ViewModel'] },
  adapter: { extends: 'androidx.recyclerview.widget.RecyclerView.Adapter<RecyclerView.ViewHolder>', imports: ['androidx.recyclerview.widget.RecyclerView'] },
  dialog: { extends: 'androidx.fragment.app.DialogFragment', imports: ['androidx.fragment.app.DialogFragment'] },
  activity: { extends: 'androidx.appcompat.app.AppCompatActivity', imports: ['androidx.appcompat.app.AppCompatActivity'] },
  application: { extends: 'android.app.Application', imports: ['android.app.Application'] },
};

function javaFile(pkg, name, kind, todo, extra = '') {
  const lines = [`package ${pkg};`, ''];
  const k = KIND[kind];
  if (k) {
    for (const imp of k.imports) lines.push(`import ${imp};`);
    lines.push('');
  }
  lines.push(`// TODO: ${todo}`);
  if (extra) lines.push(extra);
  if (kind === 'adapter') {
    // Adapter là abstract class: bắt buộc cài đặt 3 phương thức trừu tượng
    // thì mới biên dịch được. Thân vẫn rỗng, chỉ trả về giá trị trung tính.
    lines.push(`public class ${name} extends ${k.extends} {`);
    lines.push('');
    lines.push('    @Override');
    lines.push('    public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {');
    lines.push('        return null;');
    lines.push('    }');
    lines.push('');
    lines.push('    @Override');
    lines.push('    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {');
    lines.push('    }');
    lines.push('');
    lines.push('    @Override');
    lines.push('    public int getItemCount() {');
    lines.push('        return 0;');
    lines.push('    }');
    lines.push('}');
    return lines.join('\n');
  }
  if (k) {
    lines.push(`public class ${name} extends ${k.extends} {`);
  } else if (kind === 'interface') {
    lines.push(`public interface ${name} {`);
  } else {
    lines.push(`public class ${name} {`);
  }
  lines.push('}');
  return lines.join('\n');
}

function customViewFile(pkg, name, todo) {
  return [
    `package ${pkg};`,
    '',
    'import android.content.Context;',
    'import android.util.AttributeSet;',
    'import android.view.View;',
    '',
    `// TODO: ${todo}`,
    `public class ${name} extends View {`,
    '',
    `    public ${name}(Context context) {`,
    '        super(context);',
    '    }',
    '',
    `    public ${name}(Context context, AttributeSet attrs) {`,
    '        super(context, attrs);',
    '    }',
    '}',
  ].join('\n');
}

function enumFile(pkg, name, todo, values) {
  return [
    `package ${pkg};`,
    '',
    `// TODO: ${todo}`,
    `public enum ${name} {`,
    values.map((v) => `    ${v}`).join(',\n'),
    '}',
  ].join('\n');
}

/* 4.1 Fragment + ViewModel cho 86 màn hình */
for (const s of SCREENS) {
  const pkg = javaPkg(s);
  const cls = screenName(s);
  const todo = `${title(s.desc)} | ${s.fr}`;
  write(javaPath(pkg, `${cls}Fragment.java`),
    javaFile(pkg, `${cls}Fragment`, 'fragment', todo));
  write(javaPath(pkg, `${cls}ViewModel.java`),
    javaFile(pkg, `${cls}ViewModel`, 'viewmodel', `ViewModel của màn hình ${s.name} - nạp dữ liệu qua Repository | ${s.fr}`));
}

/* 4.2 Activity (Splash + 3 host → ui/host, Auth → ui/auth) */
for (const a of ACTIVITIES) {
  write(javaPath(a.pkg, `${a.name}.java`),
    javaFile(a.pkg, a.name, 'activity', `${a.desc} | ${a.fr}`));
}

/* 4.4 Adapter / View */
for (const row of ADAPTERS) {
  const [name, desc, fr] = row;
  const pkg = adapterPkg(row);
  write(javaPath(pkg, `${name}.java`), javaFile(pkg, name, 'adapter', `${desc} | ${fr}`));
}
for (const [name, desc, fr] of VIEWS) {
  write(javaPath(`${PKG}.ui.common.view`, `${name}.java`),
    customViewFile(`${PKG}.ui.common.view`, name, `${desc} | ${fr}`));
}

/* 4.6 Api + Repository + DTO — tên class PascalCase, tên package giữ theo module backend. */
const IRREGULAR = { ai: 'Ai', systemconfig: 'SystemConfig', tickettype: 'TicketType' };
const pascal = (m) => IRREGULAR[m] ?? m.charAt(0).toUpperCase() + m.slice(1);

for (const mod of MODULES) {
  const cls = pascal(mod.m);
  write(javaPath(`${PKG}.data.remote.api`, `${cls}Api.java`),
    javaFile(`${PKG}.data.remote.api`, `${cls}Api`, 'interface', `Retrofit Api cho module ${mod.m}: ${mod.desc}`));

  write(javaPath(`${PKG}.data.repository`, `${cls}Repository.java`),
    javaFile(`${PKG}.data.repository`, `${cls}Repository`, 'class', `Repository cho module ${mod.m}: ${mod.desc}. Chỉ đọc trạng thái chính thức từ backend`));

  for (const dto of mod.dto) {
    write(javaPath(`${PKG}.data.remote.dto.${mod.m}`, `${dto}.java`),
      javaFile(`${PKG}.data.remote.dto.${mod.m}`, dto, 'class', `DTO ${dto} của module ${mod.m} - tên và trường phải khớp backend`));
  }
}

/* 4.7 local / session / mapper */
for (const [name, desc, fr] of LOCAL_FILES) {
  const sub = name.endsWith('Entity') ? '.entity' : name.endsWith('Dao') ? '.dao' : '';
  const pkg = `${PKG}.data.local${sub}`;
  write(javaPath(pkg, `${name}.java`),
    javaFile(pkg, name, 'class', `${desc}. Cache cục bộ, KHÔNG phải nguồn sự thật | ${fr}`));
}
/* Session là hạ tầng dùng chung cho AuthInterceptor nên nằm ở core, không phải data. */
for (const [name, desc, fr] of SESSION_FILES) {
  write(javaPath(`${PKG}.core.session`, `${name}.java`),
    javaFile(`${PKG}.core.session`, name, 'class', `${desc} | ${fr}`));
}
for (const [name, desc, fr] of MAPPER_FILES) {
  write(javaPath(`${PKG}.data.mapper`, `${name}.java`),
    javaFile(`${PKG}.data.mapper`, name, 'class', `${desc} | ${fr}`));
}

/* 4.8 core */
for (const [name, desc, values] of ENUMS) {
  write(javaPath(`${PKG}.core.enums`, `${name}.java`),
    enumFile(`${PKG}.core.enums`, name, `${desc}. Nhãn hiển thị sẽ map ở tầng UI`, values));
}
write(javaPath(`${PKG}.core.enums`, 'EventStatus.java'), [
  `package ${PKG}.core.enums;`,
  '',
  '// TODO: Gộp 2 trạng thái của sự kiện - KHÔNG dùng 1 trường status đơn | FR-O04, FR-A05',
  '// Ghi chú: đây là class chứa dữ liệu, không phải enum.',
  'public class EventStatus {',
  '',
  '    public EventReviewStatus reviewStatus;',
  '    public EventLifecycleStatus lifecycleStatus;',
  '}',
].join('\n'));
for (const [name, desc, fr] of UTILS_FILES) {
  const pkg = name === 'PermissionUtils' ? `${PKG}.core.platform` : `${PKG}.core.util`;
  write(javaPath(pkg, `${name}.java`), javaFile(pkg, name, 'class', `${desc} | ${fr}`));
}
for (const [name, desc, fr] of NETWORK_FILES) {
  const pkg = name === 'NetworkMonitor' ? `${PKG}.core.platform`
    : name === 'TokenProvider' ? `${PKG}.core.session`
    : `${PKG}.core.network`;
  write(javaPath(pkg, `${name}.java`), javaFile(pkg, name, 'class', `${desc} | ${fr}`));
}
write(javaPath(`${PKG}.core.common`, 'AppLogger.java'),
  javaFile(`${PKG}.core.common`, 'AppLogger', 'class', 'Ghi log có tiền tố tag chung của ứng dụng | FR-U01'));
write(javaPath(`${PKG}.core.common`, 'AppConstants.java'),
  javaFile(`${PKG}.core.common`, 'AppConstants', 'class', 'Hằng số dùng chung: paging size, timeout, giá trị mặc định | FR-U01'));

/* 4.9 fcm — chỉ còn entry point; phần còn lại đã tách về data/ và ui/ */
for (const [name, desc, fr] of FCM_FILES) {
  write(javaPath(`${PKG}.fcm`, `${name}.java`),
    javaFile(`${PKG}.fcm`, name, 'class', `${desc} | ${fr}`));
}
for (const [name, pkgRel, desc, fr] of FCM_SPLIT) {
  const pkg = `${PKG}.${pkgRel.split('/').join('.')}`;
  write(javaPath(pkg, `${name}.java`), javaFile(pkg, name, 'class', `${desc} | ${fr}`));
}

/* 4.10 Application */
write(javaPath(PKG, 'EventApp.java'),
  javaFile(PKG, 'EventApp', 'application', 'Application của ứng dụng: khởi tạo container phụ thuộc và FCM | FR-U01'));

/* 4.11 Test */
write(join(TEST, 'core', 'util', 'MoneyUtilsTest.java'), [
  `package ${PKG}.core.util;`,
  '',
  'import static org.junit.Assert.assertTrue;',
  '',
  'import org.junit.Test;',
  '',
  '// TODO: kịch bản test định dạng tiền VND (1000 -> "1.000 đ") | FR-U10',
  'public class MoneyUtilsTest {',
  '',
  '    @Test',
  '    public void placeholder_moneyFormat_shouldNotThrow() {',
  '        assertTrue(true);',
  '    }',
  '}',
].join('\n'));
write(join(TEST, 'core', 'util', 'DateTimeUtilsTest.java'), [
  `package ${PKG}.core.util;`,
  '',
  'import static org.junit.Assert.assertTrue;',
  '',
  'import org.junit.Test;',
  '',
  '// TODO: kịch bản test định dạng ngày giờ theo múi giờ thiết bị | FR-U18',
  'public class DateTimeUtilsTest {',
  '',
  '    @Test',
  '    public void placeholder_dateTimeFormat_shouldNotThrow() {',
  '        assertTrue(true);',
  '    }',
  '}',
].join('\n'));
write(join(TEST, 'data', 'mapper', 'EventMapperTest.java'), [
  `package ${PKG}.data.mapper;`,
  '',
  'import static org.junit.Assert.assertTrue;',
  '',
  'import org.junit.Test;',
  '',
  '// TODO: kịch bản test map trạng thái sự kiện gồm review_status và lifecycle_status | FR-O04',
  'public class EventMapperTest {',
  '',
  '    @Test',
  '    public void placeholder_eventStatusMapping_shouldNotThrow() {',
  '        assertTrue(true);',
  '    }',
  '}',
].join('\n'));

/* ------------------------------------------------------------------ */
/* 5. SINH LAYOUT                                                      */
/* ------------------------------------------------------------------ */

function placeholderLayout(kind, desc, fr, extraAttrs = '') {
  return [
    '<?xml version="1.0" encoding="utf-8"?>',
    `<!-- TODO: ${desc} | ${fr} -->`,
    `<${kind} xmlns:android="http://schemas.android.com/apk/res/android"`,
    '    android:layout_width="match_parent"',
    '    android:layout_height="wrap_content"' + extraAttrs + ' />',
  ].join('\n');
}

/* 5.1 layout của 86 fragment */
for (const s of SCREENS) {
  write(join(RES, 'layout', `${layoutName(s)}.xml`),
    placeholderLayout('FrameLayout', `Màn hình ${s.name}: ${s.desc}`, s.fr, '\n    android:background="@color/brand_background"'));
}

/* 5.2 layout item_ (22) */
for (const [name, desc, fr] of ADAPTERS) {
  write(join(RES, 'layout', `item_${snake(name.replace('Adapter', ''))}.xml`),
    placeholderLayout('LinearLayout', `Item ${desc.toLowerCase()} trong danh sách`, fr, '\n    android:orientation="vertical"'));
}

/* 5.3 layout dialog_ — hiện không có dialog dùng chung; dialog riêng của feature
   nằm trong ui/<role>/<area>/dialog/ và layout của nó được tạo cùng lúc với class. */

/* 5.4 layout activity_ (5) */
write(join(RES, 'layout', 'activity_splash.xml'), [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<!-- TODO: Màn hình khởi động, hiển thị logo và kiểm tra phiên đăng nhập | FR-U01, FR-U02 -->',
  '<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"',
  '    android:layout_width="match_parent"',
  '    android:layout_height="match_parent"',
  '    android:background="@color/brand_primary" />',
].join('\n'));

function hostActivityLayout(name, navGraph, menu, desc, fr) {
  return [
    '<?xml version="1.0" encoding="utf-8"?>',
    `<!-- TODO: ${desc} | ${fr} -->`,
    '<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"',
    '    xmlns:app="http://schemas.android.com/apk/res-auto"',
    '    android:layout_width="match_parent"',
    '    android:layout_height="match_parent">',
    '',
    '    <androidx.fragment.app.FragmentContainerView',
    '        android:id="@+id/nav_host_fragment"',
    '        android:name="androidx.navigation.fragment.NavHostFragment"',
    '        android:layout_width="0dp"',
    '        android:layout_height="0dp"',
    '        app:defaultNavHost="true"',
    `        app:navGraph="@navigation/${navGraph}"`,
    '        app:layout_constraintTop_toTopOf="parent"',
    '        app:layout_constraintBottom_toTopOf="@id/bottom_navigation"',
    '        app:layout_constraintStart_toStartOf="parent"',
    '        app:layout_constraintEnd_toEndOf="parent" />',
    '',
    '    <com.google.android.material.bottomnavigation.BottomNavigationView',
    '        android:id="@+id/bottom_navigation"',
    '        android:layout_width="0dp"',
    '        android:layout_height="wrap_content"',
    `        app:menu="@menu/${menu}"`,
    '        app:layout_constraintBottom_toBottomOf="parent"',
    '        app:layout_constraintStart_toStartOf="parent"',
    '        app:layout_constraintEnd_toEndOf="parent" />',
    '',
    '</androidx.constraintlayout.widget.ConstraintLayout>',
  ].join('\n');
}

write(join(RES, 'layout', 'activity_auth.xml'), [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<!-- TODO: Host chứa luồng đăng nhập, đăng ký và quên mật khẩu | FR-U01, FR-U02 -->',
  '<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"',
  '    xmlns:app="http://schemas.android.com/apk/res-auto"',
  '    android:layout_width="match_parent"',
  '    android:layout_height="match_parent">',
  '',
  '    <androidx.fragment.app.FragmentContainerView',
  '        android:id="@+id/nav_host_fragment"',
  '        android:name="androidx.navigation.fragment.NavHostFragment"',
  '        android:layout_width="0dp"',
  '        android:layout_height="0dp"',
  '        app:defaultNavHost="true"',
  '        app:navGraph="@navigation/nav_auth"',
  '        app:layout_constraintTop_toTopOf="parent"',
  '        app:layout_constraintBottom_toBottomOf="parent"',
  '        app:layout_constraintStart_toStartOf="parent"',
  '        app:layout_constraintEnd_toEndOf="parent" />',
  '',
  '</androidx.constraintlayout.widget.ConstraintLayout>',
].join('\n'));

write(join(RES, 'layout', 'activity_user_host.xml'),
  hostActivityLayout('UserHostActivity', 'nav_user', 'menu_user_bottom', 'Host khu vực người dùng kèm bottom navigation', 'FR-U03'));
write(join(RES, 'layout', 'activity_organizer_host.xml'),
  hostActivityLayout('OrganizerHostActivity', 'nav_organizer', 'menu_organizer_bottom', 'Host khu vực nhà tổ chức kèm bottom navigation', 'FR-O02'));
write(join(RES, 'layout', 'activity_admin_host.xml'),
  hostActivityLayout('AdminHostActivity', 'nav_admin', 'menu_admin_bottom', 'Host khu vực quản trị kèm bottom navigation', 'FR-A01'));

/* ------------------------------------------------------------------ */
/* 6. SINH NAV GRAPH (4) VÀ MENU (3)                                   */
/* ------------------------------------------------------------------ */

const NAVS = [
  { key: 'auth', file: 'nav_auth', start: 'Login', desc: 'Luồng xác thực: đăng nhập, đăng ký, xác thực email, quên và đặt lại mật khẩu' },
  { key: 'user', file: 'nav_user', start: 'Home', desc: 'Khu vực người dùng: khám phá sự kiện, vé, đơn hàng, lịch, hồ sơ, đánh giá, hỗ trợ, thông báo' },
  { key: 'organizer', file: 'nav_organizer', start: 'OrganizerDashboard', desc: 'Khu vực nhà tổ chức: sự kiện, loại vé, người tham dự, check-in, hoàn tiền, báo cáo, khuyến mãi' },
  { key: 'admin', file: 'nav_admin', start: 'AdminDashboard', desc: 'Khu vực quản trị: người dùng, duyệt sự kiện, tài chính, hỗ trợ, báo cáo, hệ thống' },
];

for (const nav of NAVS) {
  const list = SCREENS.filter((s) => s.role === nav.key);
  const start = list.find((s) => s.name === nav.start);
  const lines = [
    '<?xml version="1.0" encoding="utf-8"?>',
    `<!-- TODO: ${nav.desc} | FR-U01,FR-O01,FR-A01 -->`,
    '<navigation xmlns:android="http://schemas.android.com/apk/res/android"',
    '    xmlns:app="http://schemas.android.com/apk/res-auto"',
    '    xmlns:tools="http://schemas.android.com/tools"',
    `    android:id="@+id/${nav.file}"`,
    `    app:startDestination="@id/${navId(start)}">`,
    '',
  ];
  for (const s of list) {
    lines.push('    <fragment');
    lines.push(`        android:id="@+id/${navId(s)}"`);
    lines.push(`        android:name="${javaPkg(s)}.${screenName(s)}Fragment"`);
    lines.push(`        tools:layout="@layout/${layoutName(s)}" />`);
    lines.push('');
  }
  lines.push('</navigation>');
  write(join(RES, 'navigation', `${nav.file}.xml`), lines.join('\n'));
}

const MENUS = [
  { file: 'menu_user_bottom', nav: 'nav_user', desc: 'Bottom navigation khu vực người dùng', items: [
    ['homeFragment', 'ic_home', 'nav_home'],
    ['my_ticketsFragment', 'ic_ticket', 'nav_my_tickets'],
    ['notification_listFragment', 'ic_notification', 'nav_notifications'],
    ['profileFragment', 'ic_profile', 'nav_profile'],
  ] },
  { file: 'menu_organizer_bottom', nav: 'nav_organizer', desc: 'Bottom navigation khu vực nhà tổ chức', items: [
    ['organizer_dashboardFragment', 'ic_dashboard', 'nav_organizer_dashboard'],
    ['my_eventsFragment', 'ic_event', 'nav_my_events'],
    ['checkin_monitorFragment', 'ic_checkin', 'nav_checkin'],
    ['event_reportFragment', 'ic_report', 'nav_reports'],
    ['organizer_profileFragment', 'ic_profile', 'nav_organizer_profile'],
  ] },
  { file: 'menu_admin_bottom', nav: 'nav_admin', desc: 'Bottom navigation khu vực quản trị', items: [
    ['admin_dashboardFragment', 'ic_dashboard', 'nav_admin_dashboard'],
    ['review_queueFragment', 'ic_review', 'nav_review_queue'],
    ['transaction_lookupFragment', 'ic_transaction', 'nav_transactions'],
    ['support_manageFragment', 'ic_support', 'nav_support'],
    ['audit_logFragment', 'ic_audit_log', 'nav_audit_log'],
  ] },
];

for (const m of MENUS) {
  const lines = [
    '<?xml version="1.0" encoding="utf-8"?>',
    `<!-- TODO: ${m.desc} - id của menu phải trùng id đích điều hướng trong ${m.nav}.xml -->`,
    '<menu xmlns:android="http://schemas.android.com/apk/res/android">',
  ];
  for (const [id, icon, title] of m.items) {
    lines.push('    <item');
    lines.push(`        android:id="@+id/${id}"`);
    lines.push(`        android:icon="@drawable/${icon}"`);
    lines.push(`        android:title="@string/${title}" />`);
  }
  lines.push('</menu>');
  write(join(RES, 'menu', `${m.file}.xml`), lines.join('\n'));
}

/* ------------------------------------------------------------------ */
/* 7. SINH ICON VÀ DRAWABLE                                            */
/* ------------------------------------------------------------------ */

const ICONS = {
  ic_home: 'M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z',
  ic_ticket: 'M22,10L22,6c0,-1.1 -0.9,-2 -2,-2L4,4c-1.1,0 -1.99,0.9 -1.99,2v4c1.1,0 1.99,0.9 1.99,2s-0.89,2 -1.99,2v4c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2v-4c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2zM13,17.5h-2v-2h2v2zM13,13h-2v-2h2v2zM13,8.5h-2v-2h2v2z',
  ic_calendar: 'M19,3h-1L18,1h-2v2L8,3L8,1L6,1v2L5,3c-1.11,0 -1.99,0.9 -1.99,2L3,19c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2L21,5c0,-1.1 -0.9,-2 -2,-2zM19,19L5,19L5,8h14v11z',
  ic_notification: 'M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.63,-5.64 -4.5,-6.32L13.5,4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.64,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z',
  ic_profile: 'M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z',
  ic_dashboard: 'M3,13h8L11,3L3,3v10zM3,21h8v-6L3,15v6zM13,21h8L21,11h-8v10zM13,3v6h8L21,3h-8z',
  ic_event: 'M17,12h-5v5h5v-5zM16,1v2L8,3L8,1L6,1v2L5,3c-1.11,0 -1.99,0.9 -1.99,2L3,19c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2L21,5c0,-1.1 -0.9,-2 -2,-2h-1L18,1h-2zM19,19L5,19L5,8h14v11z',
  ic_checkin: 'M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z',
  ic_report: 'M19,3h-4.18C14.4,1.84 13.3,1 12,1c-1.3,0 -2.4,0.84 -2.82,2L5,3c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM12,3c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM14,17H7v-2h7v2zM17,13H7v-2h10v2zM17,9H7V7h10v2z',
  ic_review: 'M20,3H4c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z',
  ic_transaction: 'M20,4H4c-1.11,0 -1.99,0.89 -1.99,2L2,18c0,1.11 0.89,2 2,2h16c1.11,0 2,-0.89 2,-2V6c0,-1.11 -0.89,-2 -2,-2zM20,18H4v-6h16v6zM20,8H4V6h16v2z',
  ic_support: 'M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,19h-2v-2h2v2zM15.07,11.25l-0.9,0.92C13.45,12.9 13,13.5 13,15h-2v-0.5c0,-1.1 0.45,-2.1 1.17,-2.83l1.24,-1.26c0.37,-0.36 0.59,-0.86 0.59,-1.41 0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2H8c0,-2.21 1.79,-4 4,-4s4,1.79 4,4c0,0.88 -0.36,1.68 -0.93,2.25z',
  ic_audit_log: 'M14,2H6C4.9,2 4,2.9 4,4v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V8L14,2zM16,18H8v-2h8v2zM16,14H8v-2h8v2zM13,9V3.5L18.5,9H13z',
  ic_image_placeholder: 'M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z',
};

for (const [name, path] of Object.entries(ICONS)) {
  write(join(RES, 'drawable', `${name}.xml`), [
    '<?xml version="1.0" encoding="utf-8"?>',
    `<!-- TODO: Icon ${name} - thay bằng bộ icon chuẩn của thiết kế | FR-U03 -->`,
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
    '    android:width="24dp"',
    '    android:height="24dp"',
    '    android:viewportWidth="24"',
    '    android:viewportHeight="24">',
    '    <path',
    '        android:fillColor="#FF0F172A"',
    `        android:pathData="${path}" />`,
    '</vector>',
  ].join('\n'));
}

write(join(RES, 'drawable', 'ic_launcher_foreground.xml'), [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
  '    android:width="108dp"',
  '    android:height="108dp"',
  '    android:viewportWidth="108"',
  '    android:viewportHeight="108">',
  '    <path',
  '        android:fillColor="#FFFFFFFF"',
  '        android:pathData="M34,38h40v6h-40z M34,50h40v6h-40z M34,62h24v6h-24z" />',
  '</vector>',
].join('\n'));

write(join(RES, 'drawable', 'bg_rounded.xml'), [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<!-- TODO: Nền bo góc dùng cho thẻ và khung nội dung | FR-U04 -->',
  '<shape xmlns:android="http://schemas.android.com/apk/res/android"',
  '    android:shape="rectangle">',
  '    <solid android:color="@color/brand_surface" />',
  '    <corners android:radius="@dimen/corner_md" />',
  '    <stroke',
  '        android:width="1dp"',
  '        android:color="@color/brand_outline" />',
  '</shape>',
].join('\n'));

write(join(RES, 'drawable', 'bg_input.xml'), [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<!-- TODO: Nền ô nhập liệu | FR-U01 -->',
  '<shape xmlns:android="http://schemas.android.com/apk/res/android"',
  '    android:shape="rectangle">',
  '    <solid android:color="@color/brand_surface" />',
  '    <corners android:radius="@dimen/corner_sm" />',
  '    <stroke',
  '        android:width="1dp"',
  '        android:color="@color/brand_outline" />',
  '</shape>',
].join('\n'));

write(join(RES, 'mipmap-anydpi-v26', 'ic_launcher.xml'), [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">',
  '    <background android:drawable="@color/ic_launcher_background" />',
  '    <foreground android:drawable="@drawable/ic_launcher_foreground" />',
  '</adaptive-icon>',
].join('\n'));

write(join(RES, 'mipmap-anydpi-v26', 'ic_launcher_round.xml'), [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">',
  '    <background android:drawable="@color/ic_launcher_background" />',
  '    <foreground android:drawable="@drawable/ic_launcher_foreground" />',
  '</adaptive-icon>',
].join('\n'));

for (const [file, round] of [['ic_launcher', false], ['ic_launcher_round', true]]) {
  write(join(RES, 'mipmap', `${file}.xml`), [
    '<?xml version="1.0" encoding="utf-8"?>',
    `<!-- TODO: Icon ứng dụng${round ? ' (bản tròn)' : ''} - thay bằng artwork chính thức | FR-U01 -->`,
    '<layer-list xmlns:android="http://schemas.android.com/apk/res/android">',
    '    <item android:drawable="@color/ic_launcher_background" />',
    '    <item android:drawable="@drawable/ic_launcher_foreground" />',
    '</layer-list>',
  ].join('\n'));
}

/* ------------------------------------------------------------------ */
/* 8. TỰ KIỂM TRA                                                     */
/* ------------------------------------------------------------------ */

const errors = [];

const layoutNames = new Set();
for (const s of SCREENS) {
  const n = layoutName(s);
  if (layoutNames.has(n)) errors.push(`Trùng tên layout: ${n}`);
  layoutNames.add(n);
}

const navIds = new Set();
for (const s of SCREENS) {
  const id = navId(s);
  if (navIds.has(id)) errors.push(`Trùng nav id: ${id}`);
  navIds.add(id);
}

for (const m of MENUS) {
  for (const [id] of m.items) {
    if (!navIds.has(id)) errors.push(`Menu ${m.file}: id ${id} không có trong nav graph`);
  }
}

/* Mọi class Java phải có tên duy nhất trên toàn project (không chỉ khác package). */
const classNames = new Set();
for (const s of SCREENS) {
  for (const suffix of ['Fragment', 'ViewModel']) {
    const n = `${screenName(s)}${suffix}`;
    if (classNames.has(n)) errors.push(`Trùng tên class: ${n}`);
    classNames.add(n);
  }
}

for (const mod of MODULES) {
  if (mod.dto.length === 0) errors.push(`Module ${mod.m} chưa có DTO`);
}

if (errors.length) {
  console.error('LỖI KHUNG:');
  for (const e of errors) console.error('  - ' + e);
  process.exit(1);
}

/* ------------------------------------------------------------------ */
/* 9. IN KẾT QUẢ                                                      */
/* ------------------------------------------------------------------ */

console.log(`Đã sinh ${written} file mới, bỏ qua ${skipped} file đã tồn tại${FORCE ? ' (--force: ghi đè)' : ''}.`);
console.log(`  Màn hình: ${SCREENS.length} (auth ${SCREENS.filter((s) => s.role === 'auth').length}, user ${SCREENS.filter((s) => s.role === 'user').length}, organizer ${SCREENS.filter((s) => s.role === 'organizer').length}, admin ${SCREENS.filter((s) => s.role === 'admin').length})`);
console.log(`  Layout fragment: ${SCREENS.length}, item: ${ADAPTERS.length}, activity: ${ACTIVITIES.length}`);
console.log(`  Module data: ${MODULES.length} (Api + Repository), DTO: ${MODULES.reduce((a, m) => a + m.dto.length, 0)}`);
console.log(`  Enum: ${ENUMS.length} + EventStatus, Activity: ${ACTIVITIES.length}, Adapter: ${ADAPTERS.length}, View: ${VIEWS.length}`);

if (process.argv.includes('--tree')) {
  const base = join(APP, 'java', 'com', 'eventapp', 'app');
  const walk = (dir, depth) => {
    if (depth > 3) return;
    const entries = readdirSync(dir).sort();
    for (const e of entries) {
      const full = join(dir, e);
      if (statSync(full).isDirectory()) {
        console.log('  '.repeat(depth) + e + '/');
        walk(full, depth + 1);
      }
    }
  };
  console.log('\nCây thư mục (3 cấp):');
  walk(base, 0);
}
