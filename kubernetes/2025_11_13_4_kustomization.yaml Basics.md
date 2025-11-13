# 쿠버네티스

## kustomization.yaml 기본과 build 흐름

### 핵심 개념
- Kustomize는 디렉터리에서 `kustomization.yaml` 파일을 찾아 동작.
- 해당 파일에는 두 가지가 핵심으로 들어감:
  - 관리할 리소스 목록(`resources`): Kustomize가 읽어들일 매니페스트 파일들
  - 적용할 커스터마이제이션(변환): 공통 라벨, 패치 등

### 예시 디렉터리 구성
```
k8s/
  deployment.yaml   # nginx Deployment
  service.yaml      # nginx Service
  kustomization.yaml
```

### kustomization.yaml 예시(리소스 + 공통 라벨)
```yaml
resources:
  - deployment.yaml
  - service.yaml

commonLabels:
  company: KodeKloud
```
- `resources`: Kustomize가 관리할 파일 목록
- `commonLabels`: 모든 리소스에 공통 라벨 추가(예: `company=KodeKloud`)

### 렌더링: kustomize build
```bash
kustomize build k8s
```
- `kustomization.yaml`를 읽고, 리소스를 합친 뒤 변환을 적용한 “최종 매니페스트”를 표준출력으로 보여줌.
- 이 단계는 리소스를 “적용”하지 않음(단순 출력).

### 적용 방법(참고)
```bash
# build 출력 → kubectl로 파이프 적용
kustomize build k8s | kubectl apply -f -

# 또는 kubectl 내장 Kustomize 사용
kubectl apply -k k8s
```

### 요약
- `kustomization.yaml`는 “무엇을 관리(resources)”와 “무엇을 바꿀지(변환)”를 선언하는 핵심 파일.
- `kustomize build`는 최종 매니페스트를 출력만 함 → 실제 적용은 `kubectl`로 수행.
- 공통 라벨 추가 같은 단순 변환부터, 패치 등 다양한 커스터마이즈를 점진적으로 확장 가능.
