# 쿠버네티스

## Kustomize Image Transformer

### 개요
- `images` 변환을 통해 매니페스트 전반에 사용된 컨테이너 이미지를 일괄 치환/태그 변경.
- Deployment/StatefulSet/DaemonSet 등 Pod 스펙의 `containers[*].image`를 대상으로 동작.

### 기본 문법(kustomization.yaml)
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - deployment.yaml

images:
  - name: nginx         # 원본 이미지 이름(레지스트리/리포지토리 포함 가능)
    newName: haproxy    # 새 이미지 이름
```
- 결과: `nginx` → `haproxy`

### 태그만 변경하기
```yaml
images:
  - name: nginx
    newTag: "2.4"
```
- 결과: `nginx` → `nginx:2.4`

### 이름과 태그 동시 변경
```yaml
images:
  - name: nginx
    newName: haproxy
    newTag: "2.4"
```
- 결과: `nginx` → `haproxy:2.4`

### 다이제스트 사용(태그 대신)
```yaml
images:
  - name: nginx
    newDigest: sha256:deadbeef...
```
- 결과: `nginx@sha256:deadbeef...` (newDigest가 지정되면 태그보다 우선)

### 레지스트리/리포지토리 매칭 주의
- `name`에는 `nginx`, `library/nginx`, `docker.io/library/nginx` 등 실제 매니페스트에 기록된 형태와 맞게 지정해야 안정적으로 매칭됨.
- 사설 레지스트리를 치환할 때는 `name: my-registry.local/app/web`처럼 전체 경로를 기준으로 매칭 권장.

### 예시: 원본 Deployment 일부
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web
spec:
  replicas: 1
  template:
    spec:
      containers:
        - name: web
          image: nginx
```

위에 `images` 변환을 적용하면 최종 산출물의 `image` 필드가 변경됨.

### 적용/검증
```bash
kustomize build . | kubectl apply -f -
kubectl get deploy web -o yaml | rg 'image:'
```

### 요약
- `images`로 이미지 이름(newName), 태그(newTag), 다이제스트(newDigest)를 선언적으로 변경.
- 이름과 태그를 함께 지정하면 `newName:newTag`로, 다이제스트가 있으면 다이제스트가 우선.
- `name` 매칭은 원본 표기와 일치시키는 것이 안전하며, 여러 리소스에 걸쳐 일관 적용 가능.
