package com.eventapp.app.ui.organizer.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FR-O06 sample payloads. Two parts matter for the screen:
 *
 * <ul>
 *   <li>{@link #create} builds variants whose wording is assembled from the supplied form data
 *       only — no invented price, date or venue, so the post-check has something honest to
 *       verify against.</li>
 *   <li>{@link #postCheck} is the O06-R03 pass: any number, date, phone number or link in the
 *       output that is absent from the input is reported as a warning.</li>
 * </ul>
 */
public final class SampleAiDescriptionData {

    private static final Pattern DATE_PATTERN =
            Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}([/-]\\d{2,4})?\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b0\\d{8,10}\\b");
    private static final Pattern LINK_PATTERN =
            Pattern.compile("https?://\\S+|www\\.\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?");

    /** Local sample of refused topics; the server owns the authoritative list. */
    private static final String[] BLOCKED_TERMS = {
            "lừa đảo", "cá độ", "chất cấm", "18+", "hack", "crack", "keygen"
    };

    private SampleAiDescriptionData() {
    }

    /** True when the form content contains a term the assistant must refuse (O06 exception). */
    public static boolean isBlocked(String title, List<String> keyPoints) {
        String haystack = (title == null ? "" : title).toLowerCase(Locale.ROOT);
        if (keyPoints != null) {
            for (String point : keyPoints) {
                haystack += " " + (point == null ? "" : point).toLowerCase(Locale.ROOT);
            }
        }
        for (String term : BLOCKED_TERMS) {
            if (haystack.contains(term)) {
                return true;
            }
        }
        return false;
    }

    public static AiDescriptionUiModel.Result create(AiDescriptionUiModel.Tone tone,
                                                     AiDescriptionUiModel.Length length,
                                                     int variants, long categoryId,
                                                     String title, String format,
                                                     String audience,
                                                     List<String> keyPoints) {
        String eventTitle = title == null ? "" : title.trim();
        String eventFormat = format == null ? "" : format.trim();
        String audienceText = audience == null ? "" : audience.trim();
        List<String> points = keyPoints == null ? new ArrayList<>() : keyPoints;

        List<String> missing = missingFields(eventFormat, audienceText);

        List<AiDescriptionUiModel.Variant> built = new ArrayList<>();
        int count = Math.max(1, Math.min(3, variants));
        for (int i = 0; i < count; i++) {
            String text = body(tone, length, i, eventTitle, eventFormat, audienceText, points);
            built.add(new AiDescriptionUiModel.Variant(text, summarize(text)));
        }

        String input = inputSnapshot(eventTitle, eventFormat, audienceText, points);
        List<AiDescriptionUiModel.Warning> warnings =
                postCheck(String.join("\n", toTexts(built)), input);

        return new AiDescriptionUiModel.Result(
                "ai_req_" + Long.toHexString(System.currentTimeMillis()),
                System.currentTimeMillis(),
                built,
                missing,
                warnings);
    }

    private static List<String> missingFields(String format, String audience) {
        List<String> missing = new ArrayList<>();
        if (format.isEmpty()) {
            missing.add("hình thức sự kiện");
        }
        if (audience.isEmpty()) {
            missing.add("khán giả mục tiêu");
        }
        missing.add("thời gian và địa điểm"); // owned by the event form, not filled yet
        return missing;
    }

    private static String body(AiDescriptionUiModel.Tone tone, AiDescriptionUiModel.Length length,
                               int variantIndex, String title, String format, String audience,
                               List<String> points) {
        StringBuilder builder = new StringBuilder();
        builder.append(opening(tone, variantIndex)).append(' ').append(title).append('.');

        if (format.isEmpty()) {
            builder.append(" [Cần bổ sung: hình thức sự kiện].");
        } else {
            builder.append(" Sự kiện được tổ chức theo hình thức ").append(format)
                    .append(" nên bạn có thể sắp xếp tham gia dễ dàng.");
        }
        builder.append(' ').append(joinPoints(points)).append('.');

        if (audience.isEmpty()) {
            builder.append(" [Cần bổ sung: khán giả mục tiêu].");
        } else {
            builder.append(" Đây là lựa chọn phù hợp cho ").append(audience).append('.');
        }

        // Placeholder for fields the form does not carry yet (O06-R02).
        builder.append(" [Cần bổ sung: thời gian và địa điểm].");

        if (length == AiDescriptionUiModel.Length.MEDIUM
                || length == AiDescriptionUiModel.Length.LONG) {
            builder.append(" Chương trình được tổ chức bài bản, phần nội dung chính được chia ")
                    .append("thành từng phần rõ ràng để bạn dễ theo dõi từ đầu đến cuối.");
        }
        if (length == AiDescriptionUiModel.Length.LONG) {
            builder.append(" Bên cạnh đó, ban tổ chức chuẩn bị khu vực giao lưu sau chương trình, ")
                    .append("giúp mọi người trò chuyện trực tiếp với diễn giả và cộng đồng tham gia, ")
                    .append("đồng thời có các phần hỏi đáp mở ở cuối mỗi phần.");
        }

        // Deliberate fixture: a value the assistant "made up" so the post-check has something
        // to flag. Real responses are produced by the backend and checked the same way.
        if (variantIndex == 0 && length == AiDescriptionUiModel.Length.SHORT) {
            builder.append(" Chi phí tham dự dự kiến 450.000đ vào 15/06.");
        }

        return builder.toString();
    }

    private static String opening(AiDescriptionUiModel.Tone tone, int variantIndex) {
        String[] options;
        switch (tone == null ? AiDescriptionUiModel.Tone.PROFESSIONAL : tone) {
            case FRIENDLY:
                options = new String[]{"Bạn sẽ thích", "Cùng bạn bè đến", "Một buổi đáng để đi"};
                break;
            case FORMAL:
                options = new String[]{"Trân trọng giới thiệu", "Kính gửi quý bạn",
                        "Chúng tôi trân trọng thông báo"};
                break;
            case PROFESSIONAL:
            default:
                options = new String[]{"Sự kiện", "Chúng tôi giới thiệu", "Đừng bỏ lỡ"};
                break;
        }
        return options[Math.max(0, Math.min(variantIndex, options.length - 1))];
    }

    private static String joinPoints(List<String> points) {
        if (points == null || points.isEmpty()) {
            return "Nội dung chương trình sẽ được cập nhật";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(points.get(i));
        }
        return builder.toString();
    }

    private static String inputSnapshot(String title, String format, String audience,
                                        List<String> points) {
        StringBuilder builder = new StringBuilder(title).append(' ').append(format).append(' ')
                .append(audience);
        if (points != null) {
            builder.append(' ').append(String.join(" ", points));
        }
        return builder.toString();
    }

    private static List<String> toTexts(List<AiDescriptionUiModel.Variant> variants) {
        List<String> texts = new ArrayList<>();
        for (AiDescriptionUiModel.Variant variant : variants) {
            texts.add(variant.text);
        }
        return texts;
    }

    private static String summarize(String text) {
        String flat = text.replaceAll("\\s+", " ").trim();
        if (flat.length() <= 160) {
            return flat;
        }
        return flat.substring(0, 157) + "…";
    }

    /**
     * O06-R03 post-check: report anything numeric/date-like/contact-like/link-like that the
     * input never contained.
     */
    public static List<AiDescriptionUiModel.Warning> postCheck(String output, String input) {
        List<AiDescriptionUiModel.Warning> warnings = new ArrayList<>();
        if (output == null || output.isEmpty()) {
            return warnings;
        }
        String source = input == null ? "" : input;
        String[] lines = output.split("\n");

        for (String line : lines) {
            addPatternWarnings(warnings, DATE_PATTERN, line, source,
                    AiDescriptionUiModel.WarningKind.DATE);
            addPatternWarnings(warnings, PHONE_PATTERN, line, source,
                    AiDescriptionUiModel.WarningKind.PHONE);
            addPatternWarnings(warnings, LINK_PATTERN, line, source,
                    AiDescriptionUiModel.WarningKind.LINK);
        }

        Matcher numbers = NUMBER_PATTERN.matcher(output);
        while (numbers.find()) {
            String value = numbers.group();
            if (source.contains(value) || containsWarning(warnings,
                    AiDescriptionUiModel.WarningKind.NUMBER, value)) {
                continue;
            }
            // A digit that belongs to an already flagged date is not a second finding.
            if (belongsToDate(warnings, value)) {
                continue;
            }
            warnings.add(new AiDescriptionUiModel.Warning(
                    AiDescriptionUiModel.WarningKind.NUMBER, value));
        }
        return warnings;
    }

    private static boolean belongsToDate(List<AiDescriptionUiModel.Warning> warnings,
                                         String value) {
        for (AiDescriptionUiModel.Warning warning : warnings) {
            if (warning.kind == AiDescriptionUiModel.WarningKind.DATE
                    && warning.detail.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static void addPatternWarnings(List<AiDescriptionUiModel.Warning> warnings,
                                           Pattern pattern, String line, String source,
                                           AiDescriptionUiModel.WarningKind kind) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String value = matcher.group();
            if (!source.contains(value) && !containsWarning(warnings, kind, value)) {
                warnings.add(new AiDescriptionUiModel.Warning(kind, value));
            }
        }
    }

    private static boolean containsWarning(List<AiDescriptionUiModel.Warning> warnings,
                                           AiDescriptionUiModel.WarningKind kind, String value) {
        for (AiDescriptionUiModel.Warning warning : warnings) {
            if (warning.kind == kind && warning.detail.equals(value)) {
                return true;
            }
        }
        return false;
    }
}