# 쿠버네티스

## Kubernetes DNS

### 개요
클러스터 내부 통신은 주로 서비스 이름을 통해 이뤄진다. Kubernetes는 기본적으로 내부 DNS 서버(CoreDNS 등)를 배포해 Pod·Service의 이름 해석을 담당한다. 여기서는 노드 외부 DNS가 아니라, **클러스터 내부** Pod/Service 간 DNS를 다룬다.

### 서비스 이름 구조
서비스를 생성하면 DNS 서버는 서비스 이름과 IP 간 매핑을 만든다. 기본 규칙은 다음과 같다.
```
<service>.<namespace>.svc.cluster.local
```
- 같은 네임스페이스에서는 `service`만으로도 접근 가능
- 다른 네임스페이스에서 접근할 때는 `service.namespace`
- 전체 FQDN은 `service.namespace.svc.cluster.local`

예: `apps` 네임스페이스의 `web-service` → `web-service.apps.svc.cluster.local`

### 네임스페이스별 서브도메인
- 각 네임스페이스는 고유한 서브도메인을 갖는다.
- `svc` 서브도메인은 서비스 레코드를 묶는 용도이며, Pod 레코드를 별도 `pod` 서브도메인으로 구성할 수 있다.
- 루트 도메인은 기본적으로 `cluster.local`이지만 `--cluster-domain` 옵션으로 변경 가능.

### Pod DNS 레코드
- 기본적으로 Pod 이름 기반 레코드는 생성되지 않지만, CoreDNS 설정에서 `pods insecure` 옵션 등을 활성화하면 IP 기반 레코드를 생성할 수 있다.
- 형식: `<ip-with-dashes>.<namespace>.pod.cluster.local`
  - 예: Pod IP `10.244.2.5` → `10-244-2-5.default.pod.cluster.local`
- Pod 이름 대신 IP 기반 문자열을 사용하므로, 실제로는 서비스 이름을 사용하는 편이 일반적이다.

### 동작 흐름
1. Pod가 `web-service`를 조회 → CoreDNS가 `web-service.default.svc.cluster.local`을 Service IP로 응답
2. Pod가 Service IP로 트래픽 전송 → kube-proxy 규칙(iptables/ipvs)이 실제 백엔드 Pod로 DNAT
3. 필요 시 Pod 레코드도 DNS에서 찾을 수 있으나, 대부분 서비스 이름으로 통신

### 실습 팁
- `kubectl exec`로 Pod에 들어가 `nslookup service-name` 실행
- `/etc/resolv.conf`를 확인하면 `search` 항목에 `<namespace>.svc.cluster.local svc.cluster.local cluster.local` 순서가 들어 있다.
- CoreDNS ConfigMap에서 `clusterDomain`, `pods`, `fallthrough` 옵션을 조정해 레코드 정책을 바꿀 수 있다.
