package com.meepleon

import com.meepleon.prefill.PrefillProperties
import com.meepleon.user.AdminAllowlistProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AdminAllowlistProperties::class, PrefillProperties::class)
class MeepleonApplication

fun main(args: Array<String>) {
    runApplication<MeepleonApplication>(*args)
}
