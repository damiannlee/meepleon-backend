package com.meepleday

import com.meepleday.user.AdminAllowlistProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AdminAllowlistProperties::class)
class MeepledayApplication

fun main(args: Array<String>) {
    runApplication<MeepledayApplication>(*args)
}
