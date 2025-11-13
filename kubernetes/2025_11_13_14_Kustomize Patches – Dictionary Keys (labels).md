# 쿠버네티스

## Kustomize 패치: 딕셔너리 키(labels) 치환/추가/삭제

### 대상 예시(Deployment 일부)
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
```

---

### JSON 6902로 라벨 값 교체(replace)
`kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml

patchesJson6902:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    patch: |-
      - op: replace
        path: /spec/template/metadata/labels/component
        value: web
```

### JSON 6902로 라벨 키 추가(add)
```yaml
patchesJson6902:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    patch: |-
      - op: add
        path: /spec/template/metadata/labels/org
        value: kodekloud
```

### JSON 6902로 라벨 키 삭제(remove)
```yaml
patchesJson6902:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    patch: |-
      - op: remove
        path: /spec/template/metadata/labels/org
```
- 참고: 여러 연산을 한 번에 수행하려면 위 `patch` 배열에 op들을 순서대로 나열.

---

### Strategic Merge로 라벨 값 교체/추가/삭제
별도 파일 방식(가독성 ↑)

1) 교체(replace) — `label-replace.yaml`
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

2) 추가(add) — `label-add.yaml`
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

3) 삭제(remove) — `label-remove.yaml`
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

`kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml
patchesStrategicMerge:
  - label-replace.yaml
  - label-add.yaml
  - label-remove.yaml
```

### 적용/검증
```bash
kustomize build . | kubectl apply -f -
kubectl get deploy api-deployment -o yaml | rg '^\s*labels:|component:|org:' -n --no-line-number
```

### 요약
- JSON6902: `op/path/value`로 딕셔너리의 특정 키를 정밀 제어(교체/추가/삭제).
- Strategic Merge: 매니페스트 형태로 병합, 삭제는 `null`로 표현.
- 인라인/파일 분리는 선호도와 규모에 맞게 선택.
