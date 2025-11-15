# 쿠버네티스

## Kustomize 오버레이(환경별 커스터마이징)

### 개요
Kustomize의 핵심 목적은 공통 베이스 설정을 바탕으로 환경별(dev/staging/prod)로 일부 속성을 손쉽게 변경하는 것이다. 이를 위해 base와 overlays 디렉터리 구조를 사용하고, 오버레이에서 패치와 추가 리소스를 정의한다.

### 디렉터리 구조 예시
```
app/
  base/
    kustomization.yaml
    nginx-depl.yaml
  overlays/
    dev/
      kustomization.yaml
      patch-replicas.yaml
    staging/
      kustomization.yaml
      patch-replicas.yaml
    prod/
      kustomization.yaml
      patch-replicas.yaml
      grafana-depl.yaml
```

### base/kustomization.yaml
공유 리소스를 선언한다.
```yaml
resources:
  - nginx-depl.yaml
```

### overlays/dev/kustomization.yaml
베이스를 참조하고 환경별 패치를 적용한다. (일부 버전에서는 `bases` 대신 `resources`로 상위 경로를 참조해도 된다.)
```yaml
bases:
  - ../../base
patches:
  - path: patch-replicas.yaml
```

patch-replicas.yaml (예: 레플리카 2로 변경)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-depl
spec:
  replicas: 2
```

### overlays/prod/kustomization.yaml
프로덕션 전용 패치와 신규 리소스를 함께 포함할 수 있다.
```yaml
bases:
  - ../../base
patches:
  - path: patch-replicas.yaml  # 예: replicas: 3
resources:
  - grafana-depl.yaml          # base에는 없는 신규 리소스 추가
```

patch-replicas.yaml (예: 레플리카 3으로 변경)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-depl
spec:
  replicas: 3
```

### 포인트 정리
- base에는 모든 환경이 공통으로 사용하는 기본 리소스를 둔다.
- overlays에는 각 환경 전용 패치와 추가 리소스를 둔다.
- 오버레이에서는 패치뿐 아니라 신규 리소스도 자유롭게 추가할 수 있다.
- 디렉터리 구조는 유연하다. 기능별 하위 폴더로 세분화해도 되며, base와 overlays의 하위 구조가 꼭 일치할 필요는 없다.

