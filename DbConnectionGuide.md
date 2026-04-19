# DB 연결 주소가 왜 두 가지인가?

> `jdbc:mysql://localhost:3306` vs `jdbc:mysql://mysql:3306`
>
> 같은 `DbConfig.java` 를 쓰는데 **어떤 상황에서 어떤 주소를 써야 하는지** 이해하는 것이
> 이 프로젝트의 핵심 설정 포인트입니다.

---

## 1. 두 가지 실행 환경

이 프로젝트에는 MySQL에 접속하는 경우가 두 가지 있습니다.

```
┌─────────────────────────────────────────────────────────────┐
│  실행 환경 1: Docker (실제 애플리케이션 구동)               │
│  실행 환경 2: Unit Test (JUnit 테스트 직접 실행)            │
└─────────────────────────────────────────────────────────────┘
```

각 환경에서 MySQL의 **네트워크 위치**가 다릅니다.
이것이 JDBC URL 호스트 부분이 달라지는 이유입니다.

---

## 2. 환경 1 — Docker: `jdbc:mysql://mysql:3306`

### 네트워크 구조

```
┌─────────────────────────────────────────────────────────┐
│                  Docker 내부 네트워크                    │
│                                                         │
│   ┌──────────────────┐       ┌──────────────────────┐   │
│   │  spring-mvc-     │       │  spring-mvc-mysql    │   │
│   │  tomcat          │       │  (컨테이너 이름)     │   │
│   │                  │       │                      │   │
│   │  Spring MVC App  │──────►│  MySQL 9.1           │   │
│   │  (WAR 배포)      │       │  port: 3306          │   │
│   │                  │       │                      │   │
│   └──────────────────┘       └──────────────────────┘   │
│          호스트명: tomcat           호스트명: mysql       │
│                                                         │
└─────────────────────────────────────────────────────────┘
          ▲
          │  docker-compose.yml이 이 네트워크를 자동 생성
```

### 핵심 원리

Docker Compose는 같은 `docker-compose.yml` 안에 정의된 서비스들을
**자동으로 같은 가상 네트워크**에 연결합니다.

이 네트워크 안에서는 **서비스 이름이 곧 호스트명(DNS)**이 됩니다.

```yaml
# docker-compose.yml
services:
  mysql:           # ← 이 이름이 네트워크 내 호스트명이 됨
    image: mysql:9.1
    ...

  tomcat:
    image: tomcat:11.0-jdk21
    ...            # tomcat 컨테이너는 "mysql" 이라는 이름으로 MySQL에 접근
```

따라서 Tomcat 컨테이너 안의 Spring 앱이 MySQL에 접속할 때:

```
jdbc:mysql://mysql:3306/productdb
              ↑
              Docker 내부 DNS 이름 (서비스명)
              "localhost"가 아님!
```

### 왜 localhost가 안 되는가?

```
┌────────────────────────────────────────────────┐
│  Tomcat 컨테이너 내부에서 localhost = Tomcat   │
│                                                │
│  localhost:3306 → Tomcat 컨테이너 자신의 3306  │
│                   MySQL이 없음 → 연결 실패 ❌  │
└────────────────────────────────────────────────┘
```

Tomcat 컨테이너 안에서 `localhost`는 Tomcat 컨테이너 자신을 가리킵니다.
MySQL은 별도 컨테이너이므로 `localhost`로는 절대 접근할 수 없습니다.

---

## 3. 환경 2 — Unit Test: `jdbc:mysql://localhost:3306`

### 네트워크 구조

```
┌──────────────────────────────────────────────────────────┐
│  개발 PC (호스트 머신)                                   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  IntelliJ IDEA (JVM 프로세스)                    │   │
│  │                                                  │   │
│  │  JUnit Test ──────────────────────────────────►  │   │
│  │  EntityRelationshipTest                          │   │
│  │  ProductRepositoryTest          DbConfig.java    │   │
│  │                                 (DataSource)     │   │
│  └──────────────────────────────────────────────────┘   │
│                          │                               │
│                          │ localhost:3306                │
│                          ▼                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Docker 컨테이너 (spring-mvc-mysql)               │   │
│  │                                                  │   │
│  │  MySQL 9.1                                       │   │
│  │  포트 바인딩: 호스트 3306 → 컨테이너 3306         │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 핵심 원리

JUnit 테스트는 **IntelliJ 안에서 직접 실행**됩니다.
즉, 테스트 코드는 Docker 컨테이너 안이 아닌 **개발 PC(호스트)** 위에서 돌아갑니다.

MySQL 컨테이너는 `docker-compose.yml`에서 포트를 바인딩했습니다:

```yaml
# docker-compose.yml
mysql:
  ports:
    - "3306:3306"   # 호스트 3306 → 컨테이너 3306 으로 포워딩
