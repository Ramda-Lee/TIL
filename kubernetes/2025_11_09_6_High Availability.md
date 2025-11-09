# 쿠버네티스

## High Availability (HA) 설계

### 왜 HA인가
- 단일 마스터 장애 시: 기존 파드는 계속 동작하지만, 새 파드 재생성/스케줄링, 관리 API 접근(kubectl/API) 불가 → 점진적 장애로 확산.
- 프로덕션에서는 마스터/컨트롤 플레인, 워커, 애플리케이션까지 단일 장애점을 제거해야 한다.

### 컨트롤 플레인 구성 요약
- 구성요소: `kube-apiserver`, `kube-controller-manager`, `kube-scheduler`, `etcd`.
- 다중 마스터 구성 시 동작 모드가 다르다.

### API Server: Active-Active + 로드 밸런서
- 모든 `kube-apiserver` 인스턴스는 동시에 활성(Active-Active)로 요청 처리 가능.
- 클라이언트(`kubectl`, 컨트롤러)는 마스터 개별 주소 대신 LB를 바라보게 한다.
  - 예: L4/L7 LB(nginx/HAProxy/클라우드 LB) → 각 마스터의 6443 포트로 분산.
- kubeconfig의 서버 엔드포인트를 LB로 지정한다.

### Scheduler/Controller-Manager: Active-Standby(리더 선출)
- 병렬 실행 시 중복 작업/경합 위험 → 리더 선출로 1개만 활성, 나머지는 대기.
- 공통 옵션(기본값 포함):
  - `--leader-elect=true`
  - `--leader-elect-lease-duration=15s`
  - `--leader-elect-renew-deadline=10s`
  - `--leader-elect-retry-period=2s`
- 리더는 Kubernetes 오브젝트(엔드포인트/리소스)에 락을 갱신하며, 실패 시 대기 노드가 승계.

### etcd 토폴로지: Stacked vs External
- Stacked(마스터와 동노드):
  - 장점: 구축/운영 단순, 노드 수 적음.
  - 단점: 마스터 노드 장애 시 해당 etcd 멤버도 동반 상실 → 중복성 저하.
- External(전용 노드):
  - 장점: 컨트롤 플레인 장애가 etcd에 직접적 영향 덜함, 데이터 안전성↑.
  - 단점: 초기 구축 복잡, 노드 수 증가(운영 비용↑).
- `kube-apiserver`는 유일하게 etcd와 직접 통신하는 컴포넌트로, 다중 엔드포인트를 지정해 접근한다.
  - 주요 옵션: `--etcd-servers=https://<etcd1>:2379,https://<etcd2>:2379,...`
  - etcd는 분산 저장소이므로 가용 멤버 중 어느 곳으로도 읽기/쓰기가 가능(합의에 따라 커밋).

### 최종 설계 스케치(예)
- 기존 계획: 1 마스터 + 2 워커(3노드).
- HA 반영: 마스터 2대로 확장 + API Server 앞단 LB 1대 → 총 5노드 구성.
  - 마스터×2: API Server Active-Active, 스케줄러/컨트롤러매니저 리더 선출.
  - 워커×2: 애플리케이션/서비스 호스팅.
  - LB×1: 6443 요청 분산(클라우드 LB/온프렘 LB/소프트웨어 LB 선택).
  - etcd: PoC는 Stacked, 프로덕션은 External 권장(규모/신뢰성 요구에 따라).

### 운영 체크포인트
- LB 헬스체크: 6443 상태 점검, 타임아웃/백오프 파라미터 튜닝.
- 컨트롤러/스케줄러 로그에서 리더 전환 이벤트 모니터링.
- etcd 스냅샷/복구 절차 정기 검증, 멤버 헬스 확인.
- kubeconfig와 컨트롤러 매니저/스케줄러 매니페스트의 플래그 일관성 유지.

