package com.eventapp.common.repository

import com.eventapp.common.entity.DatabaseHealth
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DatabaseHealthRepository : JpaRepository<DatabaseHealth, Long>
