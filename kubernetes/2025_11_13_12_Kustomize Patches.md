# 쿠버네티스

## Kustomize Patches (JSON6902 / Strategic Merge)

### 언제 패치를 쓰나
- 공통 변환(commonLabels/namespace 등)은 전체 리소스에 일괄 적용할 때 사용.
- 패치(패치 트랜스포머)는 특정 리소스의 특정 필드만 “정밀하게” 바꿀 때 사용(예: 특정 Deployment의 replicas만 변경).

### 1) JSON 6902 패치
- 구성 요소: target(대상 매칭) + operations(op/path/value 등).
- 주요 op: add, remove, replace.

`kustomization.yaml` 예시(이름 바꾸기: api-deployment → web-deployment)
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
        path: /metadata/name
        value: web-deployment
```

replicas 변경 예시(1 → 5)
```yaml
patchesJson6902:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    patch: |-
      - op: replace
        path: /spec/replicas
        value: 5
```
- path는 YAML 트리 경로를 `/`로 기술. 더 깊으면 계층을 계속 추가.

### 2) Strategic Merge Patch(전략적 병합)
- “일반 쿠버네티스 매니페스트 형태”로 바꿀 부분만 적고 병합.
- 장점: 읽기 쉬움, 기존 매니페스트 복사 후 필요한 필드만 남기면 됨.

`replicas`를 5로 바꾸는 예시(별도 파일로 저장: `replicas-patch.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment   # 대상 식별에 필요
spec:
  replicas: 5
```

`kustomization.yaml`에서 참조
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - deployment.yaml

patchesStrategicMerge:
  - replicas-patch.yaml
```

### 3) 통합 키 `patches`(신형 문법) 참고
- 일부 버전에서는 `patches:` 아래에 `target`과 `patch`(inline 또는 `path`)를 혼용 지원.
- 호환성 위해 `patchesJson6902`/`patchesStrategicMerge`를 사용하는 것이 안전함.

### 타깃 매칭 팁
- target은 `group/version/kind/name/namespace/labelSelector/annotationSelector` 등을 조합해 특정(들)을 지정.
- 동일 Kind가 여러 개면 name·ns로 한정해 오적용 방지.

### 적용/검증
```bash
kustomize build . | kubectl apply -f -
kubectl get deploy web-deployment -o yaml | rg 'name:|replicas:'
```

### 요약
- JSON6902: op/path/value로 정밀 제어(배열/필드 단위 조작에 유리).
- StrategicMerge: 매니페스트 형태로 직관적 병합(가독성 ↑).
- 프로젝트/팀 선호에 따라 선택·혼용 가능. 루트/서브디렉터리 위치에 따라 적용 범위가 달라짐.
