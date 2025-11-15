# 쿠버네티스

## Kustomize Components (재사용 가능한 기능 블록)

### 개요
Components는 선택적 기능(캐싱, 외부 DB 등)을 하나의 재사용 가능한 블록으로 묶어 여러 오버레이에서 손쉽게 활성화할 수 있게 해준다. 복붙 없이 한 곳에서 관리하고, 필요한 오버레이에서만 포함한다.

### 디렉터리 예시
```
app/
  base/
    kustomization.yaml
    api-depl.yaml
  overlays/
    dev/kustomization.yaml
    premium/kustomization.yaml
    standalone/kustomization.yaml
  components/
    caching/      # Redis 등 캐싱 기능 묶음
      kustomization.yaml
      redis-depl.yaml
      redis-secret.yaml
    database/     # 외부 DB(Postgres) 기능 묶음
      kustomization.yaml
      postgres-depl.yaml
      deployment-patch.yaml
```

### Component 정의 (database 예시)
components/database/kustomization.yaml
```yaml
apiVersion: kustomize.config.k8s.io/v1alpha1
kind: Component
resources:
  - postgres-depl.yaml
generators:
  # 예시: Secret/ConfigMap 생성기 사용 가능(버전에 따라 필드명 다를 수 있음)
patches:
  - path: deployment-patch.yaml
```

components/database/postgres-depl.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
spec:
  replicas: 1
  selector:
    matchLabels: { app: postgres }
  template:
    metadata:
      labels: { app: postgres }
    spec:
      containers:
      - name: postgres
        image: postgres:15
        env:
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
```

components/database/deployment-patch.yaml (API 배포에 환경변수 추가)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  template:
    spec:
      containers:
      - name: api
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
```

### Overlay에서 컴포넌트 포함
overlays/dev/kustomization.yaml
```yaml
bases:
  - ../../base
components:
  - ../../components/database
```

overlays/premium/kustomization.yaml (여러 컴포넌트 동시 포함)
```yaml
bases:
  - ../../base
components:
  - ../../components/database
  - ../../components/caching
```

### 포인트
- 공통(base)에 넣기 애매한 “일부 오버레이만 사용하는 기능”을 컴포넌트로 묶는다.
- 리소스, 패치, 시크릿/컨피그맵 생성 등 해당 기능에 필요한 모든 구성을 컴포넌트 폴더에 모은다.
- 오버레이에선 `components:` 항목으로 손쉽게 활성화한다.
- 구조는 유연하다. 기능별로 자유롭게 세분화 가능하며, 여러 오버레이에서 조합할 수 있다.

