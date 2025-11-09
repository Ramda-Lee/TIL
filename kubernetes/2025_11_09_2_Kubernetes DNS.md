# 쿠버네티스

## Kubernetes DNS

### 강의 핵심 요약
- 소규모 환경에서 `/etc/hosts`와 `/etc/resolv.conf`로 해결하던 이름 해석을, 쿠버네티스는 클러스터 내부 DNS(CoreDNS)로 중앙집중화한다.
- CoreDNS는 `kube-system` 네임스페이스에서 ReplicaSet/Deployment로 구동되며, ConfigMap으로 전달된 Corefile과 `kubernetes` 플러그인을 통해 서비스·(선택적) Pod 레코드를 만든다.
- 각 Pod의 `/etc/resolv.conf`는 자동으로 kube-dns 서비스 IP를 Nameserver로 사용하고, `search` 도메인 설정 덕분에 짧은 서비스 이름만으로도 해석된다.
- 서비스는 `web-service.default.svc.cluster.local` 형태의 FQDN을 가지며, Pod 레코드는 IP를 대시로 치환한 값(`<ip-with-dashes>.<ns>.pod.cluster.local`)을 사용해야 한다.

### /etc/hosts 로는 해결이 어려운 이유
- 강의 시나리오: Pod A(10.244.1.5)와 Pod B(10.244.2.5)가 있을 때 서로를 `/etc/hosts`에 직접 등록하면 통신이 가능하다.
- 하지만 수천 개의 Pod가 계속 생성·삭제되면 매번 hosts 파일을 수정하기 어렵고, Pod 자체가 새 레코드를 알지 못한다.
- 해결: 레코드를 중앙 DNS 서버로 옮기고 각 Pod의 `/etc/resolv.conf`에 `nameserver <DNS-IP>`를 지정한다. 이후 새 Pod가 생기면 DNS 레코드와 resolv.conf를 자동으로 맞춰 준다.

### 쿠버네티스의 DNS 배포 방식
- Kubernetes 1.12 이전에는 `kube-dns`, 이후에는 `CoreDNS`가 기본 DNS 서버다.
- CoreDNS Pod는 `kube-system` 네임스페이스에 ReplicaSet(또는 Deployment) 형태로 떠서 고가용성을 확보한다.
- 동일한 CoreDNS 바이너리를 실행하지만, 쿠버네티스 전용 플러그인이 활성화된 Corefile을 사용한다.

### Corefile과 kubernetes 플러그인
- Corefile은 `/etc/coredns/Corefile` 경로로 마운트되며 ConfigMap으로 관리된다. 필요 시 ConfigMap만 수정하면 CoreDNS 설정이 변경된다.
- 주요 플러그인: `errors`, `health`, `ready`, `metrics`, `cache` 등 운영용 플러그인 외에 `kubernetes` 플러그인이 핵심이다.
- `kubernetes cluster.local in-addr.arpa ip6.arpa { ... }` 블록에서 클러스터 최상위 도메인(기본 `cluster.local`)과 옵션을 정의한다.
- `pods insecure` 옵션을 활성화하면 Pod IP를 대시로 치환한 FQDN 레코드를 자동 생성한다(기본 비활성).
- CoreDNS는 클러스터 내부에서 모르는 도메인을 만나면 Pod의 `/etc/resolv.conf`에 정의된 상위 DNS로 포워딩한다.

### Pod가 DNS와 만나는 과정
- kube-dns(CoreDNS) 앞에는 서비스 `kube-dns`가 자동으로 생성되고 고정된 ClusterIP(기본 10.96.0.10)를 가진다.
- Kubelet 설정(`--cluster-dns`, `--cluster-domain`)에 따라 새 Pod의 `/etc/resolv.conf`가 작성되며, Nameserver는 kube-dns 서비스 IP로, Search 도메인은 `<namespace>.svc.cluster.local`, `svc.cluster.local`, `cluster.local` 등으로 채워진다.
- 덕분에 사용자는 `web-service`만 입력해도 Kubelet이 준비한 Search 도메인 조합을 통해 라우팅된 FQDN을 자동 질의한다.

### 서비스·Pod 레코드 조회 패턴
- `nslookup web-service` → CoreDNS는 실제로 `web-service.default.svc.cluster.local`을 찾아 응답하고, `kube-proxy`가 해당 ClusterIP를 실제 Pod로 DNAT한다.
- 다른 네임스페이스에서 접근하려면 `web-service.default`처럼 네임스페이스를 포함하거나 전체 FQDN을 써야 한다.
- Search 항목은 서비스 도메인(`svc`)까지만 커버하므로, Pod에 직접 접근하려면 `10-244-2-5.default.pod.cluster.local`과 같이 전체 FQDN을 입력해야 한다.

### 운영 시 확인 포인트
- `kubectl -n kube-system get deploy coredns`로 CoreDNS 배포 상태와 Replica 수를 확인한다.
- `kubectl -n kube-system get configmap coredns -o yaml`로 Corefile을 검토하고 `pods`, `fallthrough`, `forward` 등의 옵션을 조정한다.
- 워크로드 Pod에서 `cat /etc/resolv.conf`를 확인하면 Kubelet이 주입한 Nameserver와 Search 도메인을 확인할 수 있다.
- 서비스 이름 해석이 되지 않으면 `nslookup <svc>`로 CoreDNS 응답을 검증하고, kubelet 설정이나 kube-dns 서비스 IP가 올바른지 점검한다.

쿠버네티스는 이렇게 CoreDNS와 kubelet을 통해 “Pod가 자동으로 내부 DNS를 사용”하게 만들고, 서비스 이름만으로 안정적인 통신을 가능하게 한다. Pod 레코드는 기본적으로 비활성화되어 있으므로, 서비스에 이름을 주고 그 이름을 사용하는 것이 일반적인 운영 패턴이다.
