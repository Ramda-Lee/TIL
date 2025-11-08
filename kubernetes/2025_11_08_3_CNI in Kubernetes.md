# 쿠버네티스

## CNI in Kubernetes

### 개요
네트워크 네임스페이스 → Docker → CNI 표준 순으로 살펴본 뒤, 이번 강의에서는 Kubernetes가 네트워크 플러그인을 어떻게 적용하는지 정리한다. CNI 스펙에 따르면 컨테이너 런타임(여기서는 containerd나 CRI-O)이 컨테이너 네임스페이스를 만들고, 적절한 플러그인을 호출해 네트워크를 붙여야 한다.

### 플러그인 설치 위치
- 실행 파일(플러그인) 경로: `/opt/cni/bin`
  - `bridge`, `host-local`, `dhcp`, `flannel` 등 CNI에서 제공하거나 서드파티에서 설치한 플러그인이 배치된다.
- 설정 파일 경로: `/etc/cni/net.d`
  - 어떤 플러그인을 어떤 설정으로 사용할지 정의하는 JSON/YAML 파일이 들어 있다. 여러 개면 알파벳순으로 가장 앞선 파일이 기본 선택된다.

### 예시: bridge.conf
```json
{
  "cniVersion": "1.1.0",
  "name": "mynet",
  "type": "bridge",
  "bridge": "cni0",
  "isGateway": true,
  "ipMasq": true,
  "ipam": {
    "type": "host-local",
    "ranges": [
      [{ "subnet": "10.244.0.0/16" }]
    ]
  }
}
```
- `type`: 사용할 플러그인(여기서는 `bridge`)
- `isGateway`: 브리지 인터페이스에 IP를 주어 Pod의 게이트웨이로 동작할지 여부
- `ipMasq`: Pod 트래픽을 외부로 보낼 때 NAT(마스커레이드)를 적용할지 결정
- `ipam`: IP 할당 정책. `host-local`은 각 노드에서 로컬 파일로 IP 풀을 관리하고, `dhcp`로 지정하면 외부 DHCP를 사용할 수 있다.

### 동작 흐름
1. Kubelet이 Pod를 만들기 위해 컨테이너 런타임(containerd/CRI-O)을 호출한다.
2. 런타임은 컨테이너 네트워크 네임스페이스를 만든 후 `/etc/cni/net.d`에서 설정을 읽는다.
3. 해당 설정에 명시된 플러그인 실행 파일을 `/opt/cni/bin`에서 찾아 `ADD` 명령으로 실행한다.
4. 플러그인은 브리지 구성, veth 연결, IPAM 등에 따라 Pod 네트워크를 세팅한다.
5. Pod 삭제 시에는 동일 플러그인을 `DEL` 명령으로 호출해 정리한다.

### 실습 팁
- `ls /opt/cni/bin`으로 설치된 플러그인 확인
- `ls /etc/cni/net.d`와 `cat`으로 실제 설정 확인
- Kubelet 로그에서 `CNI ADD/DEL` 호출 메시지를 확인하면 문제 해결에 도움이 된다.
