# 쿠버네티스

## 워커 노드 트러블슈팅

### 1) 노드 상태 1차 점검
- 노드 리스트/상태: `kubectl get nodes -o wide`
- 상세 확인: `kubectl describe node <node>`
  - Conditions(참/거짓/Unknown):
    - OutOfDisk, DiskPressure, MemoryPressure, PIDPressure
    - Ready=true 이면 정상, 통신 두절 시 Unknown으로 전환
  - LastHeartbeatTime으로 장애 시점 추정

### 2) 노드 자체 확인(호스트 관점)
- 온라인 여부/재부팅 필요 여부 확인(콘솔/클라우드 메트릭)
- 자원 상태: CPU/메모리/디스크 여유(`top`, `free -h`, `df -h`)
- 네트워크: 기본 게이트웨이/ DNS / 방화벽 규칙 점검

### 3) Kubelet 상태/로그
- 서비스 상태: `systemctl status kubelet`
- 로그: `journalctl -u kubelet -e --no-pager`
- 설정 확인: `/var/lib/kubelet/config.yaml` (clusterDNS, authentication, cgroup 등)
- 런타임 연동: containerd/docker 동작 확인
  - `systemctl status containerd` / `crictl info` / `ctr c list`

### 4) 인증서/자격 증명
- kubelet 클라이언트 인증서 만료/발급자 확인
  - 경로 예: `/var/lib/kubelet/pki/kubelet-client-current.pem`
  - 확인: `openssl x509 -in <pem> -noout -text`
  - Organization이 `system:nodes`, CN이 `system:node:<node>` 인지 확인
- kubeconfig 경로: `/etc/kubernetes/kubelet.conf`

### 5) 디스크/이미지/컨테이너 런타임 이슈
- 이미지/레이어 누적으로 디스크 부족(DiskPressure) → 불필요 이미지/컨테이너 정리
  - `crictl images` / `crictl rmi <image>`
  - 워크로드 로그/아티팩트가 루트 디스크를 채우지 않는지 확인

### 6) CNI/네트워킹 확인
- CNI 플러그인/브리지 상태: `ip link`, `ip addr`, `ip route`
- CNI 로그(있을 경우) 및 `kube-proxy` 상태
  - `kubectl -n kube-system get ds kube-proxy -o wide`
  - iptables/ipvs 규칙 충돌 여부

### 7) 흔한 증상과 조치 힌트
- NotReady + NetworkUnavailable: CNI 초기화 실패, CNI 바이너리/설정(`/opt/cni/bin`, `/etc/cni/net.d`) 점검
- NotReady + DiskPressure: 디스크 확보 후 kubelet 재시작
- Unknown: 노드 통신 두절(호스트 다운/네트워크 단절). 복구 후 `Ready` 전환 확인
- 워크로드 스케줄 실패: 노드 taint/자원 부족, Pod `describe`로 이유 확인

위 순서로 노드 상태 → 호스트 자원/네트워크 → kubelet/런타임 → 인증서 → CNI 순으로 폭넓게 확인하면 원인 축소가 빠르다. 추가 팁은 Kubernetes 공식 트러블슈팅 문서를 참고한다.

