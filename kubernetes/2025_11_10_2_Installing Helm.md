# 쿠버네티스

## Installing Helm (Helm 설치)

### 사전 요구사항
- 동작 중인 쿠버네티스 클러스터가 있어야 함.
- 로컬에 `kubectl`이 설치/구성되어 있어야 함.
- `kubeconfig`(일반적으로 `~/.kube/config`)가 대상 클러스터에 접속 가능하도록 설정되어 있어야 함.

검증 예시:
```bash
kubectl get nodes
```

### 지원 OS
- Helm은 Linux, Windows, macOS에서 설치 가능.
- 본 섹션은 Linux 중심으로 간략 설치 방법을 정리.

### Linux에서 설치 방법

1) Snap 사용 시스템
```bash
sudo snap install helm --classic
```
- `--classic` 옵션: 스냅 샌드박스를 완화하여 홈 디렉터리 내 `kubeconfig` 접근이 용이함.

2) APT 기반(Debian/Ubuntu 등)
```bash
curl -fsSL https://baltocdn.com/helm/signing.asc | sudo gpg --dearmor -o /usr/share/keyrings/helm.gpg
echo "deb [signed-by=/usr/share/keyrings/helm.gpg] https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list
sudo apt-get update
sudo apt-get install -y helm
```

3) FreeBSD (pkg)
```bash
sudo pkg install helm
```

### macOS/Windows
- 설치는 운영체제별 최신 문서 참고.
- 예) macOS(Homebrew): `brew install helm` (환경에 따라 상이할 수 있음)
- 공식 문서의 최신 버전 지침을 우선 확인할 것.

### 설치 확인
```bash
helm version
```

### 랩 안내
- 실습 환경에서 위 방법으로 Helm을 설치해 보고, `helm version` 및 `kubectl cluster-info`로 연결 상태를 확인.

### 요약
- Helm 설치 전: 클러스터와 kubectl, kubeconfig가 정상이어야 함.
- Linux에서는 Snap `--classic`, APT 리포지토리 추가 후 설치, FreeBSD는 `pkg install helm` 등 사용.
- OS별 최신 설치 방법은 공식 문서 지침을 따른다.
