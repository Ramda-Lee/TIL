# 쿠버네티스

## Kustomize: 다중 디렉터리 구성 관리

### 문제 배경
- 리소스가 늘어나면서 `k8s/` 아래에 API/DB/Cache/Kafka 등 하위 디렉터리로 분리함.
- 각 디렉터리로 이동해 개별적으로 `kubectl apply -f`를 실행해야 해 번거롭고, CI 파이프라인도 복잡해짐.

### 해법 1: 루트 kustomization.yaml에서 파일 경로 직접 나열
```
k8s/
  api/
    deployment.yaml
    service.yaml
  db/
    deployment.yaml
    service.yaml
  cache/
    ...
  kafka/
    ...
  kustomization.yaml
```

`k8s/kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - api/deployment.yaml
  - api/service.yaml
  - db/deployment.yaml
  - db/service.yaml
  - cache/deployment.yaml
  - cache/service.yaml
  - kafka/deployment.yaml
  - kafka/service.yaml
```
- 장점: 즉시 동작. 단점: 리소스가 늘수록 루트 파일이 비대해지고 유지보수 어려움.

### 해법 2: 하위 디렉터리별 kustomization.yaml 구성(권장)
각 서브디렉터리에 자체 kustomization.yaml을 두고 루트는 디렉터리만 참조.

`k8s/api/kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml
  - service.yaml
```

`k8s/db/kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml
  - service.yaml
```

루트 `k8s/kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - api
  - db
  - cache
  - kafka
```
- 장점: 루트 구성 간결, 디렉터리 추가/제거가 쉬움, 모듈화/스케일에 유리.

### 적용/삭제 방법
- 적용(파이프):
```bash
kustomize build k8s | kubectl apply -f -
```
- 적용(kubectl 네이티브):
```bash
kubectl apply -k k8s
```
- 삭제:
```bash
kustomize build k8s | kubectl delete -f -
kubectl delete -k k8s
```

### 팁
- 디렉터리별로 공통 라벨/네임프리픽스/이미지 태그 변경 등의 변환을 각자의 kustomization.yaml에 정의 가능.
- 루트에서 상위 공통 변환을 정의하고, 하위에서 추가/오버라이드도 가능.

### 요약
- 다중 디렉터리는 서브디렉터리별 kustomization.yaml을 두고, 루트는 디렉터리만 `resources`로 참조하는 구조가 가장 깔끔함.
- 한 번의 `apply -k` 또는 파이프 방식으로 전체를 일괄 배포/삭제 가능해 운영과 CI가 단순해짐.
