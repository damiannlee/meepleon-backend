@docs/CLAUDE.md

# Meepleon Backend

Kotlin/Spring Boot API 서버. 실행·테스트·API 요약은 [README](README.md).

## 코딩 컨벤션 (백엔드 전용)

- **N+1 금지**: 반복문 안에서 레포지토리/쿼리 호출 금지. 배치 로딩 + `groupBy`/`associateBy`.
- **트랜잭션 경계 명시**: 읽기 전용은 `@Transactional(readOnly = true)`, 쓰기는 `@Transactional`.
- **현재 유저 접근**: 서비스에서 `SecurityContext`를 직접 잡지 말 것. [`CurrentUserProvider`](src/main/kotlin/com/meepleon/user/CurrentUserProvider.kt) 주입.
- **Null safety**: `?: throw` 엘비스 연산자만. `!!`·`requireNotNull()`·`if (x == null) throw` 금지.
- **DTO 변환**: 서비스가 companion `.of()` 팩토리로 DTO 리턴. 컨트롤러가 `.of()` 직접 호출 금지.
- **스코프 함수**: `.apply{}.also{}.let{}` 3단 체이닝 금지. 명시적 statement로 풀 것.
- **문자열 결합**: `+` 대신 템플릿 리터럴만.
- **엔티티 상태 전이**는 도메인 메서드로 캡슐화(예: `Event.publish()`/`reject()`).

## DB / 마이그레이션

- 스키마 단일 소스 = Flyway(`src/main/resources/db/migration`). `ddl-auto=validate`로 엔티티↔스키마 정합 검증.
- dev H2는 PostgreSQL 호환 모드 → 마이그레이션 이식성. 새 스키마 변경은 PostgreSQL/H2 양쪽에서 유효한 SQL만.

## 보안

- `/api/admin/**`는 이미 `hasRole("ADMIN")`으로 잠김 — [SecurityConfig](src/main/kotlin/com/meepleon/user/SecurityConfig.kt). ADMIN은 자가가입 불가(허용목록으로만 부여).
- 인증은 세션 쿠키 + CSRF 쿠키. `/api/**` 인증 실패는 리다이렉트가 아니라 `401`.

## API 문서 (OpenAPI)

- `docs/openapi.yaml` = 요청/응답 스키마 단일 소스([크로스 레포 협업](docs/CLAUDE.md#크로스-레포-협업) 참조) — 컨트롤러 애노테이션에서 springdoc-openapi로 자동 생성, 수기 작성 금지.
- 컨트롤러 시그니처(경로·파라미터·요청/응답 바디)를 바꾸면 `./gradlew generateOpenApiDocs`로 재생성 후 `docs` 서브모듈 커밋에 포함.
- PR이 `docs/`를 건드린 채 `main`에 머지되면 [`notify-frontend.yml`](.github/workflows/notify-frontend.yml)이 frontend 레포에 반영 대기 이슈를 자동 생성(`FRONTEND_ISSUE_TOKEN` 시크릿 사용) — 별도 수동 안내 불필요.
- **`docs` 서브모듈에 backend 세션이 직접 commit·push하는 건 위 openapi.yaml 재생성 반영이 유일한 경우.** PRD·ADR·README 등 문서 콘텐츠 편집은 docs repo 자체 워크플로우 소관 — backend 작업 중 임의로 고쳐 push하지 않는다. 서브모듈 포인터는 원격 docs `main`을 pull해 최신으로 맞추는 것으로 충분.
- **서브모듈 포인터 갱신만 있는 단독 PR은 만들지 않는다** — 코드 변경 없는 1줄 diff라 PR 목록만 지저분해짐(PR #9 이후 결정). 진행 중인 기능 브랜치의 커밋 하나로 포함시켜 반영. 기능 브랜치 없이 최신 docs가 당장 필요할 때만 예외적으로 단독 chore PR.

## 검증

- 완료 주장 전 `./gradlew test` 전체 통과를 실제로 확인(증거 없이 "됐다" 금지).
- 새 기능은 단위 + 통합 테스트 동반. 자체 리뷰: N+1·트랜잭션 경계·타 유저 자원 접근 차단.
- **컨트롤러 시그니처(경로·파라미터·요청/응답 바디)를 바꿨다면** `./gradlew generateOpenApiDocs`로 `docs/openapi.yaml` 재생성 여부 확인(위 [API 문서](#api-문서-openapi) 절 참조) — 테스트 통과와 별개로 매번 체크.
