package com.eventapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableAsync
@SpringBootApplication
class EventApplication

fun main(args: Array<String>) {
    runApplication<EventApplication>(*args)
}
