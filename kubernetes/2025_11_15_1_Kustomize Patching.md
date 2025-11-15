# 쿠버네티스

## Kustomize 패치 (JSON 6902 / Strategic Merge)

### 개요
Deployment 라벨 딕셔너리(labels)에서 키를 교체/추가/삭제하는 방법을 Kustomize의 두 패치 방식으로 정리한다.

### 전제 리소스 예시
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    metadata:
      labels:
        component: api
    spec:
      containers:
      - name: nginx
        image: nginx
```

### JSON 6902 패치로 키 값 교체(replace)
kustomization.yaml
```yaml
resources:
  - api-deployment.yaml
patchesJson6902:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    patch: |
      - op: replace
        path: /spec/template/metadata/labels/component
        value: web
```

### Strategic Merge 패치로 키 값 교체
kustomization.yaml
```yaml
resources:
  - api-deployment.yaml
patches:
  - path: label-patch.yaml
```

label-patch.yaml (변경되는 최소 항목만 포함)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    metadata:
      labels:
        component: web
```

### JSON 6902 패치로 키 추가(add)
kustomization.yaml
```yaml
resources:
  - api-deployment.yaml
patchesJson6902:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    patch: |
      - op: add
        path: /spec/template/metadata/labels/org
        value: kodekloud
```

### Strategic Merge 패치로 키 추가
kustomization.yaml
```yaml
resources:
  - api-deployment.yaml
patches:
  - path: label-patch.yaml
```

label-patch.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    metadata:
      labels:
        org: kodekloud
```

### JSON 6902 패치로 키 삭제(remove)
kustomization.yaml
```yaml
resources:
  - api-deployment.yaml
patchesJson6902:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    patch: |
      - op: remove
        path: /spec/template/metadata/labels/org
```

### Strategic Merge 패치로 키 삭제(null 지정)
kustomization.yaml
```yaml
resources:
  - api-deployment.yaml
patches:
  - path: label-patch.yaml
```

label-patch.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    metadata:
      labels:
        org: null
```

### 참고 경로
- 라벨 딕셔너리 경로: `/spec/template/metadata/labels`
- JSON 6902 연산: `add`, `replace`, `remove`