```

따라서 호스트(개발 PC)에서 `localhost:3306`으로 접근하면
Docker가 자동으로 MySQL 컨테이너의 3306으로 연결해 줍니다.

```
jdbc:mysql://localhost:3306/productdb
              ↑
              개발 PC 자신 = Docker 포트 바인딩을 통해 MySQL 접근 가능
```

---

## 4. 두 환경 비교 한눈에 보기

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  구분            Docker 실행              Unit Test 실행
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  실행 주체       Tomcat 컨테이너           IntelliJ (개발 PC)
  네트워크        Docker 내부 네트워크      호스트 네트워크
  MySQL 위치      같은 네트워크의 컨테이너  포트 바인딩된 컨테이너
  호스트명        mysql (서비스명 = DNS)    localhost
  JDBC URL        mysql://mysql:3306        mysql://localhost:3306
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 5. 현재 프로젝트의 선택과 이유

현재 `DbConfig.java`는 `localhost`로 설정되어 있습니다.

```java
// DbConfig.java
ds.setUrl("jdbc:mysql://localhost:3306/productdb" + ...);
```

### 이 설정으로 두 환경이 모두 동작하는 이유

```
┌────────────────────────────────────────────────────────────┐
│  Unit Test (IntelliJ)                                      │
│  localhost:3306 → Docker 포트 바인딩 → MySQL ✅            │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│  Docker (Tomcat 컨테이너)                                   │
│  localhost:3306 → Tomcat 자신의 3306 → MySQL 없음 ❌       │
│                                                            │
│  → Tomcat 컨테이너에서는 localhost가 동작하지 않음!         │
└────────────────────────────────────────────────────────────┘
```

> **현재 프로젝트는 Unit Test 중심**으로 구성되어 있으므로 `localhost`를 사용합니다.
> Tomcat 컨테이너로 실제 배포까지 하려면 `mysql`로 변경하거나
> 환경별로 설정을 분리해야 합니다.

---

## 6. 실무에서의 해결 방법 — 환경별 설정 분리

실제 프로젝트에서는 환경에 따라 JDBC URL이 자동으로 바뀌도록 구성합니다.

### 방법 A: 환경 변수 사용

```java
// DbConfig.java
String dbHost = System.getenv("DB_HOST") != null
        ? System.getenv("DB_HOST")
        : "localhost";                       // 환경 변수 없으면 localhost (테스트용)

ds.setUrl("jdbc:mysql://" + dbHost + ":3306/productdb");
```

```yaml
# docker-compose.yml
tomcat:
  environment:
    DB_HOST: mysql                           # Docker 환경에서는 mysql로 설정
```

```
  Unit Test  → DB_HOST 없음 → localhost 사용
  Docker     → DB_HOST=mysql → mysql 사용
```

### 방법 B: Spring Profile 사용 (Spring Boot)

```yaml
# application-local.yml  (Unit Test / 로컬 개발)
spring.datasource.url: jdbc:mysql://localhost:3306/productdb

# application-docker.yml  (Docker 배포)
spring.datasource.url: jdbc:mysql://mysql:3306/productdb
```

---

## 7. 정리

```
  질문: "왜 JDBC URL 호스트가 환경마다 다른가?"

  답:  네트워크 관점에서 MySQL의 위치가 다르기 때문입니다.

  Unit Test (개발 PC에서 직접 실행)
  └─ 개발 PC의 localhost:3306
     └─ Docker 포트 바인딩(3306→3306)으로 MySQL 접근

  Docker (Tomcat 컨테이너 안에서 실행)
  └─ Docker 내부 DNS: mysql:3306
     └─ 같은 Compose 네트워크 안의 MySQL 컨테이너 직접 접근
        (localhost는 Tomcat 컨테이너 자신이므로 사용 불가)
```
