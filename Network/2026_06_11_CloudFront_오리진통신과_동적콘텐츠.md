# CloudFront 오리진 통신과 동적 콘텐츠
목표: 캐시 미스가 났을 때 오리진까지 가는 경로와, 캐시 안 되는 요청 처리 방식을 이해한다

오리진 종류 — S3 / ALB / 커스텀 오리진

- S3 오리진 — 정적 파일(JS/CSS/이미지) 저장소. 주의할 건 S3 *REST 엔드포인트*와 *웹사이트 엔드포인트*가 다르다는 점. OAC를 쓰려면 REST 엔드포인트여야 한다.
- 커스텀 오리진 — 임의의 HTTP(S) 서버. ALB, EC2, 온프레미스 WAS(JEUS) 모두 여기 해당.

OAC(Origin Access Control) — S3를 CloudFront 통해서만 접근 허용

S3 버킷을 퍼블릭으로 열어두면 CloudFront를 우회해 직접 접근이 가능한데 OAC는 이걸 막는다.

- 버킷은 비공개로 두고, CloudFront만 SigV4 서명으로 S3에 접근하도록 허용.
- 버킷 정책에 "이 distribution ARN을 가진 CloudFront 요청만 허용" 조건을 등록
- 예전 OAI(Origin Access Identity)의 후속이고, 지금은 OAC가 권장(SSE-KMS 지원 등).

동적 콘텐츠 처리 — 캐시 우회(pass-through) 동작

캐시 정책(Cache Policy)에서 `CachingDisabled` 또는 TTL=0으로 두면 CloudFront는 응답을 저장하지 않고 매 요청을 오리진까지 보낸다. 핵심: 오리진으로 전달 하는 것(Origin Request Policy) — 쿼리스트링, 쿠키, 헤더 전달 여부. 동적 요청은 보통 이걸 다 전달해야 한다.

여기서 CloudFront는 사실상 글로벌 엣지에 깔린 리버스 프록시 역할만 하게 된다. 캐시 이득은 없지만, 엣지~오리진 구간이 AWS 백본망이라 인터넷 직접 연결보다 빠르고 TLS 핸드셰이크를 엣지에서 끝낼 수 있다는 이점이 있다.

캐시 무효화(Invalidation) vs 파일 버전 관리(versioning) 비교

|  | Invalidation | 파일명 버전 관리 |
| --- | --- | --- |
| 방식 | `/js/app.js` 경로를 강제 무효화 | `app.a1b2c3.js`처럼 해시를 파일명에 |
| 반영 | 엣지 전파에 수초~수분 | 새 파일명이라 즉시(캐시 미스 자연 발생) |
| 비용 | 월 1,000 경로 무료, 이후 과금 | 무료 |
| 롤백 | 어려움 | 이전 파일명으로 가리키기만 하면 끝 |
| 캐시 | TTL 짧게 가져가야 안전 | TTL 1년도 가능(immutable) |

HTTPS 처리 — 엣지에서 TLS 종료

- 뷰어 ↔ 엣지: 여기서 TLS 종료. 인증서는 ACM, 반드시 us-east-1(버지니아)에 있어야 CloudFront가 붙일 수 있습니다.
- 엣지 ↔ 오리진: 별도 연결. Origin Protocol Policy로 HTTP/HTTPS/Match-Viewer 선택.

핵심 질문: "배포 후 변경한 JS 파일이 사용자에게 반영 안 될 때, Invalidation과 파일명 버전 관리 중 무엇이 더 나은가?"

대부분의 경우 파일명 버전 관리가 더 괜찮다. 이유는 세 가지.

1. 반영이 확실하고 즉시 — Invalidation은 엣지 전파 지연이 있고, 그 사이 사용자마다 구/신 버전이 섞여 들어갈 수 있습니다(일관성 깨짐). 새 파일명은 캐시에 없으니 무조건 오리진에서 새로 받는다.
2. 캐시를 공격적으로 쓸 수 있음 — 파일명이 내용과 1:1이므로 `Cache-Control: max-age=31536000, immutable`로 사실상 영구 캐시 가능. Invalidation 방식은 변경 가능성 때문에 TTL을 길게 못 잡는다.
3. 롤백·비용 우위 — 빌드 도구(Vite/Webpack)가 해시를 자동으로 붙여주고, 잘못 배포해도 참조만 되돌리면 된다. Invalidation 과금도 없음.

예외 상황, `index.html`처럼 "어떤 해시 파일을 불러올지 가리키는 진입점"은 파일명을 바꿀 수 없다. 그래서 실무 표준 조합은:

- 해시 붙은 JS/CSS/이미지 → 긴 TTL + 버전 관리
- `index.html` → 짧은 TTL(예: 0~60초) 또는 배포 시 그것만 Invalidation

즉 "에셋은 버전 관리, 진입점만 무효화"가 정답에 가깝다. 배포한 JS가 반영 안 됨이 만약 빌드 시 해시가 안 붙는 설정이라면, 그건 Invalidation으로 땜질하지 말고 빌드 파이프라인에서 해시 파일명을 켜는 게 근본 해결이다.