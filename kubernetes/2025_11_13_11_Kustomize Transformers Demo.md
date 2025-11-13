# 쿠버네티스

## Kustomize Transformers 데모(공통 변환 + 이미지 변환)

### 시나리오 개요
- 디렉터리: `k8s/`
  - `api/`와 `db/`에 각 리소스(deployment/service/configmap 등)와 `kustomization.yaml` 존재
  - 루트 `k8s/kustomization.yaml`는 `api`, `db` 서브디렉터리를 참조
- 목표: 공통 라벨/네임스페이스/접두·접미어와 디렉터리별 라벨/접미어를 적용하고, DB 이미지를 치환/태그 변경

### 1) 루트에 공통 라벨 추가(commonLabels)
`k8s/kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - api
  - db

commonLabels:
  department: engineering
```
- 효과: 루트에 선언했으므로 api/db 모든 리소스에 `department=engineering` 적용

### 2) 서브디렉터리에만 라벨 추가(범위 차이 확인)
`k8s/api/kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - api-depl.yaml
  - api-service.yaml

commonLabels:
  feature: api
```
`k8s/db/kustomization.yaml`에도 유사하게 추가:
```yaml
commonLabels:
  feature: db
```
- 효과: 각 디렉터리 내부 리소스에만 `feature` 라벨이 적용(서브디렉터리 범위)

### 3) 네임스페이스/접두·접미어 적용
루트에서 전체 공통 적용:
```yaml
namespace: debugging
namePrefix: KodeKloud-
```
서브디렉터리별 접미어 지정:
```yaml
# k8s/api/kustomization.yaml
nameSuffix: -web

# k8s/db/kustomization.yaml
nameSuffix: -storage
```
- 결과 예: `api-deployment` → `KodeKloud-api-deployment-web` (ns: debugging)

### 4) 공통 어노테이션 추가(commonAnnotations)
루트에서 전역 적용:
```yaml
commonAnnotations:
  logging: verbose
```

### 5) 이미지 변환(images) — DB만 변경
`k8s/db/kustomization.yaml`에 추가:
```yaml
images:
  - name: mongo
    newName: postgres
    newTag: "4.2"
```
- 주의: `newTag`는 문자열로 따옴표 필요(예: "4.2").
- 컨테이너 `name` 필드와 무관하며, `image` 값의 레퍼런스만 매칭.

### 6) 빌드/적용/검증
```bash
# 렌더링 확인(적용 아님)
kustomize build k8s

# 적용(파이프)
kustomize build k8s | kubectl apply -f -
# 또는 네이티브
kubectl apply -k k8s

# 검증
kubectl get pods -n debugging
kubectl get deploy -n debugging -o yaml | rg '^\s*name:|image:|namespace:' -n --no-line-number
```

### 동작 포인트 정리
- 루트에 선언한 변환은 모든 하위 리소스에 적용, 서브디렉터리의 변환은 해당 디렉터리 리소스에만 적용.
- `namePrefix/nameSuffix`는 참조된 리소스 이름도 가능한 한 자동 갱신하나, 임베디드 문자열은 수동 패치가 필요할 수 있음.
- 이미지 변환의 `name`은 원본 `image` 표기와 일치하도록 지정(레지스트리/리포지토리 포함 형태 주의).

### 요약
- 공통 변환으로 전역 일관성(라벨/어노테이션/네임스페이스/이름 규칙)을 확보하고, 폴더별 변환으로 세분화.
- 이미지 변환으로 이름/태그를 선언적으로 교체하며, 태그는 문자열로 지정.
- `kustomize build`는 출력만, 실제 배포는 `kubectl apply -k` 또는 파이프 사용.
