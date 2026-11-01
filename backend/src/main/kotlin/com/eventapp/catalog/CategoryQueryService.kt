package com.eventapp.catalog

import com.eventapp.catalog.repository.EventCategoryRepository
import com.eventapp.discovery.dto.CategoryNodeDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryQueryService(
    private val categoryRepository: EventCategoryRepository
) {

    /** Cây danh mục ACTIVE, tối đa 2 cấp (API-02). */
    fun activeTree(): List<CategoryNodeDto> {
        val all = categoryRepository.findByIsActiveTrueOrderBySortOrderAscCategoryIdAsc()
        val byParent = all.groupBy { it.parentCategoryId }
        return (byParent[null] ?: emptyList()).map { root ->
            CategoryNodeDto(
                id = root.categoryId!!,
                code = root.categoryCode,
                name = root.categoryName,
                slug = root.slug,
                description = root.description,
                parentId = root.parentCategoryId,
                sortOrder = root.sortOrder,
                children = (byParent[root.categoryId] ?: emptyList()).map { child ->
                    CategoryNodeDto(
                        id = child.categoryId!!,
                        code = child.categoryCode,
                        name = child.categoryName,
                        slug = child.slug,
                        description = child.description,
                        parentId = child.parentCategoryId,
                        sortOrder = child.sortOrder
                    )
                }
            )
        }
    }

    /** Dùng cho Admin: bao gồm cả danh mục đã ẩn. */
    fun fullTree(): List<CategoryNodeDto> {
        val all = categoryRepository.findAllByOrderBySortOrderAscCategoryIdAsc()
        val byParent = all.groupBy { it.parentCategoryId }
        return (byParent[null] ?: emptyList()).map { root ->
            CategoryNodeDto(
                id = root.categoryId!!,
                code = root.categoryCode,
                name = root.categoryName,
                slug = root.slug,
                description = root.description,
                parentId = root.parentCategoryId,
                sortOrder = root.sortOrder,
                children = (byParent[root.categoryId] ?: emptyList()).map { child ->
                    CategoryNodeDto(
                        id = child.categoryId!!,
                        code = child.categoryCode,
                        name = child.categoryName,
                        slug = child.slug,
                        description = child.description,
                        parentId = child.parentCategoryId,
                        sortOrder = child.sortOrder
                    )
                }
            )
        }
    }
}
