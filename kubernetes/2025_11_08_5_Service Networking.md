# 쿠버네티스

## Service Networking

### 서비스 개념 정리
- Pod 네트워킹은 각 노드 브리지/네임스페이스에서 다뤘지만, 실제 접근은 대부분 `Service` 객체를 통해 이뤄진다.
- `ClusterIP`: 클러스터 내부에서만 접근 가능한 서비스. IP와 DNS 이름을 부여받고, 모든 Pod에서 동일하게 접근 가능.
- `NodePort`: `ClusterIP` 기능 + 각 노드의 특정 포트를 열어 외부 접근 허용.
- Pod는 노드에 종속되지만, Service는 “클러스터 전체”에 존재하는 추상 객체이며 특정 노드에 바인딩되지 않는다.

### Kube-proxy 역할
- 각 노드에서 `kube-proxy`가 실행되어 API Server를 감시하고 Service/Endpoint 변경을 수신한다.
- Service 생성 시 실제 프로세스/네임스페이스가 생기는 것이 아니라, `kube-proxy`가 네트워크 규칙을 추가해 “가상 IP”를 구현한다.
- 기본 모드는 `iptables`이며, `ipvs`나 `userspace` 모드도 선택할 수 있다(`--proxy-mode`).

### ClusterIP 동작
1. Service 생성 시 `serviceClusterIPRange`에서 IP를 하나 할당한다(예: 10.96.0.0/12).
2. `kube-proxy`가 각 노드에 NAT 규칙을 추가해 “Service IP:Port → Pod IP:Port”로 DNAT한다.
3. Pod나 노드 어디에서든 Service IP로 보낸 트래픽은 적절한 Pod로 전달된다.

예시 규칙(`iptables -t nat -S | grep <service-name>`):
```
-A KUBE-SVC-XXXX -p tcp -m tcp --dport 3306 -j KUBE-SEP-YYYY
-A KUBE-SEP-YYYY -p tcp -m tcp -j DNAT --to-destination 10.244.1.2:3306
```

### NodePort 동작
- ClusterIP 규칙에 더해, 모든 노드의 특정 포트(30000-32767)를 열고 Service로 포워딩하는 규칙을 추가.
- 외부 사용자는 `NodeIP:NodePort`로 접근 가능하며, `kube-proxy`가 DNAT해 백엔드 Pod로 전달한다.

### 네트워크 범위 계획
- Pod CIDR(예: 10.244.0.0/16)과 Service CIDR(예: 10.96.0.0/12)은 겹치지 않아야 한다.
- 겹치면 Pod와 Service가 동일 IP를 가질 수 있어 라우팅/Service 해석이 꼬이므로 반드시 별도 대역을 사용한다.

### 트러블슈팅 팁
- `kubectl get svc -o wide`로 IP/포트 확인.
- `iptables -t nat -S | grep KUBE-` 또는 `ipvsadm -Ln`(ipvs 모드)로 규칙 확인.
- `kubectl -n kube-system logs daemonset/kube-proxy`에서 규칙 생성 로그 확인(verbosity 필요).

Service 네트워킹을 이해하면 Pod 간 통신뿐 아니라 외부 노출(NodePort, LoadBalancer 등)을 설계하고 문제를 추적하는 데 큰 도움이 된다.
