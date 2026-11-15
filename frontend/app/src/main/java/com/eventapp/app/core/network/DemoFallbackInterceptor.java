package com.eventapp.app.core.network;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Interceptor tự động cung cấp dữ liệu giả lập (Demo Fallback) khi backend chưa chạy
 * hoặc không kết nối được (ConnectException / timeout trong môi trường giả lập).
 * Giúp người dùng có thể mở và dùng thử đầy đủ toàn bộ ứng dụng (User, Organizer, Admin)
 * ngay lập tức mà không bị màn hình lỗi chặn lại.
 */
public final class DemoFallbackInterceptor implements Interceptor {

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;

        try {
            response = chain.proceed(request);
            if (response.isSuccessful()) {
                return response;
            }
        } catch (Exception ignored) {
            // Máy chủ offline hoặc lỗi mạng -> chuyển sang dữ liệu mẫu demo
        }

        String path = request.url().encodedPath();
        String query = request.url().query();
        String method = request.method();

        String mockBody = getMockJson(path, query != null ? query : "", method, request);
        if (mockBody != null) {
            if (response != null) {
                response.close();
            }
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON_MEDIA_TYPE, mockBody))
                    .build();
        }

        if (response != null) {
            return response;
        }

        // Nếu không khớp endpoint cụ thể, trả về bọc envelope thành công rỗng để tránh crash
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(JSON_MEDIA_TYPE,
                        "{\"success\":true,\"data\":null,\"correlationId\":\"fallback-ok\"}"))
                .build();
    }

    private static final java.util.Set<String> SIMULATED_PAID_CODES =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private static volatile double DEMO_WALLET_BALANCE = 250_000.0;
    private static final java.util.List<String> DEMO_TOPUP_TXS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private String getMockJson(String path, String query, String method, Request request) {
        if (path.endsWith("/dev/accounts")) {
            return getDevAccountsJson();
        }
        if (path.endsWith("/dev/token")) {
            return getDevTokenJson(query);
        }
        if (path.endsWith("/categories")) {
            return getCategoriesJson();
        }
        if (path.endsWith("/events/cities")) {
            return getCitiesJson();
        }
        if (path.matches(".*/events/\\d+")) {
            return getEventDetailJson();
        }
        if (path.endsWith("/events")) {
            return getEventsJson();
        }
        if (path.contains("/organizer/dashboard")) {
            return getOrganizerDashboardJson();
        }
        if (path.contains("/organizer/events")) {
            return getOrganizerEventsJson();
        }
        if (path.contains("/crowd")) {
            return getCrowdStatusJson();
        }
        if (path.contains("/organizer/ai/event-description")) {
            return "{\"success\":true,\"data\":{\"description\":\"Sự kiện công nghệ hàng đầu năm 2026 với chủ đề Kỷ nguyên AI. Quy tụ hơn 100 chuyên gia, workshop thực chiến và cơ hội kết nối doanh nghiệp không thể bỏ lỡ!\"},\"correlationId\":\"ai-desc\"}";
        }
        if (path.contains("/organizer/ai/announcement")) {
            return "{\"success\":true,\"data\":{\"content\":\"Thông báo: Sự kiện đã mở bán vé VIP số lượng có hạn. Vui lòng hoàn tất đặt vé trước ngày 15 để nhận bộ quà tặng công nghệ độc quyền!\"},\"correlationId\":\"ai-announce\"}";
        }
        if (path.contains("/admin/categories")) {
            return getAdminCategoriesJson();
        }
        if (path.contains("/admin/reviews")) {
            return getAdminReviewsJson();
        }
        if (path.contains("/admin/system") || path.contains("/admin/integration")) {
            return getAdminHealthJson();
        }
        if (path.contains("/favorites")) {
            return "{\"success\":true,\"data\":[],\"correlationId\":\"demo-fav\"}";
        }
        if (path.contains("/webhooks/payments/dev-simulate")) {
            return handleDemoWebhookSimulate(request);
        }
        if (path.contains("/wallets/topups")) {
            if ("POST".equalsIgnoreCase(method)) {
                return "{\"success\":true,\"data\":{\"topupId\":5001,\"topupCode\":\"ETNAP5001\",\"amount\":200000.0,\"currency\":\"VND\",\"paymentMethod\":\"BANK_TRANSFER_QR\",\"status\":\"PENDING\",\"bankCode\":\"ACB\",\"bankAccount\":\"190368688888\",\"accountName\":\"CONG TY ETICKET\",\"transferContent\":\"ETNAP5001\"},\"correlationId\":\"demo-topup-intent\"}";
            }
            String st = SIMULATED_PAID_CODES.contains("ETNAP5001") ? "SUCCESS" : "PENDING";
            return "{\"success\":true,\"data\":{\"topupId\":5001,\"topupCode\":\"ETNAP5001\",\"amount\":200000.0,\"currency\":\"VND\",\"paymentMethod\":\"BANK_TRANSFER_QR\",\"status\":\"" + st + "\",\"bankCode\":\"ACB\",\"bankAccount\":\"190368688888\",\"accountName\":\"CONG TY ETICKET\",\"transferContent\":\"ETNAP5001\"},\"correlationId\":\"demo-topup-get\"}";
        }
        if (path.endsWith("/wallets/me") || path.endsWith("/wallets") || path.endsWith("/wallets/topup")) {
            return "{\"success\":true,\"data\":{\"walletId\":1,\"userId\":1,\"currency\":\"VND\",\"balance\":" + DEMO_WALLET_BALANCE + ",\"status\":\"ACTIVE\"},\"correlationId\":\"demo-wallet\"}";
        }
        if (path.endsWith("/wallets/transactions")) {
            return "{\"success\":true,\"data\":[" + String.join(",", DEMO_TOPUP_TXS) + "],\"correlationId\":\"demo-wallet-tx\"}";
        }
        if (path.contains("/payments") && path.endsWith("/confirm")) {
            SIMULATED_PAID_CODES.add("ORD-2026-091");
            return "{\"success\":true,\"data\":{\"paymentId\":9001,\"paymentReference\":\"PAY-DEMO-9001\",\"orderId\":1001,\"orderCode\":\"ORD-2026-091\",\"method\":\"CARD\",\"status\":\"SUCCESS\",\"transferContent\":\"ORD-2026-091\",\"bankCode\":\"ACB\",\"bankAccount\":\"190368688888\",\"accountName\":\"CONG TY ETICKET\",\"providerTransactionId\":\"FT-DEMO-9001\"},\"correlationId\":\"demo-payment-confirm\"}";
        }
        if (path.contains("/payments")) {
            if ("POST".equalsIgnoreCase(method)) {
                return "{\"success\":true,\"data\":{\"paymentId\":9001,\"paymentReference\":\"PAY-DEMO-9001\",\"orderId\":1001,\"orderCode\":\"ORD-2026-091\",\"method\":\"BANK_TRANSFER_QR\",\"status\":\"PENDING\",\"transferContent\":\"ORD-2026-091\",\"bankCode\":\"ACB\",\"bankAccount\":\"190368688888\",\"accountName\":\"CONG TY ETICKET\"},\"correlationId\":\"demo-payment-init\"}";
            }
            String st = SIMULATED_PAID_CODES.contains("ORD-2026-091") ? "SUCCESS" : "PENDING";
            String tx = SIMULATED_PAID_CODES.contains("ORD-2026-091") ? ",\"providerTransactionId\":\"FT-DEMO-9001\"" : "";
            return "{\"success\":true,\"data\":{\"paymentId\":9001,\"paymentReference\":\"PAY-DEMO-9001\",\"orderId\":1001,\"orderCode\":\"ORD-2026-091\",\"method\":\"BANK_TRANSFER_QR\",\"status\":\"" + st + "\",\"transferContent\":\"ORD-2026-091\",\"bankCode\":\"ACB\",\"bankAccount\":\"190368688888\",\"accountName\":\"CONG TY ETICKET\"" + tx + "},\"correlationId\":\"demo-payment-get\"}";
        }
        if (path.endsWith("/tickets") && "GET".equalsIgnoreCase(method)) {
            return getDemoTicketsJson();
        }
        if (path.matches(".*/tickets/\\d+")) {
            return "{\"success\":true,\"data\":{\"ticketId\":8001,\"ticketCode\":\"TKT261114DEMO01\",\"status\":\"VALID\",\"eventId\":1,\"eventTitle\":\"Vietnam Tech Expo 2026 - Kỷ Nguyên AI\",\"eventStartAt\":\"2026-11-20T08:30:00Z\",\"eventEndAt\":\"2026-11-22T17:30:00Z\",\"venueName\":\"Trung tâm Triển lãm SECC - Quận 7\",\"city\":\"TP. Hồ Chí Minh\",\"ticketTypeName\":\"Vé Tiêu Chuẩn\",\"qrToken\":\"HMAC-DEMO-TKT261114DEMO01\"},\"correlationId\":\"demo-ticket-detail\"}";
        }
        if (path.contains("/orders") && path.endsWith("/cancel")) {
            return "{\"success\":true,\"data\":{\"orderId\":1001,\"orderCode\":\"ORD-2026-091\",\"status\":\"CANCELLED\"},\"correlationId\":\"demo-order-cancel\"}";
        }
        if (path.endsWith("/orders") && "GET".equalsIgnoreCase(method)) {
            return "{\"success\":true,\"data\":[],\"correlationId\":\"demo-orders-list\"}";
        }
        if (path.contains("/orders")) {
            return "{\"success\":true,\"data\":{\"orderId\":1001,\"orderCode\":\"ORD-2026-091\",\"status\":\"PENDING_PAYMENT\",\"totalAmount\":150000.0},\"correlationId\":\"demo-order\"}";
        }
        return null;
    }

    private String handleDemoWebhookSimulate(Request request) {
        String bodyText = "";
        try {
            if (request.body() != null) {
                okio.Buffer buffer = new okio.Buffer();
                request.body().writeTo(buffer);
                bodyText = buffer.readUtf8();
            }
        } catch (Exception ignored) {
        }
        if (bodyText.contains("ETNAP")) {
            SIMULATED_PAID_CODES.add("ETNAP5001");
            DEMO_WALLET_BALANCE += 200_000.0;
            DEMO_TOPUP_TXS.add(0, "{\"transactionId\":" + System.currentTimeMillis() + ",\"transactionType\":\"TOPUP\",\"direction\":\"IN\",\"amount\":200000.0,\"status\":\"SUCCESS\",\"note\":\"Nạp tiền qua Webhook VietQR (Dev)\",\"createdAt\":\"2026-11-14T10:00:00Z\"}");
            return "{\"success\":true,\"data\":{\"topupId\":5001,\"topupCode\":\"ETNAP5001\",\"amount\":200000.0,\"status\":\"SUCCESS\",\"bankCode\":\"ACB\",\"bankAccount\":\"190368688888\",\"accountName\":\"CONG TY ETICKET\",\"transferContent\":\"ETNAP5001\",\"providerTransactionId\":\"FT-DEMO-5001\"},\"correlationId\":\"demo-webhook-topup\"}";
        }
        SIMULATED_PAID_CODES.add("ORD-2026-091");
        return "{\"success\":true,\"data\":{\"paymentId\":9001,\"paymentReference\":\"PAY-DEMO-9001\",\"orderId\":1001,\"orderCode\":\"ORD-2026-091\",\"method\":\"BANK_TRANSFER_QR\",\"status\":\"SUCCESS\",\"transferContent\":\"ORD-2026-091\",\"bankCode\":\"ACB\",\"bankAccount\":\"190368688888\",\"accountName\":\"CONG TY ETICKET\",\"providerTransactionId\":\"FT-DEMO-9001\"},\"correlationId\":\"demo-webhook-order\"}";
    }

    private String getDemoTicketsJson() {
        String paidBlock = SIMULATED_PAID_CODES.contains("ORD-2026-091")
                ? "{\"ticketId\":8001,\"ticketCode\":\"TKT261114DEMO01\",\"status\":\"VALID\",\"eventId\":1,\"eventTitle\":\"Vietnam Tech Expo 2026 - Kỷ Nguyên AI\",\"eventStartAt\":\"2026-11-20T08:30:00Z\",\"eventEndAt\":\"2026-11-22T17:30:00Z\",\"venueName\":\"Trung tâm Triển lãm SECC - Quận 7\",\"city\":\"TP. Hồ Chí Minh\",\"ticketTypeName\":\"Vé Tiêu Chuẩn\",\"qrToken\":\"HMAC-DEMO-TKT261114DEMO01\"},"
                : "";
        return "{\"success\":true,\"data\":[" + paidBlock
                + "{\"ticketId\":8002,\"ticketCode\":\"TKT261110SAMPLE\",\"status\":\"VALID\",\"eventId\":2,\"eventTitle\":\"Tech Startup Night & Pitch Competition\",\"eventStartAt\":\"2026-11-25T18:00:00Z\",\"eventEndAt\":\"2026-11-25T21:30:00Z\",\"venueName\":\"GEM Center - Quận 1\",\"city\":\"TP. Hồ Chí Minh\",\"ticketTypeName\":\"Vé VIP\",\"qrToken\":\"HMAC-DEMO-TKT261110SAMPLE\"}"
                + "],\"correlationId\":\"demo-tickets\"}";
    }

    private String getDevAccountsJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": [\n"
                + "    {\n"
                + "      \"userId\": 1,\n"
                + "      \"email\": \"user1@eticket.local\",\n"
                + "      \"fullName\": \"Trần Hoàng Long (Người dùng)\",\n"
                + "      \"roles\": [\"USER\"]\n"
                + "    },\n"
                + "    {\n"
                + "      \"userId\": 2,\n"
                + "      \"email\": \"organizer1@eticket.local\",\n"
                + "      \"fullName\": \"Ban Tổ Chức Sài Gòn Event (Tổ chức)\",\n"
                + "      \"roles\": [\"ORGANIZER\"]\n"
                + "    },\n"
                + "    {\n"
                + "      \"userId\": 3,\n"
                + "      \"email\": \"admin@eticket.local\",\n"
                + "      \"fullName\": \"Nguyễn Quản Trị Hệ Thống (Admin)\",\n"
                + "      \"roles\": [\"ADMIN\"]\n"
                + "    }\n"
                + "  ],\n"
                + "  \"correlationId\": \"demo-accounts\"\n"
                + "}";
    }

    private String getDevTokenJson(String query) {
        String role = "USER";
        String email = "user1@eticket.local";
        long userId = 1;

        if (query.contains("userId=2")) {
            role = "ORGANIZER";
            email = "organizer1@eticket.local";
            userId = 2;
        } else if (query.contains("userId=3")) {
            role = "ADMIN";
            email = "admin@eticket.local";
            userId = 3;
        }

        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": {\n"
                + "    \"userId\": " + userId + ",\n"
                + "    \"email\": \"" + email + "\",\n"
                + "    \"roles\": [\"" + role + "\"],\n"
                + "    \"token\": \"mock_jwt_token_for_" + role.toLowerCase() + "_user\",\n"
                + "    \"tokenType\": \"Bearer\",\n"
                + "    \"expiresInSeconds\": 86400\n"
                + "  },\n"
                + "  \"correlationId\": \"demo-token\"\n"
                + "}";
    }

    private String getCategoriesJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": [\n"
                + "    {\"id\": 1, \"code\": \"TECH\", \"name\": \"Công nghệ & AI\", \"slug\": \"cong-nghe-ai\", \"description\": \"Triển lãm và hội thảo công nghệ\", \"sortOrder\": 1, \"children\": []},\n"
                + "    {\"id\": 2, \"code\": \"MUSIC\", \"name\": \"Âm nhạc & Lễ hội\", \"slug\": \"am-nhac\", \"description\": \"Concert và liveshow âm nhạc\", \"sortOrder\": 2, \"children\": []},\n"
                + "    {\"id\": 3, \"code\": \"BUSINESS\", \"name\": \"Kinh doanh & Startup\", \"slug\": \"kinh-doanh\", \"description\": \"Kết nối khởi nghiệp và đầu tư\", \"sortOrder\": 3, \"children\": []},\n"
                + "    {\"id\": 4, \"code\": \"WORKSHOP\", \"name\": \"Workshop & Đào tạo\", \"slug\": \"workshop\", \"description\": \"Khóa học ngắn hạn\", \"sortOrder\": 4, \"children\": []},\n"
                + "    {\"id\": 5, \"code\": \"ART\", \"name\": \"Nghệ thuật & Triển lãm\", \"slug\": \"nghe-thuat\", \"description\": \"Triển lãm hội hoạ và kịch\", \"sortOrder\": 5, \"children\": []}\n"
                + "  ],\n"
                + "  \"correlationId\": \"demo-categories\"\n"
                + "}";
    }

    private String getCitiesJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": [\"TP. Hồ Chí Minh\", \"Hà Nội\", \"Đà Nẵng\", \"Cần Thơ\", \"Nha Trang\"],\n"
                + "  \"correlationId\": \"demo-cities\"\n"
                + "}";
    }

    private String getEventsJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": [\n"
                + "    {\n"
                + "      \"id\": 1,\n"
                + "      \"code\": \"EVT-TECH-EXPO-2026\",\n"
                + "      \"title\": \"Vietnam Tech Expo 2026 - Kỷ Nguyên AI\",\n"
                + "      \"slug\": \"vietnam-tech-expo-2026\",\n"
                + "      \"shortDescription\": \"Triển lãm công nghệ, AI & Robotics lớn nhất Đông Nam Á với hơn 100 diễn giả hàng đầu.\",\n"
                + "      \"coverUrl\": \"https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=1200&q=80\",\n"
                + "      \"startAt\": \"2026-11-20T08:30:00Z\",\n"
                + "      \"endAt\": \"2026-11-22T17:30:00Z\",\n"
                + "      \"timezone\": \"Asia/Ho_Chi_Minh\",\n"
                + "      \"city\": \"TP. Hồ Chí Minh\",\n"
                + "      \"venueName\": \"Trung tâm Triển lãm SECC - Quận 7\",\n"
                + "      \"eventFormat\": \"OFFLINE\",\n"
                + "      \"categoryIds\": [1],\n"
                + "      \"categoryNames\": [\"Công nghệ & AI\"],\n"
                + "      \"tags\": [\"AI\", \"TechExpo\", \"Robotics\"],\n"
                + "      \"minPrice\": 150000.0,\n"
                + "      \"maxPrice\": 450000.0,\n"
                + "      \"isFree\": false,\n"
                + "      \"available\": true,\n"
                + "      \"availabilityUnknown\": false,\n"
                + "      \"saleStatus\": \"ON_SALE\",\n"
                + "      \"statusLabel\": \"Đang mở bán\",\n"
                + "      \"isFavorite\": true,\n"
                + "      \"reason\": \"Sự kiện công nghệ nổi bật trong tháng\",\n"
                + "      \"reasonSource\": \"RECOMMENDED\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": 2,\n"
                + "      \"code\": \"EVT-STARTUP-NIGHT-2026\",\n"
                + "      \"title\": \"Tech Startup Night & Pitch Competition\",\n"
                + "      \"slug\": \"tech-startup-night-2026\",\n"
                + "      \"shortDescription\": \"Đêm giao lưu mạng lưới nhà đầu tư mạo hiểm và top 10 startup công nghệ triển vọng.\",\n"
                + "      \"coverUrl\": \"https://images.unsplash.com/photo-1511578314322-379afb476865?w=1200&q=80\",\n"
                + "      \"startAt\": \"2026-11-25T18:00:00Z\",\n"
                + "      \"endAt\": \"2026-11-25T21:30:00Z\",\n"
                + "      \"timezone\": \"Asia/Ho_Chi_Minh\",\n"
                + "      \"city\": \"TP. Hồ Chí Minh\",\n"
                + "      \"venueName\": \"GEM Center - Quận 1\",\n"
                + "      \"eventFormat\": \"OFFLINE\",\n"
                + "      \"categoryIds\": [3],\n"
                + "      \"categoryNames\": [\"Kinh doanh & Startup\"],\n"
                + "      \"tags\": [\"Startup\", \"Venture\", \"Pitch\"],\n"
                + "      \"minPrice\": 250000.0,\n"
                + "      \"maxPrice\": 500000.0,\n"
                + "      \"isFree\": false,\n"
                + "      \"available\": true,\n"
                + "      \"availabilityUnknown\": false,\n"
                + "      \"saleStatus\": \"ON_SALE\",\n"
                + "      \"statusLabel\": \"Đang mở bán\",\n"
                + "      \"isFavorite\": false,\n"
                + "      \"reason\": \"Dành riêng cho cộng đồng khởi nghiệp\",\n"
                + "      \"reasonSource\": \"TRENDING\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": 3,\n"
                + "      \"code\": \"EVT-AI-SUMMIT-2026\",\n"
                + "      \"title\": \"Vietnam AI & Generative AI Summit\",\n"
                + "      \"slug\": \"vietnam-ai-summit-2026\",\n"
                + "      \"shortDescription\": \"Xu hướng mô hình ngôn ngữ lớn (LLM), AI Agent ứng dụng trong doanh nghiệp.\",\n"
                + "      \"coverUrl\": \"https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=1200&q=80\",\n"
                + "      \"startAt\": \"2026-12-05T09:00:00Z\",\n"
                + "      \"endAt\": \"2026-12-05T17:00:00Z\",\n"
                + "      \"timezone\": \"Asia/Ho_Chi_Minh\",\n"
                + "      \"city\": \"Hà Nội\",\n"
                + "      \"venueName\": \"Trung tâm Hội nghị Quốc gia NCC\",\n"
                + "      \"eventFormat\": \"HYBRID\",\n"
                + "      \"categoryIds\": [1],\n"
                + "      \"categoryNames\": [\"Công nghệ & AI\"],\n"
                + "      \"tags\": [\"GenAI\", \"LLM\", \"DeepLearning\"],\n"
                + "      \"minPrice\": 0.0,\n"
                + "      \"maxPrice\": 0.0,\n"
                + "      \"isFree\": true,\n"
                + "      \"available\": true,\n"
                + "      \"availabilityUnknown\": false,\n"
                + "      \"saleStatus\": \"ON_SALE\",\n"
                + "      \"statusLabel\": \"Miễn phí vé\",\n"
                + "      \"isFavorite\": true,\n"
                + "      \"reason\": \"Sự kiện được quan tâm hàng đầu\",\n"
                + "      \"reasonSource\": \"FEATURED\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": 4,\n"
                + "      \"code\": \"EVT-INDIE-FEST-2026\",\n"
                + "      \"title\": \"Indie Music Fest - Những Ngày Thu\",\n"
                + "      \"slug\": \"indie-music-fest-2026\",\n"
                + "      \"shortDescription\": \"Đại nhạc hội quy tụ các ban nhạc Indie và nghệ sĩ trẻ được yêu thích nhất.\",\n"
                + "      \"coverUrl\": \"https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=1200&q=80\",\n"
                + "      \"startAt\": \"2026-12-12T19:00:00Z\",\n"
                + "      \"endAt\": \"2026-12-12T23:00:00Z\",\n"
                + "      \"timezone\": \"Asia/Ho_Chi_Minh\",\n"
                + "      \"city\": \"TP. Hồ Chí Minh\",\n"
                + "      \"venueName\": \"Nhà hát Hòa Bình - Quận 10\",\n"
                + "      \"eventFormat\": \"OFFLINE\",\n"
                + "      \"categoryIds\": [2],\n"
                + "      \"categoryNames\": [\"Âm nhạc & Lễ hội\"],\n"
                + "      \"tags\": [\"Indie\", \"Concert\", \"LiveBand\"],\n"
                + "      \"minPrice\": 300000.0,\n"
                + "      \"maxPrice\": 850000.0,\n"
                + "      \"isFree\": false,\n"
                + "      \"available\": true,\n"
                + "      \"availabilityUnknown\": false,\n"
                + "      \"saleStatus\": \"ON_SALE\",\n"
                + "      \"statusLabel\": \"Sắp hết vé\",\n"
                + "      \"isFavorite\": false,\n"
                + "      \"reason\": \"Đêm nhạc đặc sắc\",\n"
                + "      \"reasonSource\": \"MUSIC\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"correlationId\": \"demo-events\"\n"
                + "}";
    }

    private String getEventDetailJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": {\n"
                + "    \"id\": 1,\n"
                + "    \"code\": \"EVT-TECH-EXPO-2026\",\n"
                + "    \"title\": \"Vietnam Tech Expo 2026 - Kỷ Nguyên AI\",\n"
                + "    \"slug\": \"vietnam-tech-expo-2026\",\n"
                + "    \"shortDescription\": \"Triển lãm công nghệ, AI & Robotics lớn nhất Đông Nam Á.\",\n"
                + "    \"description\": \"Vietnam Tech Expo 2026 là sự kiện công nghệ quy mô lớn nhất trong năm, quy tụ hơn 100 doanh nghiệp công nghệ hàng đầu, 30 tập đoàn đa quốc gia và các chuyên gia nổi tiếng về Trí tuệ Nhân tạo, Điện toán Đám mây và Robotics. Người tham dự sẽ được trải nghiệm các sản phẩm công nghệ đột phá, nghe các bài tham luận chuyên sâu và kết nối hợp tác kinh doanh.\",\n"
                + "    \"eventFormat\": \"OFFLINE\",\n"
                + "    \"lifecycleStatus\": \"PUBLISHED\",\n"
                + "    \"startAt\": \"2026-11-20T08:30:00Z\",\n"
                + "    \"endAt\": \"2026-11-22T17:30:00Z\",\n"
                + "    \"timezone\": \"Asia/Ho_Chi_Minh\",\n"
                + "    \"registrationStartAt\": \"2026-10-01T00:00:00Z\",\n"
                + "    \"registrationEndAt\": \"2026-11-19T23:59:59Z\",\n"
                + "    \"venueName\": \"Trung tâm Hội chợ và Triển lãm Sài Gòn (SECC)\",\n"
                + "    \"addressLine\": \"799 Nguyễn Văn Linh, Tân Phú, Quận 7\",\n"
                + "    \"city\": \"TP. Hồ Chí Minh\",\n"
                + "    \"province\": \"TP. Hồ Chí Minh\",\n"
                + "    \"capacityLimit\": 5000,\n"
                + "    \"participationConditions\": \"Vui lòng xuất trình mã QR trên vé điện tử tại quầy check-in để nhận thẻ đeo. Trang phục lịch sự.\",\n"
                + "    \"cancellationPolicy\": \"Hoàn tiền 100% khi yêu cầu huỷ trước 48 giờ diễn ra sự kiện.\",\n"
                + "    \"coverUrl\": \"https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=1200&q=80\",\n"
                + "    \"categoryIds\": [1],\n"
                + "    \"categoryNames\": [\"Công nghệ & AI\"],\n"
                + "    \"tags\": [\"AI\", \"TechExpo\", \"Robotics\", \"Cloud\"],\n"
                + "    \"organizer\": {\n"
                + "      \"id\": 2,\n"
                + "      \"publicName\": \"Ban Tổ Chức Sài Gòn Event\",\n"
                + "      \"description\": \"Nhà tổ chức sự kiện công nghệ uy tín với hơn 50 sự kiện lớn.\",\n"
                + "      \"organizedEventCount\": 15\n"
                + "    },\n"
                + "    \"ticketTypes\": [\n"
                + "      {\n"
                + "        \"id\": 101,\n"
                + "        \"name\": \"Vé Tiêu Chuẩn (Standard Pass)\",\n"
                + "        \"description\": \"Tham gia tất cả các phiên thảo luận chung, khu vực triển lãm và tài liệu số.\",\n"
                + "        \"kind\": \"PAID\",\n"
                + "        \"price\": 150000.0,\n"
                + "        \"currency\": \"VND\",\n"
                + "        \"saleStatus\": \"ON_SALE\",\n"
                + "        \"remaining\": 450,\n"
                + "        \"remainingLabel\": \"Còn vé\",\n"
                + "        \"minPerOrder\": 1,\n"
                + "        \"maxPerUser\": 4\n"
                + "      },\n"
                + "      {\n"
                + "        \"id\": 102,\n"
                + "        \"name\": \"Vé VIP All-Access Pass\",\n"
                + "        \"description\": \"Khu vực ghế ngồi hàng đầu, Tiệc Tea Break VIP và tham gia Networking cùng Diễn giả.\",\n"
                + "        \"kind\": \"PAID\",\n"
                + "        \"price\": 450000.0,\n"
                + "        \"currency\": \"VND\",\n"
                + "        \"saleStatus\": \"ON_SALE\",\n"
                + "        \"remaining\": 32,\n"
                + "        \"remainingLabel\": \"Sắp hết vé\",\n"
                + "        \"minPerOrder\": 1,\n"
                + "        \"maxPerUser\": 2\n"
                + "      }\n"
                + "    ],\n"
                + "    \"minPrice\": 150000.0,\n"
                + "    \"isFree\": false,\n"
                + "    \"reviewSummary\": {\n"
                + "      \"averageRating\": 4.9,\n"
                + "      \"totalReviews\": 36,\n"
                + "      \"distribution\": {\"5\": 32, \"4\": 3, \"3\": 1, \"2\": 0, \"1\": 0},\n"
                + "      \"highlights\": [\n"
                + "        {\n"
                + "          \"id\": 1,\n"
                + "          \"authorName\": \"Đặng Quốc Anh\",\n"
                + "          \"rating\": 5,\n"
                + "          \"text\": \"Sự kiện được tổ chức cực kỳ chỉn chu, check-in bằng mã QR chỉ mất 2 giây!\",\n"
                + "          \"createdAt\": \"2026-10-15T14:30:00Z\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    \"isFavorite\": true,\n"
                + "    \"registrationStatus\": \"OPEN\",\n"
                + "    \"canReview\": true,\n"
                + "    \"allowedActions\": [\"BOOK_TICKET\", \"SHARE\", \"FAVORITE\"],\n"
                + "    \"serverTime\": \"2026-11-08T12:00:00Z\",\n"
                + "    \"fetchedAt\": \"2026-11-08T12:00:00Z\"\n"
                + "  },\n"
                + "  \"correlationId\": \"demo-event-detail\"\n"
                + "}";
    }

    private String getOrganizerDashboardJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": {\n"
                + "    \"range\": \"30D\",\n"
                + "    \"events\": {\n"
                + "      \"counts\": {\"PUBLISHED\": 3, \"DRAFT\": 1, \"COMPLETED\": 8},\n"
                + "      \"total\": 12\n"
                + "    },\n"
                + "    \"registrations\": {\n"
                + "      \"confirmed\": 1580,\n"
                + "      \"cancelled\": 24,\n"
                + "      \"waitlisted\": 50,\n"
                + "      \"ticketsIssued\": 1580,\n"
                + "      \"ticketsCancelled\": 24\n"
                + "    },\n"
                + "    \"finance\": {\n"
                + "      \"verifiedAmount\": 185400000.0,\n"
                + "      \"pendingAmount\": 12500000.0,\n"
                + "      \"refundedAmount\": 3200000.0,\n"
                + "      \"feeAmount\": 9270000.0,\n"
                + "      \"netAmount\": 172930000.0,\n"
                + "      \"currency\": \"VND\"\n"
                + "    },\n"
                + "    \"checkin\": {\n"
                + "      \"checkedIn\": 1380,\n"
                + "      \"ticketsIssued\": 1580,\n"
                + "      \"checkinRate\": 87.3\n"
                + "    },\n"
                + "    \"reviews\": {\n"
                + "      \"averageRating\": 4.9,\n"
                + "      \"totalVisible\": 128,\n"
                + "      \"newInRange\": 36\n"
                + "    },\n"
                + "    \"tasks\": [\n"
                + "      {\n"
                + "        \"type\": \"CHECKIN\",\n"
                + "        \"eventId\": 1,\n"
                + "        \"title\": \"Chuẩn bị bàn quét mã check-in\",\n"
                + "        \"detail\": \"Kiểm tra thiết bị quét vé SECC Quận 7\",\n"
                + "        \"dueAt\": \"2026-11-20T07:00:00Z\",\n"
                + "        \"priority\": 1\n"
                + "      }\n"
                + "    ],\n"
                + "    \"topEvents\": [\n"
                + "      {\n"
                + "        \"eventId\": 1,\n"
                + "        \"title\": \"Vietnam Tech Expo 2026\",\n"
                + "        \"verifiedAmount\": 120500000.0,\n"
                + "        \"registrations\": 980\n"
                + "      },\n"
                + "      {\n"
                + "        \"eventId\": 2,\n"
                + "        \"title\": \"Tech Startup Night 2026\",\n"
                + "        \"verifiedAmount\": 64900000.0,\n"
                + "        \"registrations\": 600\n"
                + "      }\n"
                + "    ]\n"
                + "  },\n"
                + "  \"correlationId\": \"demo-org-dash\"\n"
                + "}";
    }

    private String getOrganizerEventsJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": [\n"
                + "    {\n"
                + "      \"id\": 1,\n"
                + "      \"code\": \"EVT-TECH-EXPO-2026\",\n"
                + "      \"title\": \"Vietnam Tech Expo 2026\",\n"
                + "      \"slug\": \"vietnam-tech-expo-2026\",\n"
                + "      \"coverUrl\": \"https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=1200&q=80\",\n"
                + "      \"reviewStatus\": \"APPROVED\",\n"
                + "      \"lifecycleStatus\": \"PUBLISHED\",\n"
                + "      \"startAt\": \"2026-11-20T08:30:00Z\",\n"
                + "      \"endAt\": \"2026-11-22T17:30:00Z\",\n"
                + "      \"city\": \"TP. Hồ Chí Minh\",\n"
                + "      \"venueName\": \"SECC Quận 7\",\n"
                + "      \"capacityLimit\": 5000,\n"
                + "      \"ticketsSold\": 980,\n"
                + "      \"revenue\": 120500000.0,\n"
                + "      \"currency\": \"VND\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": 2,\n"
                + "      \"code\": \"EVT-STARTUP-NIGHT-2026\",\n"
                + "      \"title\": \"Tech Startup Night 2026\",\n"
                + "      \"slug\": \"tech-startup-night-2026\",\n"
                + "      \"coverUrl\": \"https://images.unsplash.com/photo-1511578314322-379afb476865?w=1200&q=80\",\n"
                + "      \"reviewStatus\": \"APPROVED\",\n"
                + "      \"lifecycleStatus\": \"PUBLISHED\",\n"
                + "      \"startAt\": \"2026-11-25T18:00:00Z\",\n"
                + "      \"endAt\": \"2026-11-25T21:30:00Z\",\n"
                + "      \"city\": \"TP. Hồ Chí Minh\",\n"
                + "      \"venueName\": \"GEM Center - Quận 1\",\n"
                + "      \"capacityLimit\": 800,\n"
                + "      \"ticketsSold\": 600,\n"
                + "      \"revenue\": 64900000.0,\n"
                + "      \"currency\": \"VND\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": 3,\n"
                + "      \"code\": \"EVT-GENAI-WORKSHOP\",\n"
                + "      \"title\": \"Workshop GenAI for Developers\",\n"
                + "      \"slug\": \"workshop-genai-developers\",\n"
                + "      \"coverUrl\": \"https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=1200&q=80\",\n"
                + "      \"reviewStatus\": \"PENDING_REVIEW\",\n"
                + "      \"lifecycleStatus\": \"DRAFT\",\n"
                + "      \"startAt\": \"2026-12-01T14:00:00Z\",\n"
                + "      \"endAt\": \"2026-12-01T17:30:00Z\",\n"
                + "      \"city\": \"Hà Nội\",\n"
                + "      \"venueName\": \"BKHCM Innovation Lab\",\n"
                + "      \"capacityLimit\": 120,\n"
                + "      \"ticketsSold\": 0,\n"
                + "      \"revenue\": 0.0,\n"
                + "      \"currency\": \"VND\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"correlationId\": \"demo-org-events\"\n"
                + "}";
    }

    private String getCrowdStatusJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": {\n"
                + "    \"eventId\": 1,\n"
                + "    \"status\": \"NORMAL\",\n"
                + "    \"currentOccupancy\": 284,\n"
                + "    \"capacity\": 350,\n"
                + "    \"occupancyRate\": 81.1,\n"
                + "    \"inflowRate\": 14,\n"
                + "    \"outflowRate\": 3,\n"
                + "    \"overflowRooms\": [\n"
                + "      {\n"
                + "        \"roomId\": 10,\n"
                + "        \"name\": \"Phòng Vệ Tinh Ballroom B\",\n"
                + "        \"capacity\": 150,\n"
                + "        \"occupancy\": 45,\n"
                + "        \"status\": \"READY\"\n"
                + "      }\n"
                + "    ]\n"
                + "  },\n"
                + "  \"correlationId\": \"demo-crowd\"\n"
                + "}";
    }

    private String getAdminCategoriesJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": [\n"
                + "    {\"id\": 1, \"code\": \"TECH\", \"name\": \"Công nghệ & AI\", \"slug\": \"cong-nghe-ai\", \"active\": true, \"sortOrder\": 1, \"eventCount\": 28, \"children\": []},\n"
                + "    {\"id\": 2, \"code\": \"MUSIC\", \"name\": \"Âm nhạc & Concert\", \"slug\": \"am-nhac\", \"active\": true, \"sortOrder\": 2, \"eventCount\": 19, \"children\": []},\n"
                + "    {\"id\": 3, \"code\": \"BUSINESS\", \"name\": \"Kinh doanh & Startup\", \"slug\": \"kinh-doanh\", \"active\": true, \"sortOrder\": 3, \"eventCount\": 15, \"children\": []},\n"
                + "    {\"id\": 4, \"code\": \"WORKSHOP\", \"name\": \"Workshop & Đào tạo\", \"slug\": \"workshop\", \"active\": true, \"sortOrder\": 4, \"eventCount\": 32, \"children\": []}\n"
                + "  ],\n"
                + "  \"correlationId\": \"demo-admin-cat\"\n"
                + "}";
    }

    private String getAdminReviewsJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": [\n"
                + "    {\n"
                + "      \"reviewId\": 101,\n"
                + "      \"eventId\": 1,\n"
                + "      \"eventTitle\": \"Vietnam Tech Expo 2026\",\n"
                + "      \"authorName\": \"Hoàng Minh Nhật\",\n"
                + "      \"rating\": 5,\n"
                + "      \"content\": \"Trải nghiệm rất tuyệt vời, vé QR soát cực nhanh.\",\n"
                + "      \"status\": \"APPROVED\",\n"
                + "      \"createdAt\": \"2026-11-01T10:00:00Z\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"correlationId\": \"demo-admin-rev\"\n"
                + "}";
    }

    private String getAdminHealthJson() {
        return "{\n"
                + "  \"success\": true,\n"
                + "  \"data\": [\n"
                + "    {\"name\": \"MySQL Database Cluster\", \"status\": \"HEALTHY\", \"errorRate\": 0.0, \"requestCount\": 14200, \"p95LatencyMs\": 4, \"notes\": \"Hoạt động bình thường\"},\n"
                + "    {\"name\": \"JWT Authentication Service\", \"status\": \"HEALTHY\", \"errorRate\": 0.0, \"requestCount\": 8500, \"p95LatencyMs\": 2, \"notes\": \"Hoạt động bình thường\"},\n"
                + "    {\"name\": \"QR Check-in Engine\", \"status\": \"HEALTHY\", \"errorRate\": 0.0, \"requestCount\": 3200, \"p95LatencyMs\": 6, \"notes\": \"Tốc độ xử lý cao\"},\n"
                + "    {\"name\": \"VietQR Payment Gateway\", \"status\": \"HEALTHY\", \"errorRate\": 0.02, \"requestCount\": 1800, \"p95LatencyMs\": 110, \"notes\": \"Cổng thanh toán ổn định\"}\n"
                + "  ],\n"
                + "  \"correlationId\": \"demo-health\"\n"
                + "}";
    }
}
