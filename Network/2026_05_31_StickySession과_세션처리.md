# sticky session과 세션 처리

세션을 서버 메모리에 두는 구조의 문제

- 전통적인 세션 방식: HTTP는 본질적으로 stateless 프로토콜로 로그인 상태 같은 걸 유지하려면 별도의 매커니즘이 필요하다 → 서버 메모리 기반 세션 방식 (JVM 프로세스의 heap 메모리에 저장됨)
- 메모리 기반 세션 방식은 서버가 1대일 경우 문제가 없다 → 로드밸런서 사용 시 세션이 사라짐
- 이 문제를 해결하기 위한 첫번째 시도가 sticky session이다.

L7 sticky (쿠키 기반, ALB) vs L4 sticky (소스 IP 기반, NLB) 차이

- sticky session은 같은 클라이언트의 요청은 항상 같은 서버로 보내자는 라우팅 정책
- 비교표
    
    
    | 구분 | L4 Sticky | L7 Sticky |
    | --- | --- | --- |
    | 식별 기준 | 소스 IP | 쿠키 |
    | 동작 계층 | TCP/IP 헤더 | HTTP 헤더 |
    | 대표 LB | NLB, HAProxy(L4 모드) | ALB, nginx, HAProxy(L7 모드) |
    | 정확도 | NAT/프록시 환경에서 부정확 | 클라이언트별 정확 |
    | 오버헤드 | 매우 낮음 | HTTP 파싱 필요 |
    | HTTPS 처리 | TLS 통과 가능 | TLS 종료 필요 |
- L4 Sticky 동작 방식
    
    ```bash
    Client (IP: 1.2.3.4)
       │
       ↓
    ┌─────────────┐
    │    NLB      │  해시 함수: hash(source_ip) % server_count
    │             │  hash("1.2.3.4") % 3 = 1
    └──────┬──────┘
           │ → 항상 Server B로 라우팅
           ↓
       Server B
    ```
    
    특징
    
    - 클라이언트 쪽에서 아무것도 안해도 된다.(쿠키 없음)
    - LB가 IP헤더만 보고 즉시 결정
    - IP가 바뀌면 다른 서버로 라우팅됨
- L7 Sticky 동작 방식
    
    ```bash
    [첫 번째 요청]
    Client → ALB → Server A (라운드 로빈으로 선택)
    ALB가 응답에 쿠키 주입:
      Set-Cookie: AWSALB=encrypted(server_A_id)
    
    [두 번째 요청]
    Client가 쿠키를 다시 보냄:
      Cookie: AWSALB=encrypted(server_A_id)
    ALB가 쿠키 복호화 → Server A로 라우팅
    ```
    
    특징
    
    - 쿠키로 정확한 클라이언트 식별
    - NAT 뒤 수천 명도 각자 다른 쿠키 → 분산 가능
    - HTTPS는 ALB에서 TLS 종료 필요(HTTP 헤더를 봐야해서)

nginx로 쿠키 기반 sticky 구현 (sticky cookie)

- sticky 없는 기본 구성 (라운드 로빈이라 매 요청마다 다른 서버로 분산)
    
    ```bash
    upstream backend {
        server 10.0.1.10:8080;
        server 10.0.1.11:8080;
        server 10.0.1.12:8080;
    }
    
    server {
        listen 80;
        
        location / {
            proxy_pass http://backend;
        }
    }
    ```
    
- ip_hash (L4 sticky랑 유사)
    
    ```bash
    upstream backend {
        ip_hash;
        server 10.0.1.10:8080;
        server 10.0.1.11:8080;
        server 10.0.1.12:8080;
    }
    ```
    
- sticky cookie (nginx plus 버전만 적용 가능)
    
    ```bash
    upstream backend {
        server 10.0.1.10:8080;
        server 10.0.1.11:8080;
        server 10.0.1.12:8080;
        
        sticky cookie srv_id expires=1h domain=.example.com path=/;
    }
    
    # 동작
    [첫 요청]
    Client → nginx → Server A 선택
    응답: Set-Cookie: srv_id=ABC123; expires=...; domain=.example.com; path=/
    
    [두 번째 요청]
    Client: Cookie: srv_id=ABC123
    nginx가 srv_id 보고 Server A로 라우팅
    ```
    

무상태 설계 패턴 (JWT, Redis 세션 스토어)

sticky session으로 우회하는 방식의 한계가 명확하다면, 근본 해결책은 세션을 서버 메모리에 두지 않는 것

