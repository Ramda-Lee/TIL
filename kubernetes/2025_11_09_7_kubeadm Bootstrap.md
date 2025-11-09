# 쿠버네티스

## kubeadm로 클러스터 부트스트랩

### 개요
- kubeadm은 쿠버네티스 베스트 프랙티스를 따르는 멀티 노드 클러스터를 빠르게 구성하는 도구다.
- 컨트롤 플레인/워커에 필요한 컴포넌트 설치·인증서·구성을 자동화해 초기 셋업 부담을 줄인다.

### 사전 준비(모든 노드)
- 64‑bit Linux, 시간 동기화(NTP), 고정/예측 가능한 호스트네임.
- 스왑 비활성화: `sudo swapoff -a` 및 `/etc/fstab`에서 스왑 항목 주석 처리.
- 커널 모듈: `overlay`, `br_netfilter` 로드.
  ```bash
  cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
  overlay
  br_netfilter
  EOF
  sudo modprobe overlay
  sudo modprobe br_netfilter
  ```
- sysctl: 브릿지/포워딩 활성화.
  ```bash
  cat <<EOF | sudo tee /etc/sysctl.d/99-kubernetes-cri.conf
  net.bridge.bridge-nf-call-iptables = 1
  net.bridge.bridge-nf-call-ip6tables = 1
  net.ipv4.ip_forward = 1
  EOF
  sudo sysctl --system
  ```

### 컨테이너 런타임(containerd) 설치/설정
- 배포판 패키지 또는 바이너리로 설치 후, 기본 설정 생성 및 cgroup 드라이버를 systemd로 통일.
  ```bash
  sudo mkdir -p /etc/containerd
  containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
  sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
  sudo systemctl enable --now containerd
  ```

### kubeadm/kubelet/kubectl 설치(모든 노드)
- 배포판 가이드를 따르되, kubelet은 부팅 시 자동 시작.
  ```bash
  # 리포지토리 추가 후 예시
  sudo apt-get update && sudo apt-get install -y kubelet kubeadm kubectl
  sudo systemctl enable --now kubelet
  ```

### 컨트롤 플레인 초기화(마스터 1대)
- CNI가 사용할 Pod CIDR을 지정(예: Calico=192.168.0.0/16, Flannel=10.244.0.0/16).
  ```bash
  sudo kubeadm init \
    --pod-network-cidr=10.244.0.0/16 \
    --cri-socket unix:///run/containerd/containerd.sock
  ```
- 관리자 kubeconfig 설정:
  ```bash
  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config
  ```
- CNI 플러그인 배포(예시 중 택1):
  ```bash
  # Flannel 예시
  kubectl apply -f https://raw.githubusercontent.com/flannel-io/flannel/refs/heads/master/Documentation/kube-flannel.yml
  # Calico 예시
  # kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.27.3/manifests/calico.yaml
  ```

### 워커 노드 조인(모든 워커)
- `kubeadm init` 출력의 join 명령을 각 워커에서 실행:
  ```bash
  sudo kubeadm join <CONTROL_PLANE_LB_OR_IP>:6443 \
    --token <TOKEN> \
    --discovery-token-ca-cert-hash sha256:<HASH> \
    --cri-socket unix:///run/containerd/containerd.sock
  ```
- 토큰 재발급: `kubeadm token create --print-join-command`

### 검증
```bash
kubectl get nodes -o wide
kubectl get pods -A
```

### 네트워킹 주의사항(CNI)
- `--pod-network-cidr`는 선택한 CNI의 기본값과 일치해야 한다.
- 서로 다른 CNI 간 CIDR/정책이 다르므로 공식 매니페스트의 권장값을 따른다.

### HA 관련(간단 메모)
- 추가 컨트롤 플레인 노드: LB 전면 배치 후 `kubeadm join ... --control-plane` 사용.
- etcd 외부화 토폴로지는 추후 고가용성 설계 문서를 참고.

### 트러블슈팅/리셋
- 이미지 프리풀: `kubeadm config images pull`
- 초기화 재시도 전 정리: `sudo kubeadm reset -f && sudo systemctl restart containerd && sudo rm -rf $HOME/.kube`

