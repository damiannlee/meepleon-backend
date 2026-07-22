package com.meepleday

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MeepledayApplication

fun main(args: Array<String>) {
    runApplication<MeepledayApplication>(*args)
}
