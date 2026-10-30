# 01 — Bản đồ màn hình (86 màn hình)

Tổng: **86 màn hình** = 5 auth + 29 user + 29 organizer + 23 admin.
Mỗi màn hình = 1 `Fragment` + 1 `ViewModel` + 1 layout `fragment_<area>_<snake>.xml`.

## 1. Xác thực — 5 màn hình (`nav_auth`, start `loginFragment`)

| # | Màn hình | Fragment | Layout | Nav id | FR |
|---|---|---|---|---|---|
| 1 | Đăng nhập | `ui/auth/LoginFragment` | `fragment_auth_login.xml` | `loginFragment` | FR-U02 |
| 2 | Đăng ký | `ui/auth/RegisterFragment` | `fragment_auth_register.xml` | `registerFragment` | FR-U01 |
| 3 | Xác thực email | `ui/auth/VerifyEmailFragment` | `fragment_auth_verify_email.xml` | `verify_emailFragment` | FR-U01 |
| 4 | Quên mật khẩu | `ui/auth/ForgotPasswordFragment` | `fragment_auth_forgot_password.xml` | `forgot_passwordFragment` | FR-U02 |
| 5 | Đặt lại mật khẩu | `ui/auth/ResetPasswordFragment` | `fragment_auth_reset_password.xml` | `reset_passwordFragment` | FR-U02 |

## 2. Người dùng — 29 màn hình (`nav_user`, start `homeFragment`)

Bottom navigation (thanh nổi, 4 mục): `homeFragment`, `my_ticketsFragment`, `my_calendarFragment`, `profileFragment`.
Thông báo (`notification_listFragment`) và Yêu thích (`favoritesFragment`) mở từ header trang Khám phá.

| # | Màn hình | Fragment | Layout | Nav id | FR |
|---|---|---|---|---|---|
| 1 | Trang chủ | `ui/user/home/HomeFragment` | `fragment_user_home.xml` | `homeFragment` | FR-U04 |
| 2 | Danh sách sự kiện | `ui/user/home/EventListFragment` | `fragment_user_home_event_list.xml` | `event_listFragment` | FR-U04 |
| 3 | Bộ lọc sự kiện | `ui/user/home/EventFilterFragment` | `fragment_user_home_event_filter.xml` | `event_filterFragment` | FR-U04 |
| 4 | Chi tiết sự kiện | `ui/user/home/EventDetailFragment` | `fragment_user_home_event_detail.xml` | `event_detailFragment` | FR-U05 |
| 5 | Gợi ý sự kiện | `ui/user/home/RecommendFragment` | `fragment_user_home_recommend.xml` | `recommendFragment` | FR-U07 |
| 6 | Yêu thích | `ui/user/favorite/FavoritesFragment` | `fragment_user_favorites.xml` | `favoritesFragment` | FR-U06 |
| 7 | Vé của tôi | `ui/user/ticket/MyTicketsFragment` | `fragment_user_ticket_my_tickets.xml` | `my_ticketsFragment` | FR-U14 |
| 8 | Chi tiết vé | `ui/user/ticket/TicketDetailFragment` | `fragment_user_ticket_detail.xml` | `ticket_detailFragment` | FR-U14 |
| 9 | Hủy vé | `ui/user/ticket/CancelTicketFragment` | `fragment_user_ticket_cancel_ticket.xml` | `cancel_ticketFragment` | FR-U15 |
| 10 | Đăng ký miễn phí | `ui/user/order/FreeRegisterFragment` | `fragment_user_order_free_register.xml` | `free_registerFragment` | FR-U09 |
| 11 | Thanh toán | `ui/user/order/CheckoutFragment` | `fragment_user_order_checkout.xml` | `checkoutFragment` | FR-U10 |
| 12 | QR thanh toán | `ui/user/order/PaymentQrFragment` | `fragment_user_order_payment_qr.xml` | `payment_qrFragment` | FR-U11 |
| 13 | Trạng thái đơn hàng | `ui/user/order/OrderStatusFragment` | `fragment_user_order_status.xml` | `order_statusFragment` | FR-U12 |
| 14 | Lịch sử đơn hàng | `ui/user/order/OrderHistoryFragment` | `fragment_user_order_history.xml` | `order_historyFragment` | FR-U16 |
| 15 | Chi tiết đơn hàng | `ui/user/order/UserOrderDetailFragment` | `fragment_user_order_detail.xml` | `user_order_detailFragment` | FR-U16 |
| 16 | Quét QR | `ui/user/checkin/ScanQrFragment` | `fragment_user_checkin_scan_qr.xml` | `scan_qrFragment` | FR-U17 |
| 17 | Kết quả check-in | `ui/user/checkin/CheckinResultFragment` | `fragment_user_checkin_result.xml` | `checkin_resultFragment` | FR-U17 |
| 18 | Lịch của tôi | `ui/user/calendar/MyCalendarFragment` | `fragment_user_calendar_my_calendar.xml` | `my_calendarFragment` | FR-U18 |
| 19 | Cá nhân | `ui/user/profile/ProfileFragment` | `fragment_user_profile.xml` | `profileFragment` | FR-U03 |
| 20 | Sửa hồ sơ | `ui/user/profile/EditProfileFragment` | `fragment_user_profile_edit_profile.xml` | `edit_profileFragment` | FR-U03 |
| 21 | Tùy chọn | `ui/user/profile/PreferencesFragment` | `fragment_user_profile_preferences.xml` | `preferencesFragment` | FR-U03 |
| 22 | Quyền riêng tư | `ui/user/profile/PrivacySettingsFragment` | `fragment_user_profile_privacy_settings.xml` | `privacy_settingsFragment` | FR-U22 |
| 23 | Trở thành nhà tổ chức | `ui/user/profile/BecomeOrganizerFragment` | `fragment_user_profile_become_organizer.xml` | `become_organizerFragment` | FR-O01 |
| 24 | Viết đánh giá | `ui/user/review/WriteReviewFragment` | `fragment_user_review_write_review.xml` | `write_reviewFragment` | FR-U20 |
| 25 | Đánh giá sự kiện | `ui/user/review/EventReviewsFragment` | `fragment_user_review_event_reviews.xml` | `event_reviewsFragment` | FR-U20 |
| 26 | Danh sách hỗ trợ | `ui/user/support/SupportListFragment` | `fragment_user_support_list.xml` | `support_listFragment` | FR-U21 |
| 27 | Tạo yêu cầu hỗ trợ | `ui/user/support/CreateSupportFragment` | `fragment_user_support_create_support.xml` | `create_supportFragment` | FR-U21 |
| 28 | Chi tiết hỗ trợ | `ui/user/support/SupportDetailFragment` | `fragment_user_support_detail.xml` | `support_detailFragment` | FR-U21 |
| 29 | Thông báo | `ui/user/notification/NotificationListFragment` | `fragment_user_notification_list.xml` | `notification_listFragment` | FR-U19 |

