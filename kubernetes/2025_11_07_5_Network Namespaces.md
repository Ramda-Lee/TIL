# 쿠버네티스

## Network Namespaces

### 네임스페이스 개념
리눅스 네임스페이스는 컨테이너가 호스트와 분리된 독립 환경을 갖도록 도와준다. 집과 방에 비유하면, 호스트가 집이고 각 컨테이너는 자기 방(네임스페이스) 안에서만 존재를 인식한다. 컨테이너 안에서는 PID 1 하나만 보이지만, 호스트에서는 모든 프로세스를 볼 수 있다.

### 네트워크 네임스페이스 만들기
네트워크를 격리하려면 `ip netns add red`, `ip netns add blue` 같은 명령으로 네임스페이스를 만든다. `ip link`는 호스트 인터페이스(루프백, `eth0` 등)를 보여주지만 `ip netns exec red ip link` 또는 `ip link show dev lo -n red`로 네임스페이스 안을 보면 루프백만 보이고 호스트 인터페이스는 보이지 않는다. ARP, 라우팅 테이블도 마찬가지로 각 네임스페이스마다 독립적이다.

### veth 페어로 두 네임스페이스 연결
서로 통신하려면 가상 이더넷 쌍(veth pair)을 만든다.
- `ip link add veth-red type veth peer name veth-blue`
- `ip link set veth-red netns red`
- `ip link set veth-blue netns blue`
- `ip netns exec red ip addr add 192.168.15.1/24 dev veth-red`
- `ip netns exec blue ip addr add 192.168.15.2/24 dev veth-blue`
- `ip netns exec red ip link set veth-red up`
- `ip netns exec blue ip link set veth-blue up`

이제 서로 ping이 되고, 각자 ARP 테이블에 상대 MAC이 기록된다. 호스트 ARP 테이블은 해당 사실을 모른다.

### 리눅스 브리지로 다수 네임스페이스 연결
네임스페이스가 늘어나면 일일이 veth로 서로 연결하기 어렵다. 호스트 내에 가상 스위치(리눅스 브리지)를 만들어 모두 연결한다.
1. `ip link add vnet0 type bridge`
2. `ip link set vnet0 up`
3. 각각의 네임스페이스에 대해 `ip link add veth-red type veth peer name veth-red-br`
4. `ip link set veth-red netns red`
5. `ip link set veth-red-br master vnet0`
6. `ip link set veth-red-br up`
7. 네임스페이스 내부에서 IP 설정 후 인터페이스 up

이 과정을 반복해 여러 네임스페이스를 같은 브리지(192.168.15.0/24 등)에 붙이면 서로 통신할 수 있다.

### 호스트와 네임스페이스 간 통신
브리지는 호스트 입장에서는 또 하나의 인터페이스이므로, `ip addr add 192.168.15.5/24 dev vnet0`처럼 IP를 주면 호스트에서도 네임스페이스를 ping할 수 있다. 이 네트워크는 여전히 호스트 안에만 있으므로 외부에서는 접근할 수 없다.

### 외부 네트워크와 통신
네임스페이스에서 LAN(예: 192.168.1.0/24)으로 나가려면 게이트웨이를 지정해야 한다.
1. 네임스페이스 라우팅 테이블에 `ip route add 192.168.1.0/24 via 192.168.15.5` 추가 (게이트웨이는 브리지에 붙은 호스트 IP).
2. 호스트에서 NAT 설정:
   ```bash
   iptables -t nat -A POSTROUTING -s 192.168.15.0/24 -o eth0 -j MASQUERADE
   ```
NAT 덕분에 네임스페이스에서 오는 패킷이 호스트 IP로 변환되어 외부에서 응답을 돌려줄 수 있다.

### 인터넷 접근과 기본 게이트웨이
LAN 외 다른 네트워크(예: 8.8.8.8)에 접근하려면 네임스페이스에 기본 경로를 추가한다.
```bash
ip route add default via 192.168.15.5
```
이제 호스트가 접근 가능한 모든 네트워크로 NAT를 통해 나갈 수 있다.

### 외부에서 네임스페이스로 접근
네임스페이스의 웹 서비스(포트 80 등)를 외부에 노출하려면:
- 다른 호스트에 `ip route add 192.168.15.0/24 via 192.168.1.2`처럼 경로를 알려주거나,
- 더 일반적으로 호스트에서 포트 포워딩 설정:
  ```bash
  iptables -t nat -A PREROUTING -p tcp --dport 80 -j DNAT --to-destination 192.168.15.2:80
  ```
이렇게 하면 외부에서 호스트 IP의 80번 포트를 요청했을 때 네임스페이스로 트래픽이 전달된다.

네임스페이스를 활용하면 컨테이너와 유사한 네트워크 격리를 수동으로 구성하면서, 브리지·NAT·포트 포워딩 조합으로 외부와의 경로를 유연하게 제어할 수 있다.
