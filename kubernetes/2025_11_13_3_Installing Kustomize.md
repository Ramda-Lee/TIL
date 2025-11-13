# 쿠버네티스

## Installing Kustomize (Kustomize 설치)

### 사전 요구사항
- 동작 중인 쿠버네티스 클러스터
- 로컬에 `kubectl` 설치 및 kubeconfig 설정 완료
- 확인:
```bash
kubectl cluster-info
kubectl version --client
```

### 1) 공식 설치 스크립트(자동 OS 감지)
```bash
curl -s https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh | bash
sudo mv kustomize /usr/local/bin/
```
- 스크립트가 OS/아키텍처에 맞는 바이너리를 내려받아 `kustomize` 파일 생성.
- 권한 문제 시 `sudo` 사용 또는 PATH 내 사용자 디렉터리에 배치.

### 2) macOS(Homebrew)
```bash
brew install kustomize
```

### 3) Windows
```powershell
# Chocolatey
choco install kustomize -y

# 또는 Scoop
scoop install kustomize
```

### 4) Go로 설치(개발 환경)
```bash
go install sigs.k8s.io/kustomize/kustomize/v5@latest
# GOPATH/bin을 PATH에 추가 필요
```

### 5) kubectl 내장 Kustomize 사용(대안)
- 별도 설치 없이도 기본 사용 가능:
```bash
kubectl kustomize ./overlays/staging
kubectl apply -k ./overlays/staging
```
- 단, kubectl 내장 버전이 최신이 아닐 수 있어 기능 차이가 날 수 있음.

### 설치 확인
```bash
kustomize version
```
- 출력이 없거나 명령을 찾지 못하면 PATH 문제 가능.

### 문제 해결(환경변수/세션)
- 새 터미널을 열어 PATH 갱신 반영 여부 확인.
- `echo $PATH`(Linux/macOS), `$env:Path`(Windows PowerShell)로 바이너리 경로 포함 여부 확인.
- 바이너리를 PATH 내 디렉터리로 이동 또는 심볼릭 링크 생성.

### 요약
- Kustomize는 스크립트/패키지 매니저/Go 등 다양한 방식으로 설치 가능.
- 빠르게는 설치 스크립트 또는 Homebrew/Chocolatey/Scoop 사용.
- 독립 실행형(`kustomize`)과 `kubectl -k` 둘 다 활용 가능하며, 최신 기능이 필요하면 독립 실행형 권장.
