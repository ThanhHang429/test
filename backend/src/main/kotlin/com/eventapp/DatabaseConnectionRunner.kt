package com.eventapp

import com.eventapp.common.repository.DatabaseHealthRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DatabaseConnectionRunner(
    private val repository: DatabaseHealthRepository
) : CommandLineRunner {

    override fun run(vararg args: String) {
        val count = repository.count()
        println("=== DATABASE CONNECTION OK ===")
        println("Tables accessible via JPA: database_health (count=$count)")
        println("==============================")
    }
}
