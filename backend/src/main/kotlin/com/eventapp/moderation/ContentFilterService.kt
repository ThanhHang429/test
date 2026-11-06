package com.eventapp.moderation

import com.eventapp.ai.repository.PlatformSettingRepository
import com.eventapp.common.util.VietnameseTextNormalizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Bộ lọc nội dung của FR-U20 (bước 3 luồng chính) và FR-A07 (cờ kiểm duyệt).
 *
 * Ba nhóm vi phạm theo SRS: từ khoá cấm, liên kết, số điện thoại.
 * Danh sách từ khoá đọc từ `platform_settings` khoá `content.blocked_keywords`
 * (schema thật không có bảng `content_rules`), cache 60 giây.
 *
 * Kết quả CHỈ dùng để gắn cờ — không tự sửa nội dung (A07-R03, A14-R03).
 */
@Service
class ContentFilterService(
    private val platformSettingRepository: PlatformSettingRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    data class Violation(val code: String, val label: String, val evidence: String)

    data class Result(val violations: List<Violation>) {
        val passed: Boolean get() = violations.isEmpty()
        val reason: String? get() = violations.firstOrNull()?.label
    }

    private data class CachedTerms(val terms: List<String>, val loadedAt: Instant)

    @Volatile
    private var cache: CachedTerms? = null

    private val urlPattern = Regex(
        """(?i)(https?://|www\.)\S+|(?<![\w@.])([a-z0-9][a-z0-9-]{0,62}\.)+(com|net|org|vn|io|me|info|biz|xyz|top|link)(?![\w.])"""
    )
    private val phonePattern = Regex(
        """(?<![\d+])(?:\+?84|0)(?:[\s.\-]?\d){8,10}(?!\d)"""
    )

    fun check(text: String?): Result {
        if (text.isNullOrBlank()) return Result(emptyList())
        val violations = mutableListOf<Violation>()

        val normalized = " ${VietnameseTextNormalizer.normalize(text)} "
        blockedTerms().forEach { term ->
            if (normalized.contains(" $term ")) {
                violations += Violation(BLOCKED_KEYWORD, "Chứa từ khoá bị chặn", term)
            }
        }

        urlPattern.find(text)?.let {
            violations += Violation(LINK, "Chứa liên kết ngoài", it.value.take(120))
        }
        phonePattern.find(text)?.let {
            violations += Violation(PHONE_NUMBER, "Chứa số điện thoại", maskPhone(it.value))
        }

        if (violations.isNotEmpty()) {
            log.debug("Nội dung bị gắn cờ: {}", violations.map { it.code })
        }
        return Result(violations)
    }

    /** Dùng cho A07: quét lại toàn bộ nội dung đang hiển thị. */
    fun blockedKeywords(): List<String> = blockedTerms()

    private fun blockedTerms(): List<String> {
        val current = cache
        if (current != null && Duration.between(current.loadedAt, Instant.now()) < CACHE_TTL) {
            return current.terms
        }
        val terms = try {
            platformSettingRepository.findFirstBySettingKeyOrderByVersionNoDesc(SETTING_KEY)
                ?.settingValue
                ?.get("terms")
                ?.let { raw ->
                    @Suppress("UNCHECKED_CAST")
                    (raw as? List<Any?>).orEmpty()
                        .mapNotNull { it?.toString() }
                        .map { VietnameseTextNormalizer.normalize(it) }
                        .filter { it.isNotBlank() }
                }
                .orEmpty()
        } catch (ex: Exception) {
            log.warn("Không đọc được {} — dùng danh sách rỗng", SETTING_KEY, ex)
            emptyList()
        }
        cache = CachedTerms(terms, Instant.now())
        return terms
    }

    private fun maskPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 6) return "***"
        return digits.take(3) + "***" + digits.takeLast(2)
    }

    companion object {
        const val SETTING_KEY = "content.blocked_keywords"

        const val BLOCKED_KEYWORD = "BLOCKED_KEYWORD"
        const val LINK = "LINK"
        const val PHONE_NUMBER = "PHONE_NUMBER"

        private val CACHE_TTL: Duration = Duration.ofSeconds(60)
    }
}
