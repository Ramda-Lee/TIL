# 쿠버네티스

## 컨트롤 플레인 트러블슈팅

### 개요
클러스터 제어면(API Server, Controller Manager, Scheduler, etcd)과 노드 에이전트(Kubelet, Kube-proxy)의 상태를 점검해 장애 원인을 추적한다. 배포 방식에 따라 컨트롤 플레인 구성요소가 Pod(kubeadm 등) 또는 호스트 서비스(systemd)로 동작할 수 있다.

### 1) 클러스터/노드/파드 상태 1차 점검
- 노드 상태: `kubectl get nodes -o wide`
- 시스템 파드: `kubectl -n kube-system get pods -o wide`
- 이벤트 확인: `kubectl get events -A --sort-by=.lastTimestamp`

### 2) kubeadm 기반(컨트롤 플레인 = Static Pod)
- 대상 파드: `kube-apiserver-<master>`, `kube-controller-manager-<master>`, `kube-scheduler-<master>`, `etcd-<master>`
- 상태/로그:
  - `kubectl -n kube-system describe pod <name>`
  - `kubectl -n kube-system logs <apiserver|controller-manager|scheduler|etcd-pod>`
- 매니페스트 경로: `/etc/kubernetes/manifests/` (YAML 수정 시 Kubelet이 재기동 처리)

### 3) 호스트 서비스 기반(systemd 등)
- 마스터 노드에서 상태 확인:
  - `systemctl status kube-apiserver kube-controller-manager kube-scheduler etcd`
  - 로그: `journalctl -u kube-apiserver -e --no-pager` (다른 유닛도 동일)
- 워커 노드에서 상태 확인:
  - `systemctl status kubelet` (필수), `kubectl -n kube-system get ds kube-proxy` 또는 `systemctl status kube-proxy`(네이티브 배포 시)

### 4) 공통 로그/헬스체크 포인트
- API Server 헬스: `kubectl get --raw /healthz` 또는 `/livez`, `/readyz`
- etcd 헬스(로컬 실행 시): `etcdctl endpoint health` (환경변수/인증옵션 필요), 스냅샷/공간고갈 확인
- 인증서 만료: `/etc/kubernetes/pki` 하위 cert/키 유효기간 확인, `kubeadm certs check-expiration`
- 포트/방화벽: 6443(API), 2379/2380(etcd), 10250(kubelet), 10257/10259(controller/scheduler) 개방 여부
- Kubelet 설정/로그: `/var/lib/kubelet/config.yaml`, `journalctl -u kubelet`
- kubeconfig 경로: `/etc/kubernetes/admin.conf`, `scheduler.conf`, `controller-manager.conf`, `kubelet.conf`

### 5) 증상별 힌트
- kubectl 불가(REFUSED/EOF): API Server 다운, 6443 차단, 인증서 문제 확인
- 컨트롤러/스케줄러 Pending: 해당 파드 CrashLoop, 권한(RBAC) 또는 kubeconfig 경로/권한 오류
- 워커 NotReady: Kubelet 다운/인증서 만료/컨테이너런타임 문제, CNI 초기화 실패
- CoreDNS/네트워킹 이슈: `kube-proxy`/CNI 상태, iptables/ipvs 규칙, Service CIDR 겹침 여부

### 6) 유용 명령 모음
- `kubectl -n kube-system get pods -o wide`
- `kubectl -n kube-system logs <pod>` / `describe <pod>`
- `systemctl status <unit>` / `journalctl -u <unit>`
- `ss -lntp | grep -E '6443|2379|2380|1025|30000'`
- `crictl ps -a` 또는 `ctr`, `docker ps` (런타임에 따라)

문제가 재현되면 우선 노드·파드 전반 상태를 확인하고, 배포 방식에 맞춰(Static Pod vs systemd) 로그와 설정을 점검하면 원인 축소가 빠르다. 추가 팁은 Kubernetes 공식 문서의 트러블슈팅 가이드를 참고한다.

