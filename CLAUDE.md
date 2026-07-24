@docs/CLAUDE.md

# MeepleDay Backend

Kotlin/Spring Boot API 서버. 실행·테스트·API 요약은 [README](README.md).

## 코딩 컨벤션 (백엔드 전용)

- **N+1 금지**: 반복문 안에서 레포지토리/쿼리 호출 금지. 배치 로딩 + `groupBy`/`associateBy`.
- **트랜잭션 경계 명시**: 읽기 전용은 `@Transactional(readOnly = true)`, 쓰기는 `@Transactional`.
- **현재 유저 접근**: 서비스에서 `SecurityContext`를 직접 잡지 말 것. [`CurrentUserProvider`](src/main/kotlin/com/meepleday/user/CurrentUserProvider.kt) 주입.
- **Null safety**: `?: throw` 엘비스 연산자만. `!!`·`requireNotNull()`·`if (x == null) throw` 금지.
- **DTO 변환**: 서비스가 companion `.of()` 팩토리로 DTO 리턴. 컨트롤러가 `.of()` 직접 호출 금지.
- **스코프 함수**: `.apply{}.also{}.let{}` 3단 체이닝 금지. 명시적 statement로 풀 것.
- **문자열 결합**: `+` 대신 템플릿 리터럴만.
- **엔티티 상태 전이**는 도메인 메서드로 캡슐화(예: `Event.publish()`/`reject()`).

## DB / 마이그레이션

- 스키마 단일 소스 = Flyway(`src/main/resources/db/migration`). `ddl-auto=validate`로 엔티티↔스키마 정합 검증.
- dev H2는 PostgreSQL 호환 모드 → 마이그레이션 이식성. 새 스키마 변경은 PostgreSQL/H2 양쪽에서 유효한 SQL만.

## 보안

- `/api/admin/**`는 이미 `hasRole("ADMIN")`으로 잠김 — [SecurityConfig](src/main/kotlin/com/meepleday/user/SecurityConfig.kt). ADMIN은 자가가입 불가(허용목록으로만 부여).
- 인증은 세션 쿠키 + CSRF 쿠키. `/api/**` 인증 실패는 리다이렉트가 아니라 `401`.

## 검증

- 완료 주장 전 `./gradlew test` 전체 통과를 실제로 확인(증거 없이 "됐다" 금지).
- 새 기능은 단위 + 통합 테스트 동반. 자체 리뷰: N+1·트랜잭션 경계·타 유저 자원 접근 차단.