## 3. Nhà tổ chức — 29 màn hình (`nav_organizer`, start `organizer_dashboardFragment`)

Bottom navigation: `organizer_dashboardFragment`, `my_eventsFragment`, `checkin_monitorFragment`, `event_reportFragment`, `organizer_profileFragment`.

| # | Màn hình | Fragment | Layout | Nav id | FR |
|---|---|---|---|---|---|
| 1 | Đăng ký nhà tổ chức | `ui/organizer/onboarding/OrganizerRegisterFragment` | `fragment_organizer_onboarding_register.xml` | `organizer_registerFragment` | FR-O01 |
| 2 | Hồ sơ nhà tổ chức | `ui/organizer/onboarding/OrganizerProfileFragment` | `fragment_organizer_onboarding_profile.xml` | `organizer_profileFragment` | FR-O02 |
| 3 | Trạng thái đơn đăng ký | `ui/organizer/onboarding/ApplicationStatusFragment` | `fragment_organizer_onboarding_application_status.xml` | `application_statusFragment` | FR-O01 |
| 4 | Tổng quan | `ui/organizer/dashboard/OrganizerDashboardFragment` | `fragment_organizer_dashboard.xml` | `organizer_dashboardFragment` | FR-O03 |
| 5 | Sự kiện của tôi | `ui/organizer/event/MyEventsFragment` | `fragment_organizer_event_my_events.xml` | `my_eventsFragment` | FR-O04 |
| 6 | Form sự kiện | `ui/organizer/event/EventFormFragment` | `fragment_organizer_event_form.xml` | `event_formFragment` | FR-O04,05,08,09 |
| 7 | Mô tả AI | `ui/organizer/event/AiDescriptionFragment` | `fragment_organizer_event_ai_description.xml` | `ai_descriptionFragment` | FR-O06 |
| 8 | Quản lý media | `ui/organizer/event/MediaManageFragment` | `fragment_organizer_event_media_manage.xml` | `media_manageFragment` | FR-O07 |
| 9 | Gửi duyệt | `ui/organizer/event/SubmitReviewFragment` | `fragment_organizer_event_submit_review.xml` | `submit_reviewFragment` | FR-O13 |
| 10 | Kết quả duyệt | `ui/organizer/event/ReviewResultFragment` | `fragment_organizer_event_review_result.xml` | `review_resultFragment` | FR-O14 |
| 11 | Lịch sử sự kiện | `ui/organizer/event/EventHistoryFragment` | `fragment_organizer_event_history.xml` | `event_historyFragment` | FR-O29 |
| 12 | Hủy sự kiện | `ui/organizer/event/CancelEventFragment` | `fragment_organizer_event_cancel_event.xml` | `cancel_eventFragment` | FR-O24 |
| 13 | Bật/tắt bán vé | `ui/organizer/event/SaleControlFragment` | `fragment_organizer_event_sale_control.xml` | `sale_controlFragment` | FR-O15 |
| 14 | Loại vé | `ui/organizer/ticket/TicketTypeListFragment` | `fragment_organizer_ticket_type_list.xml` | `ticket_type_listFragment` | FR-O10 |
| 15 | Form loại vé | `ui/organizer/ticket/TicketTypeFormFragment` | `fragment_organizer_ticket_type_form.xml` | `ticket_type_formFragment` | FR-O10,11,12 |
| 16 | Người đăng ký | `ui/organizer/attendee/RegistrationListFragment` | `fragment_organizer_attendee_registration_list.xml` | `registration_listFragment` | FR-O16 |
| 17 | Đơn hàng | `ui/organizer/attendee/OrderListFragment` | `fragment_organizer_attendee_order_list.xml` | `order_listFragment` | FR-O17 |
| 18 | Chi tiết đơn hàng | `ui/organizer/attendee/OrganizerOrderDetailFragment` | `fragment_organizer_attendee_order_detail.xml` | `organizer_order_detailFragment` | FR-O17 |
| 19 | QR check-in sự kiện | `ui/organizer/checkin/ShowCheckinQrFragment` | `fragment_organizer_checkin_show_checkin_qr.xml` | `show_checkin_qrFragment` | FR-O19 |
| 20 | Giám sát check-in | `ui/organizer/checkin/CheckinMonitorFragment` | `fragment_organizer_checkin_monitor.xml` | `checkin_monitorFragment` | FR-O21 |
| 21 | Tra cứu vé | `ui/organizer/checkin/TicketLookupFragment` | `fragment_organizer_checkin_ticket_lookup.xml` | `ticket_lookupFragment` | FR-O22 |
| 22 | Check-in thủ công | `ui/organizer/checkin/ManualCheckinFragment` | `fragment_organizer_checkin_manual_checkin.xml` | `manual_checkinFragment` | FR-O22 |
| 23 | Quản lý nhân sự | `ui/organizer/checkin/StaffManageFragment` | `fragment_organizer_checkin_staff_manage.xml` | `staff_manageFragment` | FR-O20 |
| 24 | Gửi thông báo | `ui/organizer/notification/SendAttendeeNoticeFragment` | `fragment_organizer_notification_send_attendee_notice.xml` | `send_attendee_noticeFragment` | FR-O23 |
| 25 | Hoàn tiền | `ui/organizer/refund/OrganizerRefundsFragment` | `fragment_organizer_refunds.xml` | `organizer_refundsFragment` | FR-O25 |
| 26 | Báo cáo sự kiện | `ui/organizer/report/EventReportFragment` | `fragment_organizer_report_event_report.xml` | `event_reportFragment` | FR-O26 |
| 27 | Gợi ý AI | `ui/organizer/report/AiInsightFragment` | `fragment_organizer_report_ai_insight.xml` | `ai_insightFragment` | FR-O27 |
| 28 | Xuất dữ liệu | `ui/organizer/report/ExportDataFragment` | `fragment_organizer_report_export_data.xml` | `export_dataFragment` | FR-O28 |
| 29 | Khuyến mãi | `ui/organizer/promotion/PromoManageFragment` | `fragment_organizer_promotion_promo_manage.xml` | `promo_manageFragment` | FR-O18 |

