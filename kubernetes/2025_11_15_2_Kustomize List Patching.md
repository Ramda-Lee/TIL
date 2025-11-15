# 쿠버네티스

## Kustomize 패치 (리스트 항목: 컨테이너 교체/추가/삭제)

### 전제 리소스 예시
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    spec:
      containers:
      - name: nginx
        image: nginx
```

### JSON 6902로 리스트 항목 교체(replace)
첫 번째 컨테이너(인덱스 0)의 name과 image를 교체한다.
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
        path: /spec/template/spec/containers/0/name
        value: haproxy
      - op: replace
        path: /spec/template/spec/containers/0/image
        value: haproxy
```

### Strategic Merge로 리스트 항목 교체
컨테이너 리스트는 name을 머지 키로 사용하므로 대상 name을 지정해 변경한다.
```yaml
resources:
  - api-deployment.yaml
patches:
  - path: container-patch.yaml
```

container-patch.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    spec:
      containers:
      - name: nginx
        image: haproxy
```

### JSON 6902로 리스트 항목 추가(add)
리스트 끝에 새 컨테이너를 추가한다. 인덱스 숫자를 쓰면 특정 위치에 삽입 가능하다.
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
        path: /spec/template/spec/containers/-
        value:
          name: haproxy
          image: haproxy
```

### Strategic Merge로 리스트 항목 추가
추가할 컨테이너만 나열하면 병합 시 기존과 합쳐진다.
```yaml
resources:
  - api-deployment.yaml
patches:
  - path: container-add.yaml
```

container-add.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    spec:
      containers:
      - name: haproxy
        image: haproxy
```

### JSON 6902로 리스트 항목 삭제(remove)
두 번째 컨테이너(인덱스 1)를 삭제한다.
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
        path: /spec/template/spec/containers/1
```

### Strategic Merge로 리스트 항목 삭제($patch: delete)
삭제할 리스트 항목에 `$patch: delete`를 지정한다.
```yaml
resources:
  - api-deployment.yaml
patches:
  - path: container-del.yaml
```

container-del.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    spec:
      containers:
      - name: database
        $patch: delete
```

### 참고
- 리스트 인덱스는 0부터 시작한다.
- JSON 6902에서 `/-`는 리스트 끝에 추가를 의미한다.
- Strategic Merge는 컨테이너 리스트의 머지 키(name)로 항목을 식별한다.

