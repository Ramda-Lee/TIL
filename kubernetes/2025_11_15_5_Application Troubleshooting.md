# 쿠버네티스

## 애플리케이션 트러블슈팅 개요

### 출발점과 맵 그리기
- 웹 서비스, DB 서비스/Pod 등 구성요소와 연결 관계를 간단히 그려두고 한 지점씩 확인
- 사용자 이슈가 있다면 프런트엔드부터(또는 백엔드부터) 추적하되, 맵의 모든 연결을 점검

### 프런트엔드 접근 확인
- NodePort 또는 Ingress 등 외부 진입점으로 접근 시도: `curl http://<NodeIP>:<NodePort>`
- 실패 시 네트워크/보안그룹/방화벽보다 우선 서비스/엔드포인트 상태 점검

### Service 점검
- 서비스가 올바른 엔드포인트를 보유하는지: `kubectl get endpoints <svc>`
- 셀렉터 불일치 여부 확인: `kubectl get svc <svc> -o yaml` vs `kubectl get pod -l <same-labels>`

### Pod 상태/이벤트 확인
- 상태/재시작 횟수: `kubectl get pod -o wide`
- 상세 이벤트: `kubectl describe pod <pod>` (스케줄 실패, 이미지 풀 실패, 프로브 실패 등)

### 애플리케이션 로그 확인
- 현재 컨테이너 로그: `kubectl logs <pod> [-c <container>]`
- 재현 대기: `kubectl logs -f <pod>`
- 직전 크래시 원인: `kubectl logs --previous <pod>`

### 백엔드 체인 추적
- 웹 → DB 서비스 접근 여부 확인(서비스 IP/포트로 통신되는지)
- DB 서비스 엔드포인트와 DB Pod 상태/로그 점검

### 팁
- 다중 컴포넌트일수록 “서비스 셀렉터 ↔ Pod 라벨” 매칭 오류가 흔함
- 프로브(Readiness/Liveness) 실패는 재시작 루프로 이어질 수 있으므로 설정/로그를 함께 확인
- 공식 문서의 애플리케이션 트러블슈팅 가이드 참고 권장

