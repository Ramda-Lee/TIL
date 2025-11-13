# 쿠버네티스

## Kustomize 데모: 다중 디렉터리 적용 흐름

### 디렉터리 구성(예)
```
k8s/
  api/
    api-depl.yaml
    api-service.yaml
  cache/
    redis-config.yaml
    redis-depl.yaml
    redis-service.yaml
  db/
    mongo-depl.yaml
    mongo-service.yaml
```

### 1) Kustomize 없이 표준 kubectl로 배포/삭제
- 배포(디렉터리별):
```bash
kubectl apply -f k8s/api
kubectl apply -f k8s/cache
kubectl apply -f k8s/db
```
- 한 줄로도 가능(여러 -f 나열):
```bash
kubectl apply -f k8s/db -f k8s/cache -f k8s/api
```
- 삭제는 `apply` → `delete`로 변경:
```bash
kubectl delete -f k8s/db -f k8s/cache -f k8s/api
```
- 단점: 디렉터리가 늘수록 명령이 길어지고 관리가 번거로움.

### 2) 루트에 단일 kustomization.yaml 두고 파일 경로 나열
`k8s/kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - api/api-depl.yaml
  - api/api-service.yaml
  - cache/redis-config.yaml
  - cache/redis-depl.yaml
  - cache/redis-service.yaml
  - db/mongo-depl.yaml
  - db/mongo-service.yaml
```
- 렌더 확인:
```bash
kustomize build k8s
```
- 적용:
```bash
kustomize build k8s | kubectl apply -f -
# 또는
kubectl apply -k k8s
```

### 3) 권장: 서브디렉터리별 kustomization.yaml + 루트는 디렉터리만 참조
- 하위(api/cache/db) 각각에 `kustomization.yaml` 배치 후, 파일은 해당 디렉터리에서 상대 경로로 나열.
예) `k8s/api/kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - api-depl.yaml
  - api-service.yaml
```
루트 `k8s/kustomization.yaml`는 디렉터리만 참조:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - api
  - cache
  - db
```
- 적용/삭제:
```bash
kubectl apply -k k8s
kubectl delete -k k8s
```

### 4) 검증
```bash
kubectl get pods
# api, redis, mongo 관련 파드가 Running 상태인지 확인
```

### 요약
- 초기엔 디렉터리별 `kubectl apply -f`로도 가능하나, 규모가 커지면 Kustomize가 유리.
- 루트에서 파일 나열 → 더 나아가 디렉터리별 kustomization으로 모듈화하면 유지보수와 CI가 단순해짐.
