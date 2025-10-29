# HairinJob Scraper (Java)

> Java Spring Boot 기반 HairinJob.com 채용공고 스크래퍼

## 🚀 기술 스택

- **Java**: 21 (LTS)
- **Spring Boot**: 3.5.0
- **빌드 도구**: Gradle 8.x
- **HTTP 클라이언트**: RestClient (Spring 6.1+)
- **브라우저 자동화**: Playwright
- **HTML 파싱**: Jsoup
- **데이터베이스**: PostgreSQL 15 + Spring Data JPA
- **비동기 처리**: @Async + ThreadPoolTaskExecutor (10 workers)

## 📋 주요 기능

- ✅ Playwright 기반 자동 로그인
- ✅ 10개 워커로 병렬 페이지 스크래핑
- ✅ 상세페이지 정보 추출 (연락처, 업체 정보)
- ✅ PostgreSQL 데이터베이스 저장
- ✅ 세션 추적 및 이력 관리
- ✅ 20분마다 쿠키 자동 갱신
- ✅ 랜덤 딜레이 (봇 감지 회피)

## 🔧 설치 및 설정

### 1. 사전 요구사항

```bash
# Java 21 설치 확인
java -version

# PostgreSQL 15 설치 및 실행
# 데이터베이스 생성
createdb hairinjob
```

### 2. 환경 변수 설정

`.env` 파일 생성:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/hairinjob
DB_USER=postgres
DB_PASSWORD=your_password

# Scraper
SCRAPER_USERNAME=01028721278
SCRAPER_PASSWORD=235194

# Browser
USER_DATA_DIR=./playwright-data
HEADLESS=false
```

### 3. 데이터베이스 스키마 생성

Python 프로젝트의 `schema.sql`을 실행하거나 JPA가 자동 생성하도록 설정:

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 개발 시에만 사용
```

### 4. Playwright 브라우저 설치

```bash
./gradlew build
# Playwright 브라우저 자동 다운로드됨
```

## 🎮 사용법

### 빌드

```bash
./gradlew clean build
```

### 실행

```bash
# 전체 스크래핑 (페이지 + 상세)
java -jar build/libs/java-hairinjob-0.0.1-SNAPSHOT.jar scrape-all

# 상세페이지만 스크래핑
java -jar build/libs/java-hairinjob-0.0.1-SNAPSHOT.jar scrape-all --skip-pages

# 도움말
java -jar build/libs/java-hairinjob-0.0.1-SNAPSHOT.jar help
```

### Gradle로 직접 실행

```bash
# 전체 스크래핑
./gradlew bootRun --args='scrape-all'

# 상세페이지만
./gradlew bootRun --args='scrape-all --skip-pages'
```

## 📁 프로젝트 구조

```
src/main/java/org/nicenomu/hairinjob/
├── Application.java                          # 메인 애플리케이션
├── common/
│   ├── config/
│   │   ├── AsyncConfig.java                  # 비동기 스레드풀 설정
│   │   ├── JpaConfig.java                    # JPA 설정
│   │   ├── RestClientConfig.java             # HTTP 클라이언트 설정
│   │   └── ScraperProperties.java            # 프로퍼티 바인딩
│   └── util/
│       └── GlobalConstants.java              # 전역 상수
├── domain/
│   └── job/
│       ├── model/
│       │   ├── entity/
│       │   │   ├── BaseEntity.java           # 공통 엔티티
│       │   │   ├── JobPostingEntity.java     # 채용공고 엔티티
│       │   │   └── ScrapeSessionEntity.java  # 세션 엔티티
│       │   └── dto/
│       │       ├── JobPostingDTO.java
│       │       └── DetailInfoDTO.java
│       ├── repository/
│       │   ├── JobPostingRepository.java
│       │   └── ScrapeSessionRepository.java
│       └── service/
│           ├── HtmlParserService.java         # HTML 파싱
│           ├── PageScrapingService.java       # 페이지 스크래핑
│           ├── DetailScrapingService.java     # 상세페이지 스크래핑
│           └── JobScrapingOrchestratorService.java  # 전체 조율
├── infrastructure/
│   ├── browser/
│   │   └── PlaywrightManager.java            # 브라우저 자동화
│   └── http/
│       └── HttpClientService.java            # HTTP 클라이언트
└── cli/
    └── ScraperCommandLineRunner.java         # CLI 인터페이스
```

## 🔄 실행 흐름

1. **Playwright 초기화**: 브라우저 실행 및 로그인
2. **쿠키 추출**: 로그인 세션 쿠키 저장
3. **페이지 수 감지**: 전체 페이지 개수 확인
4. **페이지 범위 분할**: 10개 워커에 균등 분배
5. **병렬 스크래핑**: @Async로 동시 실행
6. **데이터베이스 저장**: JPA로 UPSERT
7. **상세페이지 스크래핑**: 순차 처리 (20분마다 쿠키 갱신)
8. **세션 완료**: 통계 업데이트 및 종료

## 📊 데이터베이스 스키마

### job_postings

| 컬럼 | 타입 | 설명 |
|------|------|------|
| idx | VARCHAR(20) | Primary Key |
| company_name | VARCHAR(200) | 회사명 |
| title | TEXT | 공고 제목 |
| detail_url | VARCHAR(500) | 상세페이지 URL |
| positions | TEXT | 모집 직종 |
| location | VARCHAR(100) | 근무지역 |
| salary | VARCHAR(100) | 급여 |
| phone | VARCHAR(20) | 전화번호 (상세요강) |
| company_phone | VARCHAR(20) | 회사 대표 전화 |
| recruiter_name | VARCHAR(100) | 채용담당자 |
| recruiter_contact | VARCHAR(20) | 담당자 연락처 |
| special_badges | JSONB | 특수 배지 |
| session_id | INTEGER | FK → scrape_sessions |

### scrape_sessions

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | SERIAL | Primary Key |
| started_at | TIMESTAMP | 시작 시간 |
| completed_at | TIMESTAMP | 완료 시간 |
| total_jobs | INTEGER | 총 수집 건수 |
| status | VARCHAR(20) | 상태 (RUNNING/COMPLETED/FAILED) |

## ⚙️ 설정 (application.yml)

```yaml
scraper:
  target-url: https://www.hairinjob.com
  login:
    username: ${SCRAPER_USERNAME}
    password: ${SCRAPER_PASSWORD}
  browser:
    user-data-dir: ${USER_DATA_DIR:./playwright-data}
    headless: ${HEADLESS:false}
  cookie-refresh-interval-minutes: 20
  request-delay-min-ms: 300
  request-delay-max-ms: 1000
```

## 📝 마이그레이션 가이드

Python 프로젝트에서 마이그레이션 시 참고:
- `MIGRATION_GUIDE.md` 참조 (Python 프로젝트에 있음)
- 모든 기능이 1:1 매핑되어 구현됨

## 🤝 기여

이슈 및 PR 환영합니다.

## 📄 라이선스

Private Project