- Redis 세션 스토어: 세션을 서버 메모리가 아닌 Redis에 저장한다.
    
    ```bash
    										┌────────────────┐
                        │       LB       │ (sticky 불필요!)
                        └───────┬────────┘
                                │
                  ┌─────────────┼─────────────┐
                  ↓             ↓             ↓
            ┌─────────┐   ┌─────────┐   ┌─────────┐
            │Server A │   │Server B │   │Server C │
            └────┬────┘   └────┬────┘   └────┬────┘
                 │             │             │
                 └─────────────┼─────────────┘
                               ↓
                        ┌────────────┐
                        │   Redis    │
                        │ sessions:  │
                        │  abc123    │
                        └────────────┘
                        
                        
    
    # springboot 적용
    spring:
      session:
        store-type: redis
        timeout: 30m
      data:
        redis:
          host: redis.internal
          port: 6379
    ```
    
- JWT: 세션 자체를 없애는 접근이다. 서버는 상태를 저장하지 않고, 클라이언트가 매 요청마다 인증 정보를 들고 온다.
    
    ```bash
    eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMDF9.signature
    └──── header ────┘└──── payload ─────┘└signature┘
         (base64)         (base64)
    
    [payload 디코딩]
    {
      "userId": 1001,
      "exp": 1719648000
    }
    
    [signature]
    HMAC_SHA256(
      base64(header) + "." + base64(payload),
      secret_key
    )
    
    # 동작 방식
    [로그인]
    Client → Server: POST /login
    Server:
      1. 인증 확인
      2. JWT 생성
      3. JWT 반환
    Server 메모리: 아무것도 저장 안 함 ⭐
    
    [이후 요청]
    Client → Server: GET /my-orders
      Header: Authorization: Bearer eyJhbGc...
    Server:
      1. JWT signature 검증
      2. payload에서 userId 추출
      3. 만료 시간 확인
      4. 비즈니스 로직 수행
    ```
    
- 실제 실무 적용 시 많이 사용하는 방법 (Hybrid)
    
    ```bash
    [Access Token (JWT)]
      - 짧은 만료 (15분~1시간)
      - 무상태 인증/인가
      - 매 API 호출에 사용
    
    [Refresh Token]
      - 긴 만료 (7~30일)
      - Redis에 저장
      - Access Token 갱신용
      
    # 동작 방식
    [일반 요청]
    Client → Server: Bearer <access_token>
    Server: JWT 검증만 (무상태)
    
    [Access Token 만료 시]
    Client → Server: POST /refresh, Refresh Token
    Server:
      1. Redis에서 Refresh Token 존재 확인
      2. 새 Access Token 발급
    
    [로그아웃]
    Server: Redis에서 Refresh Token 삭제
      → 새 Access Token 발급 불가
      → 기존 Access Token은 자연 만료까지 유효
    ```
    

ELB/ALB의 AWSALB 쿠키 동작 이해

- ALB-generated cookie: ALB가 자동으로 쿠키 생성/관리
- Application-based cookie: 애플리케이션이 발급한 쿠키를 ALB가 활용

```bash
[첫 번째 요청]
Client → ALB → Target B 선택
ALB가 응답에 쿠키 주입:
  Set-Cookie: AWSALB=encrypted_target_info;
              Expires=Wed, 28-May-2026 12:00:00 GMT
  Set-Cookie: AWSALBCORS=encrypted_target_info;
              SameSite=None; Secure

[두 번째 요청]
Client: Cookie: AWSALB=encrypted_target_info
ALB가 쿠키 복호화:
  - target_arn 확인
  - 만료 시간 확인
  - Target healthy 여부 확인
ALB → Target B로 라우팅
```

ALB 쿠키의 한계 → sticky session이 임시방편인 이유, 세션을 다른곳에 저장해야 진짜 해결된다.

1. Target 장애 시 세션 손실
    
    ```bash
    Target B 장애 → unhealthy
    Client 쿠키는 여전히 Target B 가리킴
    ALB가 다른 healthy Target으로 라우팅
    → 새 Target에 세션 없음 → 로그아웃
    ```
    
2. 새 Target 추가 시 불균형
    
    ```bash
    기존 A/B/C에 sticky로 사용자 분산되어 있음
    새 Target D 추가
    → 새 사용자만 D로 갈 수 있음
    → D는 한가, A/B/C는 부하 높음
    ```
    
3. 배포 시 영향
    
    ```bash
    Rolling deployment:
      Target A 종료 → 거기 묶여있던 사용자 모두 다른 서버로
      → 새 서버에 세션 없음 → 재로그인
    ```
    

**핵심 질문:** "NAT 뒤에 있는 수천 명의 사용자가 같은 공인 IP를 쓸 때, L4 sticky는 어떻게 동작하는가?"

- L4 sticky는 클라이언트 IP가 충분히 분산된 환경에서만 동작