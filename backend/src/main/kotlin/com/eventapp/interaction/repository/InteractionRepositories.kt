package com.eventapp.interaction.repository

import com.eventapp.common.enums.ReviewStatus
import com.eventapp.interaction.entity.Favorite
import com.eventapp.interaction.entity.FavoriteId
import com.eventapp.interaction.entity.Review
import com.eventapp.interaction.entity.UserPreference
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface FavoriteRepository : JpaRepository<Favorite, FavoriteId> {
    fun findByIdUserIdOrderByCreatedAtDesc(userId: Long): List<Favorite>
    fun countByIdUserId(userId: Long): Long
    fun existsByIdUserIdAndIdEventId(userId: Long, eventId: Long): Boolean
    fun deleteByIdUserIdAndIdEventId(userId: Long, eventId: Long)
    fun findByIdUserIdAndIdEventIdIn(userId: Long, eventIds: Collection<Long>): List<Favorite>
}

@Repository
interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByUserIdAndEventId(userId: Long, eventId: Long): Review?
    fun findByEventIdAndStatusOrderByCreatedAtDesc(eventId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>
    fun findByEventIdAndStatusAndUserIdNotOrderByCreatedAtDesc(
        eventId: Long,
        status: ReviewStatus,
        userId: Long,
        pageable: Pageable
    ): Page<Review>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Review>
    fun findByStatusOrderByCreatedAtAsc(status: ReviewStatus, pageable: Pageable): Page<Review>
    fun findByStatus(status: ReviewStatus): List<Review>
    fun findByStatusAndReviewTextIsNotNull(status: ReviewStatus): List<Review>
    fun countByEventIdAndStatus(eventId: Long, status: ReviewStatus): Long
    fun countByUserIdAndCreatedAtAfter(userId: Long, since: Instant): Long

    /** Hàng chờ kiểm duyệt A14: lọc theo trạng thái / sự kiện / số sao (tham số null = bỏ qua). */
    @Query(
        """
        select r from Review r
        where (:status is null or r.status = :status)
          and (:eventId is null or r.eventId = :eventId)
          and (:rating is null or r.rating = :rating)
        order by r.createdAt asc
        """
    )
    fun searchForAdmin(status: ReviewStatus?, eventId: Long?, rating: Byte?, pageable: Pageable): Page<Review>

    @Query("select avg(r.rating) from Review r where r.eventId = :eventId and r.status = 'VISIBLE'")
    fun averageRating(eventId: Long): Double?

    @Query(
        """
        select r.rating, count(r) from Review r
        where r.eventId = :eventId and r.status = 'VISIBLE'
        group by r.rating
        """
    )
    fun ratingDistribution(eventId: Long): List<Array<Any>>
}

@Repository
interface UserPreferenceRepository : JpaRepository<UserPreference, Long>
