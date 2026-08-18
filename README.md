# GAMSS

일기가 아닌 "감정 쓰레기통" Android 앱이다. 사용자가 최대 140자로 감정을 정제 없이 남기면, 감정 캐릭터 6종 중 일부가 랜덤하게 반응하고 상호작용한다. 하나의 대화가 끝나면 카드로 저장되어, 캘린더에서 회고하거나 외부로 공유할 수 있다.

## 주요 기능

- 최대 140자로 감정을 정제 없이 기록
- 감정 캐릭터 6종의 랜덤 반응 및 상호작용
- 대화 종료 시 카드 생성, 캘린더에서 회고
- 카드 이미지 저장 및 인스타그램 공유

## 핵심 도메인

- **대화**: 사용자의 기록 제출로 시작해 사용자가 명시적으로 종료할 때까지의 상호작용 단위. (1차 반응 → 사용자 대댓글 → 2차 반응 등 포함)
- **카드**: 대화가 종료될 때만 생성되는 결과물. 이름은 사용자가 정하는 별명이 아니라 클라이언트가 생성한 요약을 사용한다. 캘린더·공유의 기본 단위다.

### 감정 캐릭터 6종

| 캐릭터 | 역할 |
| --- | --- |
| 기쁨 | 긍정적 환기 |
| 분노 | 대신 화내주는 역할 |
| 불안 | 내가 느끼는 감정을 투영 |
| 까칠 | 시니컬하게 반응 |
| 슬픔 | 슬픈 감정 공감 |
| 엉뚱 | 덤덤하게 딴소리해서 주의력 환기 |

## 요구 사항

| 항목 | 버전 |
| --- | --- |
| JDK | 17 이상 |
| Gradle | 9.4.1 (wrapper 포함) |
| Android Studio | 최신 안정 버전 권장 |

minSdk / targetSdk / compileSdk 등 세부 빌드 설정은 확정되는 대로 업데이트한다.

## 시작하기

### 1. 저장소 클론

```bash
git clone <repository-url>
cd GAMSS-Android
```

### 2. local.properties 설정

`local.properties`에 Android SDK 경로와 API 서버 주소를 지정한다. (SDK 경로는 Android Studio로 열면 자동 생성된다.)

```properties
sdk.dir=/path/to/Android/sdk

# 필수. 없으면 빌드가 실패한다.
DEV_BASE_URL=<debug-base-url>
PROD_BASE_URL=<release-base-url>
```

`DEV_BASE_URL`은 debug 빌드, `PROD_BASE_URL`은 release 빌드의 `BuildConfig.BASE_URL`이 된다.
기본값으로 떨어뜨리지 않는 이유는, 값이 빠진 채 빌드되면 잘못된 서버를 가리키는 앱이 나오기 때문이다.

CI에서는 저장소 시크릿(`DEV_BASE_URL`, `PROD_BASE_URL`)으로 같은 파일을 만든다.

API 키 등 비밀 값이 필요한 경우에도 `local.properties`에 두고 저장소에 커밋하지 않는다. (`.gitignore`에 포함)

### 3. 빌드 및 실행

```bash
# 디버그 빌드
./gradlew assembleDebug

# 연결된 기기/에뮬레이터에 설치
./gradlew installDebug

# 테스트 실행
./gradlew test
```

## 프로젝트 구조

멀티모듈 구조로 구성한다. 새 feature나 데이터 소스를 추가할 때도 아래 구조와 의존성 방향을 따른다.

```
GAMSS-Android/
├─ app/                # 앱 진입점, Hilt DI 그래프 조립, Navigation3 호스트
├─ domain/             # UseCase, Repository 인터페이스, 도메인 모델 (순수 Kotlin/JVM, Android 의존성 없음)
├─ data/                # Repository 구현체, 원격(remote)·로컬 데이터 소스, DI 모듈(di)
├─ core/
│   ├─ common/         # 모듈 간 공유되는 순수 유틸 (AppResult 등)
│   └─ ui/             # 공용 Compose UI 컴포넌트 (GamssBottomBar 등)
├─ feature/
│   └─ home/           # 화면 단위 기능 모듈. 화면별로 하나씩 추가한다
└─ gradle/             # 버전 카탈로그, wrapper
```

| 모듈 | 역할 | 의존하는 모듈 |
| --- | --- | --- |
| `app` | 앱 진입점, Hilt DI 그래프 조립, Navigation3 호스트 | `domain`, `data`, `core:common`, `core:ui`, `feature:*` |
| `domain` | UseCase, Repository 인터페이스, 도메인 모델 | 없음 |
| `data` | Repository 구현, 원격/로컬 데이터 소스, Hilt DI 모듈 | `domain`, `core:common` |
| `core:common` | 여러 모듈이 공유하는 순수 유틸 | 없음 |
| `core:ui` | 공용 Compose UI 컴포넌트 | 없음 |
| `feature:*` | 화면 단위 기능 모듈 (기능별로 분리) | `core:ui` (필요 시 `domain`) |

**의존성 규칙**: 안쪽 레이어(`domain`)는 바깥쪽 어떤 모듈도 참조하지 않는다. `data`, `feature`, `app`은 `domain`이 정의한 인터페이스(Repository, UseCase)에 의존하고, 구현은 바깥쪽(`data`)에 둔다. `app`은 조립부이므로 예외적으로 전체 모듈을 참조한다.

## 기술 스택

| 항목 | 선택 |
| --- | --- |
| 언어 / UI | Kotlin / Jetpack Compose |
| 의존성 주입 | Hilt |
| 상태 관리 | Orbit |
| 네비게이션 | Navigation3 |
| 네트워크 | Retrofit |
| 로컬 DB | Room 3.0 |
| 이미지 로드 | Coil |
| 테스트 | JUnit |
| 앱 트래킹 | Firebase / Google Analytics |
| CI/CD | GitHub Actions |
| 온디바이스 AI | LiteRT |
| 소셜 로그인 | Google |
| 공유 | 인스타그램 ShareScheme |

## 컨벤션

### 커밋 메시지

`type: 메시지` 형식을 쓴다.

| 타입 | 설명 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `chore` | 빌드, 설정 등 기타 작업 |
| `refactor` | 리팩터링 |

예: `feat: 카드 공유 기능 추가`

참고: [Conventional Commits](https://www.conventionalcommits.org/)

### 브랜칭 전략

Git Flow를 따른다.

| 브랜치 | 용도 |
| --- | --- |
| `main` | 배포 가능한 안정 브랜치 |
| `develop` | 개발 통합 브랜치 |
| `feature/*` | 기능 개발 브랜치 |
| `release/*` | 릴리스 준비 브랜치 |
| `hotfix/*` | 긴급 수정 브랜치 |

### PR 제목

```
[GMSS-${Notion Task ID}] 제목
```

예: `[GMSS-12] 카드 공유 기능 추가`

`GMSS-` 뒤의 번호는 Notion Task ID다.

## 주의 사항

- `local.properties`와 API 키·토큰 등 비밀 값은 커밋하지 않는다. (`.gitignore` 포함 확인)
- `main`은 배포 가능한 상태만 유지한다. 작업은 `feature/*` 브랜치에서 진행한 뒤 PR로 병합한다.
