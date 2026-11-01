package com.eventapp.catalog.repository

import com.eventapp.catalog.entity.EventCategory
import com.eventapp.catalog.entity.EventCategoryMap
import com.eventapp.catalog.entity.EventCategoryMapId
import com.eventapp.catalog.entity.EventTag
import com.eventapp.catalog.entity.EventTagMap
import com.eventapp.catalog.entity.EventTagMapId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EventCategoryRepository : JpaRepository<EventCategory, Long> {
    fun findAllByOrderBySortOrderAscCategoryIdAsc(): List<EventCategory>
    fun findByIsActiveTrueOrderBySortOrderAscCategoryIdAsc(): List<EventCategory>
    fun findByCategoryCode(categoryCode: String): EventCategory?
    fun findBySlug(slug: String): EventCategory?
    fun findByParentCategoryId(parentCategoryId: Long): List<EventCategory>
    fun countByParentCategoryId(parentCategoryId: Long): Long
}

@Repository
interface EventTagRepository : JpaRepository<EventTag, Long> {
    fun findBySlug(slug: String): EventTag?
    fun findByTagCode(tagCode: String): EventTag?
    fun findByIsActiveTrueAndTagNameContainingIgnoreCaseOrderByTagNameAsc(name: String): List<EventTag>
    fun findByIsActiveTrueOrderByTagNameAsc(): List<EventTag>
}

@Repository
interface EventCategoryMapRepository : JpaRepository<EventCategoryMap, EventCategoryMapId> {
    fun findByIdEventId(eventId: Long): List<EventCategoryMap>
    fun findByIdEventIdIn(eventIds: Collection<Long>): List<EventCategoryMap>
    fun deleteByIdEventId(eventId: Long)
}

@Repository
interface EventTagMapRepository : JpaRepository<EventTagMap, EventTagMapId> {
    fun findByIdEventId(eventId: Long): List<EventTagMap>
    fun findByIdEventIdIn(eventIds: Collection<Long>): List<EventTagMap>
    fun deleteByIdEventId(eventId: Long)

    @Query("select count(tm) from EventTagMap tm where tm.id.tagId = :tagId")
    fun countByTagId(tagId: Long): Long
}
