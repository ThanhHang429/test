package com.eventapp.common.util

import java.text.Normalizer

/**
 * Chuẩn hoá tiếng Việt để tìm kiếm không dấu (SRS U04-R04: "hoi nhac" phải tìm được "Hội nhạc").
 *
 * Bảng `events` không có cột `search_text` (schema do nhóm quản lý), nên không thể lưu sẵn
 * chuỗi đã chuẩn hoá. Thay vào đó sinh biểu thức regex cho từng token: mỗi nguyên âm được
 * mở rộng thành một nhóm ký tự cùng "gốc" (a → [aàáảãạăằắẳẵặâầấẩẫậ]).
 *
 * Đánh đổi: truy vấn dùng REGEXP_LIKE nên KHÔNG tận dụng được index.
 * Với quy mô đồ án (< 10.000 sự kiện) vẫn đạt mục tiêu p95 ≤ 800 ms.
 * Nếu sau này cần tối ưu: thêm cột `search_text` + index (khi được phép đổi DDL).
 */
object VietnameseTextNormalizer {

    private val DIACRITIC_GROUPS: Map<Char, String> = mapOf(
        'a' to "aàáảãạăằắẳẵặâầấẩẫậ",
        'e' to "eèéẻẽẹêềếểễệ",
        'i' to "iìíỉĩị",
        'o' to "oòóỏõọôồốổỗộơờớởỡợ",
        'u' to "uùúủũụưừứửữự",
        'y' to "yỳýỷỹỵ",
        'd' to "dđ"
    )

    /** Bỏ dấu, lowercase, gộp khoảng trắng. */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        val withoutMarks = decomposed.replace(Regex("\\p{M}+"), "")
        return withoutMarks
            .replace('đ', 'd')
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** Tách token (tối đa 8) để dựng điều kiện tìm kiếm. */
    fun tokens(input: String?): List<String> =
        normalize(input).split(' ').filter { it.isNotBlank() }.take(MAX_TOKENS)

    /**
     * Sinh regex (không neo) cho một token đã chuẩn hoá.
     * Ví dụ "nhac" -> "n[h][aàáảãạăằắẳẵặâầấẩẫậ][c]"
     */
    fun regexPattern(token: String): String {
        val sb = StringBuilder()
        for (ch in token) {
            val group = DIACRITIC_GROUPS[ch]
            if (group != null) {
                sb.append('[').append(group).append(']')
            } else if (ch.isLetterOrDigit()) {
                sb.append(Regex.escape(ch.toString()))
            }
        }
        return sb.toString()
    }

    /** true nếu chuỗi chỉ gồm ký tự đặc biệt (coi như rỗng — SRS U04). */
    fun isMeaningless(input: String?): Boolean = tokens(input).isEmpty()

    const val MAX_TOKENS = 8
}
