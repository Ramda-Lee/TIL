# 쿠버네티스

## Network Plugin in Kubernetes

여러 CNI 플러그인이 있으며 대표 예시는 다음과 같다.

### Weave Net
- 설치:
```bash
kubectl apply -f https://github.com/weaveworks/weave/releases/download/v2.8.1/weave-daemonset-k8s.yaml
```
- 플러그인 목록과 정책은 Kubernetes 애드온 문서 참고: https://kubernetes.io/docs/concepts/cluster-administration/addons/#networking-and-network-policy

### Flannel
- 설치:
```bash
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/2140ac876ef134e0ed5af15c65e414cf26827915/Documentation/kube-flannel.yml
```
- 주의: 현재 Flannel은 Kubernetes NetworkPolicy를 지원하지 않는다.

### Calico
- 설치:
```bash
curl -LO https://raw.githubusercontent.com/projectcalico/calico/v3.25.0/manifests/calico.yaml
kubectl apply -f calico.yaml
```
- Calico는 고급 네트워크 정책/라우팅 기능을 폭넓게 제공한다.

### 시험/운영 팁
- CKA/CKAD에서 직접 설치를 요구하진 않지만, 요구 시 정확한 URL이 제공된다.
- 하나의 노드에 CNI 설정 파일이 여러 개 있으면 kubelet은 이름의 사전식 순서로 가장 먼저 오는 설정을 사용한다.

## DNS in Kubernetes

Kubernetes는 기본적으로 CoreDNS를 사용한다. CoreDNS는 유연하고 확장 가능한 DNS 서버로, 클러스터 내부 DNS 역할을 수행한다.

### 메모리와 규모
- 대규모 클러스터에서 CoreDNS 메모리 사용량은 Pod/Service 수, DNS 캐시 크기, 인스턴스당 QPS에 좌우된다.

### CoreDNS 리소스 구성
- ServiceAccount: `coredns`
- ClusterRole: `coredns`, `kube-dns`
- ClusterRoleBinding: `coredns`, `kube-dns`
- Deployment: `coredns`
- ConfigMap: `coredns`
- Service: `kube-dns`

### Corefile 주요 설정 예시
포트 53에서 DNS를 제공하며, ConfigMap의 Corefile에 핵심 플러그인 구성이 담긴다.
```txt
kubernetes cluster.local in-addr.arpa ip6.arpa {
   pods insecure
   fallthrough in-addr.arpa ip6.arpa
   ttl 30
}

proxy . /etc/resolv.conf
```
- `kubernetes` 블록은 `cluster.local` 및 리버스 도메인의 백엔드 동작.
- `proxy . /etc/resolv.conf`는 클러스터 외부 도메인을 상위 DNS로 포워딩.

## CoreDNS 트러블슈팅

### Pending 상태
- CoreDNS Pod가 Pending이면 먼저 CNI 플러그인이 정상 설치/동작 중인지 확인한다.

### CrashLoopBackOff/Error 상태
- SELinux + 구버전 Docker 환경에서 CoreDNS가 시작되지 않을 수 있다. 조치:
  - Docker 업그레이드
  - SELinux 비활성화
  - Deployment에서 `allowPrivilegeEscalation: true`로 수정
```bash
kubectl -n kube-system get deployment coredns -o yaml \
 | sed 's/allowPrivilegeEscalation: false/allowPrivilegeEscalation: true/g' \
 | kubectl apply -f -
```
- 루프(loop) 감지로 CrashLoopBackOff가 발생할 수 있다. 대응 방안:
  - kubelet 설정에 실제 resolv.conf 지정: `resolvConf: <real-resolv-conf>` (systemd-resolved 사용 시 일반적으로 `/run/systemd/resolve/resolv.conf`)
  - 노드의 로컬 DNS 캐시 비활성화 후 `/etc/resolv.conf` 원복
  - 임시 조치: Corefile의 `forward . /etc/resolv.conf`를 상위 DNS IP로 교체(예 `forward . 8.8.8.8`). 단, kubelet이 여전히 잘못된 resolv.conf를 Pod에 전달할 수 있음

### Endpoints 확인
- CoreDNS Pod/Service가 정상인데 해석이 안 된다면 `kube-dns` 서비스 엔드포인트 유효성 점검
```bash
kubectl -n kube-system get ep kube-dns
```
- 엔드포인트가 없으면 Service의 selector/ports를 점검한다.

## Kube-Proxy

kube-proxy는 각 노드에서 동작하는 네트워크 프록시로, Service/Endpoint 변화를 감시하고 노드의 네트워크 규칙을 유지한다. 이를 통해 클러스터 내/외부에서 Service IP로 접근한 트래픽이 실제 Pod로 전달된다.

### 배포/동작
- kubeadm 클러스터에서는 일반적으로 DaemonSet으로 배포된다.
- 역할: Service 및 해당 Endpoints를 감시하고, 가상 IP로 유입된 트래픽을 백엔드 Pod로 전달

### kube-proxy 실행 옵션 예시
`kubectl describe ds kube-proxy -n kube-system`에서 확인 가능
```txt
/usr/local/bin/kube-proxy \
  --config=/var/lib/kube-proxy/config.conf \
  --hostname-override=$(NODE_NAME)
```
- 설정 파일(`/var/lib/kube-proxy/config.conf`)에 `clusterCIDR`, `mode`(ipvs/iptables), `bindAddress`, `kubeconfig` 등이 정의된다.

### kube-proxy 트러블슈팅
1) kube-proxy Pod가 Running인지 확인: `kubectl -n kube-system get pods -l k8s-app=kube-proxy`
2) 로그 확인: `kubectl -n kube-system logs ds/kube-proxy`
3) ConfigMap/설정 파일 검증: `kubectl -n kube-system get cm kube-proxy -o yaml`
4) kubeconfig 경로/권한 확인
5) 프로세스/포트 확인
```bash
netstat -plan | grep kube-proxy
# 예시
# tcp  0  0 0.0.0.0:30081   0.0.0.0:*   LISTEN      1/kube-proxy
# tcp  0  0 127.0.0.1:10249 0.0.0.0:*   LISTEN      1/kube-proxy
# tcp  0  0 172.17.0.12:33706 172.17.0.12:6443 ESTABLISHED 1/kube-proxy
# tcp6 0  0 :::10256       :::*        LISTEN      1/kube-proxy
```

## Pre-Requisites - JSONPath

고급 kubectl 명령(예: jsonpath, custom-columns, sort)을 다루기 전에 JSONPath 문법에 익숙해지는 것을 권장한다. 아래 자료로 먼저 연습한 뒤, 본 노트의 고급 명령 예제로 돌아오자.

- 입문/연습: https://kodekloud.com/p/json-path-quiz
- Kubernetes 데이터 객체로 연습:
  - https://mmumshad.github.io/json-path-quiz/index.html#!/?questions=questionskub1
  - https://mmumshad.github.io/json-path-quiz/index.html#!/?questions=questionskub2

## References
- Debug Service issues: https://kubernetes.io/docs/tasks/debug-application-cluster/debug-service/
- DNS Troubleshooting: https://kubernetes.io/docs/tasks/administer-cluster/dns-debugging-resolution/
