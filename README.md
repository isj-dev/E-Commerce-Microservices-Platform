# E-Commerce Microservices Platform

Spring Boot 기반의 마이크로서비스 아키텍처(MSA)로 구현한 이커머스 플랫폼입니다.  
서비스 디스커버리, 중앙화된 설정 관리, 이벤트 기반 분산 트랜잭션, 서킷 브레이커, 분산 추적 등 프로덕션 수준의 MSA 패턴을 적용했습니다.

<br>

## 목차

- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [서비스 구성](#서비스-구성)
- [주요 구현 내용](#주요-구현-내용)
- [실행 방법](#실행-방법)
- [API 명세](#api-명세)
- [모니터링](#모니터링)
- [프로젝트 구조](#프로젝트-구조)

<br>

## 기술 스택

### Backend
| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.1.0, Spring Cloud 2025.1.1 (Oakwood) |
| Build | Gradle 9 (Multi-module) |
| ORM | Spring Data JPA, QueryDSL 5.1 |
| Security | Spring Security, JWT (jjwt 0.12.6) |
| API Docs | SpringDoc OpenAPI 3.0.3 |

### Spring Cloud
| 컴포넌트 | 기술 |
|----------|------|
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Service Discovery | Netflix Eureka |
| Config Server | Spring Cloud Config (Native) |
| Load Balancer | Spring Cloud LoadBalancer |
| Circuit Breaker | Resilience4J |
| Service Communication | Spring Cloud OpenFeign |

### Messaging & Storage
| 분류 | 기술 |
|------|------|
| Message Broker | Apache Kafka |
| Relational DB | PostgreSQL 17 |
| Cache | Redis 7 |

### Observability
| 분류 | 기술 |
|------|------|
| Metrics | Micrometer + Prometheus |
| Distributed Tracing | Zipkin (Micrometer Tracing / B3) |
| Dashboard | Grafana |

### DevOps
| 분류 | 기술 |
|------|------|
| Container | Docker, Docker Compose |
| CI | GitHub Actions |

<br>

## 시스템 아키텍처

```
                              ┌─────────────────────────────────────────────────────────┐
                              │                     Client (Browser)                    │
                              └───────────────────────────┬─────────────────────────────┘
                                                          │ HTTP :9000
                              ┌───────────────────────────▼─────────────────────────────┐
                              │                      API Gateway                         │
                              │         JWT Filter │ CORS │ Load Balancing              │
                              │         OpenAPI Server Rewrite Filter                    │
                              └──┬──────────┬──────────┬──────────┬──────────┬──────────┘
                                 │          │          │          │          │
                    ┌────────────▼──┐  ┌────▼──────┐  ┌─▼──────┐  ┌────────▼──┐  ┌────▼──────┐
                    │ User Service  │  │ Product   │  │ Order  │  │ Payment   │  │   Cart    │
                    │    :8081      │  │ Service   │  │Service │  │ Service   │  │  Service  │
                    │               │  │  :8082    │  │ :8083  │  │  :8084    │  │   :8085   │
                    └──────┬────────┘  └─────┬─────┘  └───┬────┘  └────┬──────┘  └────┬──────┘
                           │                 │             │            │               │
                           │          ┌──────▼──────┐      │ Feign+CB   │               │
                           │          │  QueryDSL   │◄─────┘            │               │
                           │          │  (search)   │                   │               │
                    ┌──────▼──────┐   └─────────────┘                   │          ┌────▼──────┐
                    │ PostgreSQL  │                                       │          │   Redis   │
                    │  userdb     │                              ┌────────▼──────┐   │  (cart)   │
                    │  productdb  │                              │  PostgreSQL   │   └───────────┘
                    │  orderdb    │                              │  paymentdb    │
                    │  paymentdb  │                              └───────────────┘
                    └─────────────┘
                                                  Kafka Topics
                              ┌────────────────────────────────────────────────────────┐
                              │  order-events ──────────────────────────────────────►  │
                              │                                            Payment Svc  │
                              │  payment-events ◄───────────────────────────────────── │
                              │       │                                                 │
                              │       ├──► Order Service (상태 업데이트 / 재고 복구)    │
                              │       └──► Notification Service (결제 알림)             │
                              └────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────────────────────────────────────┐
                    │                    Spring Cloud Infrastructure                  │
                    │   Eureka (:8761)   Config Server (:8888)   Zipkin (:9411)       │
                    │   Prometheus (:9090)   Grafana (:3000)                          │
                    └─────────────────────────────────────────────────────────────────┘
```

<br>

## 서비스 구성

| 서비스 | 포트 | 역할 | 주요 기술 |
|--------|------|------|-----------|
| **api-gateway** | 9000 | 단일 진입점, 라우팅, JWT 인증 | Spring Cloud Gateway (WebFlux) |
| **user-service** | 8081 | 회원가입, 로그인, 사용자 관리 | Spring Security, JWT, JPA |
| **product-service** | 8082 | 상품 CRUD, 재고 관리, 검색 | JPA, QueryDSL |
| **order-service** | 8083 | 주문 생성/취소, Saga 오케스트레이션 | Kafka, OpenFeign, Resilience4J |
| **payment-service** | 8084 | 결제 처리 (90% 성공 시뮬레이션) | Kafka |
| **cart-service** | 8085 | 장바구니 관리 | Redis |
| **notification-service** | 8086 | 결제 완료/실패 알림 | Kafka, Spring Mail |
| **discovery-service** | 8761 | 서비스 레지스트리 | Netflix Eureka |
| **config-service** | 8888 | 중앙화된 설정 관리 | Spring Cloud Config |
| **common** | — | 공통 DTO, 예외, 이벤트 클래스 | — |

<br>

## 주요 구현 내용

### 1. Choreography Saga 패턴 (분산 트랜잭션)

Kafka 이벤트를 통해 서비스 간 데이터 일관성을 유지합니다. 결제 실패 시 재고를 자동으로 복구하는 보상 트랜잭션을 구현했습니다.

```
① 주문 생성      ② 재고 감소          ③ Kafka 발행           ④ 결제 처리
Order Service ──► Product Service ──► order-events ──────► Payment Service
    (PENDING)       (OpenFeign)                                 (90% 성공)
                                                                    │
                                                            payment-events
                                                                    │
⑤-A 결제 성공 ◄──────────────────────────────────────────────────┘
   Order: PAID
   Notification: 주문 확인 메시지

⑤-B 결제 실패 ◄──────────────────────────────────────────────────┘
   Order: CANCELLED + 재고 복구 (보상 트랜잭션)
   Notification: 결제 실패 메시지
```

### 2. Circuit Breaker (Resilience4J)

Order Service에서 Product Service 호출 시 Circuit Breaker를 적용해 장애 전파를 차단합니다.

```yaml
# order-service 설정
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10          # 10회 요청 기준
        failure-rate-threshold: 50       # 50% 이상 실패 시 오픈
        wait-duration-in-open-state: 10s # 10초 후 Half-Open
        permitted-number-of-calls-in-half-open-state: 3
```

**상태 전이**: `CLOSED` → `OPEN` (50% 실패) → `HALF_OPEN` (10초 후) → `CLOSED` (복구)

### 3. API Gateway 패턴

**JWT 인증 필터**: 토큰 검증 후 `X-User-Id`, `X-User-Email` 헤더를 하위 서비스에 전달합니다.

```
Client ──[Authorization: Bearer {token}]──► Gateway
                                              │
                                         JWT 검증
                                              │
                                ┌─────────────▼──────────────┐
                                │ X-User-Id: 1               │
                                │ X-User-Email: user@...     │──► 하위 서비스
                                └────────────────────────────┘
```

**Swagger UI 통합**: Gateway의 `OpenApiServerRewriteFilter`가 각 서비스의 OpenAPI spec에서 `servers` 필드를 Gateway URL로 재작성합니다. Swagger UI에서 모든 서비스의 API를 단일 Gateway를 통해 테스트할 수 있습니다.

### 4. 상품 검색 (QueryDSL)

카테고리 필터링과 키워드 검색을 QueryDSL 동적 쿼리로 구현했습니다.

```java
// 카테고리, 검색어를 조합한 동적 쿼리
BooleanBuilder builder = new BooleanBuilder();
if (category != null) builder.and(product.category.eq(category));
if (search != null)   builder.and(product.name.containsIgnoreCase(search));
```

### 5. 분산 추적 (Zipkin)

B3 헤더 전파로 Gateway → 각 서비스 간의 요청 흐름을 추적합니다.

```
Request ──► Gateway ──► Order Service ──► Product Service (Feign)
              │              │                   │
         [TraceID: abc]  [TraceID: abc]    [TraceID: abc]
                                                         ▼
                                              Zipkin (전체 흐름 시각화)
```

<br>

## 실행 방법

### 사전 요구사항

- Java 17
- Docker Desktop
- Git

### Docker Compose로 전체 실행 (권장)

```bash
# 1. 저장소 클론
git clone https://github.com/{username}/ecommerce.git
cd ecommerce

# 2. 빌드 및 실행 (Windows)
docker-build.bat

# 또는 단계별 실행
./gradlew bootJar -x test --parallel
docker-compose up --build -d
```

> 전체 서비스가 준비되는 데 약 2~3분 소요됩니다.

### 접속 정보

| 서비스 | URL | 계정 |
|--------|-----|------|
| **API Gateway** | http://localhost:9000 | — |
| **Swagger UI** | http://localhost:9000/swagger-ui.html | — |
| **Eureka Dashboard** | http://localhost:8761 | — |
| **Grafana** | http://localhost:3000 | admin / admin123 |
| **Prometheus** | http://localhost:9090 | — |
| **Zipkin** | http://localhost:9411 | — |
| **Kafka UI** | http://localhost:8090 | — |
| **Loki** | http://localhost:3100 | — |

### 로컬 개발 환경 (인프라만 Docker)

인프라 서비스는 Docker로, Spring Boot 서비스는 IDE에서 실행하는 방식입니다.

```bash
# 인프라만 실행
docker-compose up -d postgres redis kafka zipkin prometheus grafana

# IDE에서 서비스 실행 순서
# 1. discovery-service
# 2. config-service
# 3. user-service, product-service, order-service, payment-service, cart-service, notification-service
# 4. api-gateway

# 또는 PowerShell 스크립트 사용
./start-all.ps1
./start-all.ps1 -Build  # 빌드 후 실행
```

<br>

## API 명세

Swagger UI에서 전체 API를 확인하고 테스트할 수 있습니다: **http://localhost:9000/swagger-ui.html**

### 인증

JWT Bearer 토큰 방식을 사용합니다.

```bash
# 1. 회원가입
POST /api/v1/users/signup
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}

# 2. 로그인 → accessToken 발급
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

# 3. 이후 요청에 토큰 포함
Authorization: Bearer {accessToken}
```

### 주요 엔드포인트

**User API** — `/api/v1`
```
POST   /users/signup            회원가입
POST   /auth/login              로그인 (JWT 발급)
GET    /me                      내 정보 조회 [인증]
GET    /users/{userId}          사용자 조회
```

**Product API** — `/api/v1`
```
POST   /products                상품 등록
GET    /products                상품 목록 (페이징, 카테고리/검색 필터)
GET    /products/{id}           상품 상세
PUT    /products/{id}           상품 수정
DELETE /products/{id}           상품 삭제
PUT    /products/{id}/stock/decrease   재고 감소
PUT    /products/{id}/stock/increase   재고 증가
```

**Order API** — `/api/v1` [인증 필요]
```
POST   /orders                  주문 생성 → Saga 시작
GET    /orders                  내 주문 목록 (페이징)
GET    /orders/{id}             주문 상세
POST   /orders/{id}/cancel      주문 취소
```

**Payment API** — `/api/v1` [인증 필요]
```
GET    /payments/orders/{orderId}   주문 결제 정보 조회
```

**Cart API** — `/api/v1` [인증 필요]
```
GET    /cart                    장바구니 조회
POST   /cart/items              상품 추가
PUT    /cart/items/{productId}  수량 수정
DELETE /cart/items/{productId}  상품 제거
DELETE /cart                    장바구니 비우기
```

### 응답 형식

모든 API는 통일된 응답 형식을 사용합니다.

```json
// 성공
{
  "success": true,
  "message": "요청이 처리되었습니다",
  "data": { ... }
}

// 실패
{
  "code": "P001",
  "message": "상품을 찾을 수 없습니다",
  "timestamp": "2026-06-14T13:00:00"
}
```

**에러 코드**

| 코드 | 설명 |
|------|------|
| C001 | 잘못된 입력값 |
| C002 | 리소스를 찾을 수 없음 |
| U001 | 사용자를 찾을 수 없음 |
| U002 | 이메일 중복 |
| U003 | 잘못된 비밀번호 |
| U004 | 인증 필요 |
| P001 | 상품을 찾을 수 없음 |
| P002 | 재고 부족 |
| P003 | 상품 서비스 이용 불가 (Circuit Breaker) |
| O001 | 주문을 찾을 수 없음 |
| O002 | 취소 불가능한 주문 |
| PAY001 | 결제 실패 |
| PAY002 | 결제 정보를 찾을 수 없음 |

<br>

## 모니터링

### Grafana 대시보드

http://localhost:3000 (admin / admin123)

Docker Compose 시작 시 **Grafana Provisioning**으로 데이터소스와 대시보드가 자동 적재됩니다.

| 패널 | 설명 |
|------|------|
| HTTP 요청 처리량 (RPS) | 서비스별 초당 요청 수 |
| HTTP 평균 응답시간 | 서비스별 p50 응답시간 |
| HTTP 에러율 (5xx) | 서비스별 에러 비율 |
| JVM 힙 메모리 | 서비스별 힙 사용량 |
| DB 커넥션 풀 (HikariCP) | Active / Idle 커넥션 수 |
| Kafka 리스너 처리량 | 토픽별 메시지 처리 속도 |
| JVM GC 횟수 | 서비스별 GC 발생 빈도 |
| CPU 사용률 | 서비스별 CPU 점유율 |
| Circuit Breaker 상태 | CLOSED / OPEN / HALF_OPEN 상태 |
| Circuit Breaker 실패율 | 실패율 및 차단된 호출 수 |

### Zipkin 분산 추적

http://localhost:9411

서비스 간 요청 흐름을 트레이스 단위로 시각화합니다.

```
Gateway (2ms) → Order Service (45ms) → Product Service (12ms)
                      └── Kafka Publish → Payment Service (async)
```

### Prometheus 메트릭 수집

http://localhost:9090

9개 서비스의 `/actuator/prometheus` 엔드포인트에서 15초 간격으로 메트릭을 수집합니다.

<br>

## 프로젝트 구조

```
ecommerce/
├── common/                          # 공통 모듈
│   └── src/main/java/com/isj/common/
│       ├── dto/
│       │   ├── ApiResponse.java     # 통일된 API 응답 래퍼
│       │   └── ErrorResponse.java
│       ├── event/
│       │   ├── OrderEvent.java      # Kafka 주문 이벤트 DTO
│       │   └── PaymentEvent.java    # Kafka 결제 이벤트 DTO
│       └── exception/
│           ├── BusinessException.java
│           ├── ErrorCode.java       # 에러 코드 열거형
│           └── GlobalExceptionHandler.java
│
├── discovery-service/               # Eureka Service Registry (:8761)
├── config-service/                  # Spring Cloud Config Server (:8888)
│   └── src/main/resources/configs/  # 서비스별 설정 파일
│       ├── user-service.yml
│       ├── product-service.yml
│       ├── order-service.yml
│       ├── payment-service.yml
│       ├── cart-service.yml
│       └── notification-service.yml
│
├── api-gateway/                     # API Gateway (:9000)
│   └── src/main/java/com/isj/gateway/
│       └── filter/
│           ├── JwtAuthenticationFilter.java   # JWT 검증, 헤더 주입
│           └── OpenApiServerRewriteFilter.java # Swagger servers 재작성
│
├── user-service/                    # 사용자 서비스 (:8081)
├── product-service/                 # 상품 서비스 (:8082)
├── order-service/                   # 주문 서비스 (:8083)
│   └── src/main/java/com/isj/order/
│       ├── client/ProductClient.java       # Feign Client
│       └── listener/OrderSagaListener.java # Kafka Consumer (Saga)
├── payment-service/                 # 결제 서비스 (:8084)
├── cart-service/                    # 장바구니 서비스 (:8085, Redis)
├── notification-service/            # 알림 서비스 (:8086)
│
├── monitoring/
│   ├── prometheus.yml               # 로컬 개발용 Prometheus 설정
│   ├── prometheus-docker.yml        # Docker 환경용 Prometheus 설정
│   ├── grafana/
│   │   ├── provisioning/
│   │   │   ├── datasources/prometheus.yml  # 데이터소스 자동 설정
│   │   │   └── dashboards/dashboard.yml    # 대시보드 경로 지정
│   │   └── dashboards/ecommerce.json       # 대시보드 정의
│   └── upload_dashboard.py          # 로컬 개발용 대시보드 업로드 스크립트
│
├── scripts/
│   └── init-db.sql                  # PostgreSQL DB 초기화
│
├── docker-compose.yml               # 전체 인프라 + 서비스 구성
├── docker-build.bat                 # Windows 원클릭 빌드 & 실행
├── start-all.ps1                    # 로컬 개발용 서비스 기동 스크립트
├── build.gradle                     # 루트 Gradle 설정
└── settings.gradle                  # 멀티모듈 정의
```

<br>

## 개발 환경 설정

### 환경 변수 (Docker 실행 시 자동 주입)

| 변수 | 값 | 대상 서비스 |
|------|----|------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/{db}` | user, product, order, payment |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | order, payment, notification |
| `SPRING_DATA_REDIS_HOST` | `redis` | cart |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://discovery-service:8761/eureka/` | 전체 |
| `SPRING_CONFIG_IMPORT` | `optional:configserver:http://config-service:8888` | 전체 |
| `MANAGEMENT_TRACING_EXPORT_ZIPKIN_ENDPOINT` | `http://zipkin:9411/api/v2/spans` | 전체 |

### CI/CD(미구현)

GitHub Actions (`.github/workflows/ci.yml`)

- `main`, `develop` 브랜치 push 시 빌드 및 테스트 실행
- `main` 브랜치 push 시 9개 서비스 Docker 이미지 빌드
- Testcontainers 기반 통합 테스트 (user-service, product-service)
