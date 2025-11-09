# 쿠버네티스

## kubeadm 데모: 단일 컨트롤 플레인 + 워커 2

### 환경 개요
- Vagrant로 VM 3대(마스터 1, 워커 2) 준비 완료.
- 통신 인터페이스: `enp0s8`
  - master: `192.168.56.11`
  - node1: `192.168.56.21`
  - node2: `192.168.56.22`
- 문서 탭
  - kubeadm 설치: https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/
  - kubeadm로 클러스터 생성: https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/create-cluster-kubeadm/
  - (HA 시) 고가용성: create-cluster-kubeadm/#control-plane-node

### 1) 모든 노드에 kubeadm/kubelet/kubectl 설치 (예: Ubuntu 1.31)
```bash
# 서명키/리포지토리 추가 (버전에 맞게 변경)
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gpg
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.31/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.31/deb/ /" | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo systemctl enable --now kubelet
kubeadm version
```

### 2) 컨테이너 런타임(containerd) 설치 및 cgroup 드라이버 정합
```bash
sudo apt-get install -y containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo systemctl restart containerd

# init 시스템 확인 (systemd 여부)
ps -p 1 -o comm=
# kubeadm 1.22+는 kubelet cgroupDriver 기본값이 systemd
```

### 3) 컨트롤 플레인 초기화 (master에서 실행)
```bash
sudo kubeadm init \
  --apiserver-advertise-address=192.168.56.11 \
  --pod-network-cidr=10.244.0.0/16 \
  --upload-certs

# kubeconfig 설정
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

kubectl get nodes
# 초기엔 master가 NotReady (CNI 미설치)
```

### 4) CNI(Flannel) 설치 및 CIDR 확인
```bash
# 기본 PodCIDR가 10.244.0.0/16이므로 그대로 적용 가능
kubectl apply -f https://raw.githubusercontent.com/flannel-io/flannel/refs/heads/master/Documentation/kube-flannel.yml

# 필요 시 매니페스트 내려받아 net-attach 설정에서 CIDR 수정 후 적용
# curl -LO <위 URL>; vi kube-flannel.yml; kubectl apply -f kube-flannel.yml

kubectl get ns
kubectl get pods -n kube-flannel -w
kubectl get nodes  # master가 Ready로 전환
```

### 5) 워커 노드 조인 (node1, node2에서 실행)
```bash
# master의 kubeadm init 출력에 표시된 join 명령 사용
sudo kubeadm join <API_SERVER_IP_or_LB>:6443 \
  --token <TOKEN> \
  --discovery-token-ca-cert-hash sha256:<HASH>
```

### 6) 검증 및 간단 테스트
```bash
kubectl get nodes -o wide   # master, node1, node2 모두 Ready
kubectl get pods -n kube-flannel

kubectl run web --image=nginx
kubectl get pods -w
kubectl delete pod web
```

### 참고/팁
- HA 구성 시: `kubeadm init --control-plane-endpoint <LB:6443>` 사용 → 이후 추가 CP 노드는 `kubeadm join ... --control-plane`.
- 컨테이너 런타임 소켓 경로가 비표준이면 `--cri-socket` 지정.
- `kubeadm token create --print-join-command`로 조인 커맨드 재생성.

