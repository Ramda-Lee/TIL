# 쿠버네티스

## Kustomize Common Transformers

### 개요
- 공통 변환으로 여러 매니페스트에 일괄 적용할 설정을 선언적으로 추가/수정.
- 대표 항목: `commonLabels`, `commonAnnotations`, `namespace`, `namePrefix`, `nameSuffix`.

### 기본 예시(kustomization.yaml)
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - deployment.yaml
  - service.yaml

namespace: demo

namePrefix: dev-
nameSuffix: -v1

commonLabels:
  org: KodeKloud
  env: dev

commonAnnotations:
  owner: platform-team
  note: "managed by kustomize"
```

### 효과
- namespace: 모든 리소스를 `demo` 네임스페이스로 지정(리소스 정의에 ns가 없을 경우 부여/대체).
- namePrefix/nameSuffix: 모든 리소스 이름에 접두/접미어 추가(예: `nginx` → `dev-nginx-v1`).
- commonLabels: 모든 리소스의 `metadata.labels`와 셀렉터에 라벨을 추가(가능한 경우).
- commonAnnotations: 모든 리소스의 `metadata.annotations`에 주석 추가.

### 적용/검증
```bash
kustomize build . | kubectl apply -f -
kubectl get all -n demo -l env=dev
```

### 주의사항
- 이름 참조(NameReference)가 자동으로 갱신되는 필드는 대부분 안전하게 변경되지만, 임베디드 문자열이나 커스텀 필드는 자동 반영되지 않을 수 있음 → 필요한 경우 패치로 보완.
- 기존 리소스에 이미 `namespace`/라벨/어노테이션이 있다면 병합 규칙에 따라 결합(동일 키는 덮어씀).

### 요약
- 공통 변환으로 네임스페이스/라벨/어노테이션/이름 규칙을 한 곳에서 일괄 관리.
- 대규모 리소스 집합에서 수작업 반복을 제거하고 일관성을 보장.
