# AWS ALB / NLB / GWLB 매핑

**목표:** 직접 구현한 nginx 개념들을 AWS 콘솔에서 1:1로 찾아내고, 각 LB가 언제 적합한지 설명할 수 있다

ALB 구성 요소 매핑 (리스너 / 리스너 룰 / 타겟 그룹 / 타겟)

| nginx 개념 | ALB 구성 요소 | 역할 |
| --- | --- | --- |
| `listen 443` | **리스너(Listener)** | 프로토콜/포트로 들어오는 연결 수신 |
| `location /api`, `if`, `server_name` | **리스너 룰(Listener Rule)** | 경로/호스트/헤더 기반 조건 매칭 |
| `upstream backend { ... }` | **타겟 그룹(Target Group)** | 트래픽을 보낼 백엔드 묶음 + 헬스체크 단위 |
| `server 10.0.0.1:8080` | **타겟(Target)** | 실제 백엔드 인스턴스/IP/Lambda |

NLB 구성 요소와 Elastic IP, 가용영역별 고정 IP 이해

- NLB(L4)에서 동작 ALB와 다른 점이 IP 고정이라는 것
- 가용영역별 고정 IP: NLB는 활성화한 각 AZ마다 네트워크 인터페이스 하나를 가지며 거기에 IP가 붙는다.
- Elastic IP 할당 가능: NLB 생성 시 AZ별로 EIP를 직접 지정할 수 있다. → IP가 절대 안바뀜
- ALB는 EIP 할당 불가. ALB의 IP는 AWS가 트래픽에 따라 동적으로 추가/제거하므로 DNS 이름으로만 접근해야한다.

GWLB의 존재 이유 (보안 어플라이언스용, GENEVE 캡슐화)

- GWLB는 서드파티 보안 어플라이언스(방화벽, IDS/IPS, DPI)를 투명하게 끼워넣기 위한 LB이다.
- L3에서 동작하며 모든 트래픽을 어플라이언스로 우회시킨 뒤 다시 원래 경로로 돌려보낸다.
- GENEVE 캡슐화(포트 6081):원본 패킷을 그대로 보존한 채 캡슐화해서 어플라이언스로 전달.
- 어플라이언스 입장에선 자인이 인라인에 있는지 모르게 트래픽이 흘러들어온다 → 확장/HA를 GWLB가 대신 처리

타겟 타입 3가지 (Instance / IP / Lambda)

- Instance: EC2 인스턴스 ID로 등록. 클라이언트 IP가 보존됨. 가장 일반적
- IP: IP 주소로 등록. 온프레므스 서버(Direct Connect/VPN 너머), 컨테이너, ALB가 못 보는 대상 등록 시. VPC 외부 IP도 가능.
- Lambda: ALB 전용. 함수를 직접 타겟으로. 서버리스 백엔드.

가중치 라우팅 - 카나리 배포

- ALB는 하나의 리스너 룰에서 여러 타겟 그룹에 가중치를 부여할 수 있다.
    
    ```jsx
    listener rule (forward):
      - target-group: prod-v1   weight: 95
      - target-group: canary-v2 weight: 5
    ```
    
    5%만 신버전으로 흘리고 지표 확인 후 점진적으로 95 → 50 → 0으로 조정. nginx의 upstream에서 server….weight=5로 가중치 주던 방식과 동일한 개념이지만, ALB는 타겟 그룹 단위로 가중치를 준다(서버 단위가 아님). stickiness를 켜면 동일 클라이언트가 같은 버전으로 고정되어 카나리 중 사용자 경험 일관성 확보
    

ALB가 자동 추가하는 헤더(X-Forwarded-*) 확인

- ALB는 리버스 프록시이므로 nginx에서 직접 넣던 proxy_set_header를 자동으로 처리한다.
- X-Forwarded-For: 원본 클라이언트 IP
- X-Forwarded-Proto: 원본 프로토콜 - 백엔드가 HTTPS 여부 판단
- X-Forwarded-Port: 원본 포트

LB 선택 기준 정리 (HTTP면 ALB, TCP/UDP면 NLB, 보안 어플라이언스면 GWLB)

- HTTP/HTTPS, 경로·호스트 기반 라우팅, 카나리/Lambda → ALB (L7)
- TCP/UDP, 초저지연, 초당 수백만 요청, 고정 IP 필요 → NLB (L4)
- 보안 어플라이언스 인라인 삽입 → GWLB (L3)

**핵심 질문:** "Elastic IP가 필요한 시스템에서 ALB 대신 NLB를 골라야 하는 이유는 무엇인가?"

ALB는 고정 IP를 가질 수 없기 때문이다.

ALB의 IP는 AWS가 트래픽 부하에 따라 백그라운드에서 동적으로 추가·교체 그래서 AWS는 ALB 접근 시 반드시 DNS 이름을 쓰라고 하고, EIP 할당 자체를 막아놈. 반면 NLB는 AZ별로 EIP를 명시적으로 붙일 수 있어 IP가 영구히 고정.