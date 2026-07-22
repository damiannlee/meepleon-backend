package com.meepleday.common

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {

    /** Injected everywhere time is read so lifecycle logic stays testable. */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
