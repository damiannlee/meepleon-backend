package com.meepleday.event

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Seeds a handful of sample events in dev so the feed isn't empty while iterating on the UI. */
@Configuration
@Profile("dev")
class DevDataLoader {

    @Bean
    fun seedEvents(eventRepository: EventRepository): ApplicationRunner = ApplicationRunner {
        if (eventRepository.count() > 0) return@ApplicationRunner
        val now = Instant.now()
        eventRepository.saveAll(
            listOf(
                Event(
                    title = "다이스 쓰론: 시즌2 텀블벅 펀딩",
                    eventType = EventType.FUNDING,
                    region = Region.DOMESTIC,
                    platform = "텀블벅",
                    originalUrl = "https://tumblbug.com/example-dicethrone",
                    description = "인기 주사위 배틀 게임 시즌2 한국어판 펀딩.",
                    publisher = "코리아보드게임즈",
                    startAt = now.minus(5, ChronoUnit.DAYS),
                    endAt = now.plus(30, ChronoUnit.HOURS),
                ).apply { publish() },
                Event(
                    title = "Nemesis: Retaliation — Gamefound",
                    eventType = EventType.FUNDING,
                    region = Region.OVERSEAS,
                    platform = "Gamefound",
                    originalUrl = "https://gamefound.com/example-nemesis",
                    description = "Awaken Realms 신작 협력 생존 게임.",
                    publisher = "Awaken Realms",
                    startAt = now.minus(2, ChronoUnit.DAYS),
                    endAt = now.plus(12, ChronoUnit.DAYS),
                ).apply { publish() },
                Event(
                    title = "아컴호러 카드게임 신규 확장 선주문",
                    eventType = EventType.PREORDER,
                    region = Region.DOMESTIC,
                    platform = "보드엠",
                    originalUrl = "https://boardm.co.kr/example-arkham",
                    startAt = now.minus(1, ChronoUnit.DAYS),
                    endAt = now.plus(9, ChronoUnit.DAYS),
                ).apply { publish() },
                Event(
                    title = "윙스팬 아시아 한정 특가",
                    eventType = EventType.SALE,
                    region = Region.DOMESTIC,
                    platform = "다이브다이스",
                    originalUrl = "https://divedice.com/example-wingspan",
                    startAt = now.minus(3, ChronoUnit.DAYS),
                    endAt = now.plus(3, ChronoUnit.DAYS),
                ).apply { publish() },
                Event(
                    title = "2026 서울 보드게임 페스타",
                    eventType = EventType.OFFLINE_EVENT,
                    region = Region.DOMESTIC,
                    platform = "보드게임 페스타",
                    originalUrl = "https://example.com/bg-festa-2026",
                    description = "국내 최대 규모 보드게임 오프라인 행사.",
                    startAt = now.plus(20, ChronoUnit.DAYS),
                    endAt = now.plus(22, ChronoUnit.DAYS),
                    location = "서울 코엑스",
                    address = "서울특별시 강남구 영동대로 513",
                    reservationUrl = "https://example.com/bg-festa-2026/reserve",
                ).apply { publish() },
                Event(
                    title = "Frosthaven 한글판",
                    eventType = EventType.PREORDER,
                    region = Region.DOMESTIC,
                    platform = "코리아보드게임즈",
                    originalUrl = "https://example.com/frosthaven-kr",
                    description = "일정 미정, 정식 발표 대기 중.",
                    scheduleNote = "2026년 4분기 예정",
                ).apply { publish() },
                // A pending submission so the admin queue isn't empty either.
                Event(
                    title = "제보 테스트: 이름 미정 신작 펀딩",
                    eventType = EventType.FUNDING,
                    region = Region.OVERSEAS,
                    platform = "Kickstarter",
                    originalUrl = "https://kickstarter.com/example-pending",
                    endAt = now.plus(20, ChronoUnit.DAYS),
                    submitterName = "보드게이머A",
                    submitterEmail = "gamer@example.com",
                ),
            ),
        )
    }
}
