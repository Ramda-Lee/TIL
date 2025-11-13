# 쿠버네티스

## Kustomize 패치: Inline vs 별도 파일

### 개요
- 패치는 두 방식으로 정의 가능:
  - Inline: `kustomization.yaml` 안에 바로 작성
  - 별도 파일: 패치 내용을 외부 YAML 파일로 분리 후 참조
- JSON 6902와 Strategic Merge 모두 지원(전자는 `patchesJson6902`, 후자는 `patchesStrategicMerge`가 전통적; 통합 키 `patches`도 존재).

### JSON 6902 — Inline 예시
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
        path: /spec/replicas
        value: 5
```

### JSON 6902 — 별도 파일 예시
`replicas-patch.json.yaml`(패치 연산 배열)
```yaml
- op: replace
  path: /spec/replicas
  value: 5
```
`kustomization.yaml`
```yaml
patchesJson6902:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    path: replicas-patch.json.yaml
```

### Strategic Merge — 별도 파일 예시(권장)
`name-patch.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-deployment
spec:
  replicas: 5
```
`kustomization.yaml`
```yaml
patchesStrategicMerge:
  - name-patch.yaml
```

### Strategic Merge — Inline 예시(통합 키 사용)
- 최신 스키마에서 `patches` 키로 inline 가능(호환성 주의).
```yaml
patches:
  - target:
      group: apps
      version: v1
      kind: Deployment
      name: api-deployment
    patch: |-
      apiVersion: apps/v1
      kind: Deployment
      metadata:
        name: api-deployment
      spec:
        replicas: 5
```

### 어떤 방식을 쓸까
- Inline
  - 장점: 간단한 변경을 한 파일에서 관리, 참조가 적음
  - 단점: 패치가 많아지면 `kustomization.yaml`이 비대해짐
- 별도 파일
  - 장점: 파일 분리로 가독성/재사용/리뷰 용이, 대규모 변경에 적합
  - 단점: 파일 수 증가, 경로 관리 필요

### 팁
- 팀/CI에서 여러 kustomize/kubectl 버전을 혼용하면, 보수적으로 `patchesJson6902`/`patchesStrategicMerge` 사용 권장.
- JSON6902는 배열/필드 조작에 섬세, StrategicMerge는 매니페스트 유사 문법으로 가독성 우수.

### 적용/검증
```bash
kustomize build . | kubectl apply -f -
kubectl get deploy api-deployment -o jsonpath='{.spec.replicas}{"\n"}'
```

### 요약
- Inline/파일 분리는 선택 사항. 규모와 팀 선호에 따라 결정.
- JSON6902는 `patch`(inline)/`path`(file) 모두 지원, StrategicMerge는 파일 방식이 일반적이나 `patches`로 inline도 가능.
