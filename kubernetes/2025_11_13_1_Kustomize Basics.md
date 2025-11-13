# 쿠버네티스

## Kustomize 기본(Base/Overlay로 환경별 구성 관리)

### 문제 상황과 배경
- 동일 리소스를 여러 환경(dev/stage/prod)에서 쓰되, 일부 값(예: replicas)만 다르게 운영하고 싶음.
- 단순 복제(환경별 폴더에 동일 YAML 복사)는 파일 증가/동기화 누락/스케일 한계 문제 유발.

### Kustomize가 해결하는 방식
- Base: 모든 환경에 공통인 기본 매니페스트 모음과 기본값.
- Overlays: 환경별로 변경될 부분만 덮어쓰기(override) 또는 추가 리소스 정의.
- 결과: Base + 특정 Overlay를 합성해 최종 매니페스트 생성 후 적용.

### 디렉터리 구조 예시
```
app/
  base/
    deployment.yaml
    kustomization.yaml
  overlays/
    dev/
      kustomization.yaml
    staging/
      kustomization.yaml
      patch-replicas.yaml
    prod/
      kustomization.yaml
      patch-replicas.yaml
```

### Base 예시
`app/base/deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
        - name: nginx
          image: nginx:1.25
          ports:
            - containerPort: 80
```

`app/base/kustomization.yaml`
```yaml
resources:
  - deployment.yaml
```

### Overlays 예시(스테이징/프로덕션에서 replicas 변경)
`app/overlays/staging/kustomization.yaml`
```yaml
resources:
  - ../../base
patchesStrategicMerge:
  - patch-replicas.yaml
```

`app/overlays/staging/patch-replicas.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx
spec:
  replicas: 2
```

`app/overlays/prod/kustomization.yaml`
```yaml
resources:
  - ../../base
patchesStrategicMerge:
  - patch-replicas.yaml
```

`app/overlays/prod/patch-replicas.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx
spec:
  replicas: 5
```

### 적용 방법(kubectl 내장 Kustomize 사용)
- 개발(dev, 기본값 1):
```bash
kubectl apply -k app/overlays/dev
```
- 스테이징(2 replicas):
```bash
kubectl apply -k app/overlays/staging
```
- 프로덕션(5 replicas):
```bash
kubectl apply -k app/overlays/prod
```

### 동작 개념 정리
- Base는 공통 리소스와 기본값을 제공.
- Overlay는 필요한 변경만 패치(예: replicas, 이미지 태그, 환경변수 추가 등).
- 템플릿 언어 없음: 순수 YAML + 패치 규칙으로 동작하므로 가독성/검증이 용이.

### 추가 팁
- `kustomize build app/overlays/staging` 또는 `kubectl kustomize app/overlays/staging`으로 렌더 결과 확인 가능.
- 패치 수단
  - `patchesStrategicMerge`: 동일 Kind/Name 리소스를 전략 병합으로 덮어쓰기.
  - `patchesJson6902`: JSON 패치 문법으로 세밀 변경.
- 그 외 변환기: `namePrefix`, `commonLabels`, `images`, `configMapGenerator`, `secretGenerator`, `replicas` 등.

### 요약
- Kustomize는 Base/Overlay 개념으로 환경별 차이만 선언하고 재사용을 극대화.
- 디렉터리와 kustomization.yaml로 구성을 표준화해 복제/동기화 실수를 줄임.
- kubectl에 내장되어 별도 템플릿 언어 없이 순수 YAML로 관리 가능.