## 4. Quản trị — 23 màn hình (`nav_admin`, start `admin_dashboardFragment`)

Bottom navigation: `admin_dashboardFragment`, `review_queueFragment`, `transaction_lookupFragment`, `support_manageFragment`, `audit_logFragment`.

| # | Màn hình | Fragment | Layout | Nav id | FR |
|---|---|---|---|---|---|
| 1 | Tổng quan | `ui/admin/dashboard/AdminDashboardFragment` | `fragment_admin_dashboard.xml` | `admin_dashboardFragment` | FR-A01 |
| 2 | Quản lý người dùng | `ui/admin/user/UserManageFragment` | `fragment_admin_user_manage.xml` | `user_manageFragment` | FR-A02 |
| 3 | Chi tiết người dùng | `ui/admin/user/UserDetailFragment` | `fragment_admin_user_detail.xml` | `user_detailFragment` | FR-A02 |
| 4 | Duyệt nhà tổ chức | `ui/admin/organizer/OrganizerApprovalListFragment` | `fragment_admin_organizer_approval_list.xml` | `organizer_approval_listFragment` | FR-A03 |
| 5 | Chi tiết hồ sơ | `ui/admin/organizer/OrganizerApprovalDetailFragment` | `fragment_admin_organizer_approval_detail.xml` | `organizer_approval_detailFragment` | FR-A03 |
| 6 | Đình chỉ nhà tổ chức | `ui/admin/organizer/OrganizerSuspendFragment` | `fragment_admin_organizer_suspend.xml` | `organizer_suspendFragment` | FR-A04 |
| 7 | Hàng đợi duyệt | `ui/admin/event/ReviewQueueFragment` | `fragment_admin_event_review_queue.xml` | `review_queueFragment` | FR-A05 |
| 8 | Chi tiết duyệt | `ui/admin/event/ReviewDetailFragment` | `fragment_admin_event_review_detail.xml` | `review_detailFragment` | FR-A06 |
| 9 | Kiểm duyệt nội dung | `ui/admin/event/ContentModerationFragment` | `fragment_admin_event_content_moderation.xml` | `content_moderationFragment` | FR-A07 |
| 10 | Giám sát sự kiện | `ui/admin/event/EventMonitorFragment` | `fragment_admin_event_monitor.xml` | `event_monitorFragment` | FR-A08 |
| 11 | Danh mục | `ui/admin/category/CategoryManageFragment` | `fragment_admin_category_manage.xml` | `category_manageFragment` | FR-A09 |
| 12 | Tra cứu giao dịch | `ui/admin/finance/TransactionLookupFragment` | `fragment_admin_finance_transaction_lookup.xml` | `transaction_lookupFragment` | FR-A10 |
| 13 | Chi tiết giao dịch | `ui/admin/finance/TransactionDetailFragment` | `fragment_admin_finance_transaction_detail.xml` | `transaction_detailFragment` | FR-A10 |
| 14 | Giám sát webhook | `ui/admin/finance/WebhookMonitorFragment` | `fragment_admin_finance_webhook_monitor.xml` | `webhook_monitorFragment` | FR-A11 |
| 15 | Quản lý hoàn tiền | `ui/admin/finance/RefundManageFragment` | `fragment_admin_finance_refund_manage.xml` | `refund_manageFragment` | FR-A12 |
| 16 | Cấu hình phí | `ui/admin/finance/FeeConfigFragment` | `fragment_admin_finance_fee_config.xml` | `fee_configFragment` | FR-A16 |
| 17 | Quản lý hỗ trợ | `ui/admin/support/SupportManageFragment` | `fragment_admin_support_manage.xml` | `support_manageFragment` | FR-A13 |
| 18 | Xử lý hỗ trợ | `ui/admin/support/SupportHandleFragment` | `fragment_admin_support_handle.xml` | `support_handleFragment` | FR-A13 |
| 19 | Gửi thông báo chung | `ui/admin/notification/BroadcastNoticeFragment` | `fragment_admin_notification_broadcast_notice.xml` | `broadcast_noticeFragment` | FR-A15 |
| 20 | Báo cáo nền tảng | `ui/admin/report/PlatformReportFragment` | `fragment_admin_report_platform_report.xml` | `platform_reportFragment` | FR-A17 |
| 21 | Nhật ký thao tác | `ui/admin/report/AuditLogFragment` | `fragment_admin_report_audit_log.xml` | `audit_logFragment` | FR-A18 |
| 22 | Phân vai trò | `ui/admin/system/RoleManageFragment` | `fragment_admin_system_role_manage.xml` | `role_manageFragment` | FR-A19 |
| 23 | Tình trạng tích hợp | `ui/admin/system/IntegrationHealthFragment` | `fragment_admin_system_integration_health.xml` | `integration_healthFragment` | FR-A20 |

## 5. Activity (5)

| Activity | Layout | Vai trò | FR |
|---|---|---|---|
| `SplashActivity` | `activity_splash.xml` | LAUNCHER, kiểm tra phiên | FR-U01, FR-U02 |
| `AuthActivity` | `activity_auth.xml` | host `nav_auth` | FR-U01, FR-U02 |
| `UserHostActivity` | `activity_user_host.xml` | host `nav_user` + menu user | FR-U03 |
| `OrganizerHostActivity` | `activity_organizer_host.xml` | host `nav_organizer` + menu organizer | FR-O02 |
| `AdminHostActivity` | `activity_admin_host.xml` | host `nav_admin` + menu admin | FR-A01 |

## 6. Đích điều hướng không nằm trên bottom navigation

Mỗi nav graph chỉ có 5 màn hình trên bottom navigation; các màn hình còn lại điều hướng bằng `action` (deep link / nút bấm), ví dụ từ `my_eventsFragment` mở `event_formFragment`, từ `event_detailFragment` tới `checkoutFragment`.
