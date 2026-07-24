# MeepleDay Backend

[MeepleDay](https://github.com/damiannlee/meepleday-docs) — 흩어져 있는 보드게임 이벤트 일정(펀딩·선주문·특가·오프라인 행사·예고)을 국내·해외 통합해 한 곳에서 보는 서비스의 백엔드.

제품 정의·로드맵·ADR은 [docs](docs)(submodule, [meepleday-docs](https://github.com/damiannlee/meepleday-docs)) 참조. 프론트엔드는 [meepleday-frontend](https://github.com/damiannlee/meepleday-frontend).

## 스택

Kotlin 1.9 · Spring Boot 3.4 · Spring Security(OAuth2 Client) · JPA/Hibernate · Flyway
DB: dev = H2(PostgreSQL 호환 모드), prod = PostgreSQL

## 실행

```bash
./gradlew bootRun            # 기본 dev 프로파일, http://localhost:8080
# 포트 충돌 시:
./gradlew bootRun --args='--server.port=18080'
```

dev 프로파일은 인메모리 H2 + 샘플 이벤트 시드([DevDataLoader](src/main/kotlin/com/meepleday/event/DevDataLoader.kt))로 뜬다.

소셜 로그인을 실제로 쓰려면 Kakao/Google OAuth2 클라이언트 자격증명 설정이 필요하다. 미설정이어도 공개 피드·제보는 동작한다.

## 테스트

```bash
./gradlew test               # 도메인 단위 + API 통합 테스트
```

## API 요약

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/events` | — | 공개 피드 (PUBLISHED만, 필터·정렬·페이징) |
| GET | `/api/events/{id}` | — | 이벤트 상세 (PUBLISHED만) |
| POST | `/api/events` | — | 이벤트 제보 (PENDING 생성, 익명 허용) |
| GET | `/api/admin/events?status=PENDING` | ADMIN | 검수 큐 |
| PATCH | `/api/admin/events/{id}/moderation` | ADMIN | 승인/반려 |
| GET | `/api/me` | 로그인 | 현재 사용자 |
| GET | `/oauth2/authorization/{kakao\|google}` | — | 소셜 로그인 시작 |
| POST | `/api/auth/logout` | 로그인 | 로그아웃 |

- 인증은 **세션 쿠키 + CSRF 쿠키**([SecurityConfig](src/main/kotlin/com/meepleday/user/SecurityConfig.kt)). `/api/**` 인증 실패는 리다이렉트가 아니라 `401`.
- ADMIN은 자가가입 불가 — 허용목록(`AdminAllowlistProperties`)으로 부여.

## 문서

```bash
git submodule update --init   # docs/ 서브모듈 최초 클론 시 필요
```

| 문서 | 내용 |
|---|---|
| [docs/prd.md](docs/prd.md) | 기획서 — 제품 정의 단일 소스 |
| [docs/product.md](docs/product.md) | 성공지표 · 데이터 수급 전략 |
| [docs/adr](docs/adr) | 설계 결정과 기각한 대안 |
| [docs/spec](docs/spec) | 기능명세 |
