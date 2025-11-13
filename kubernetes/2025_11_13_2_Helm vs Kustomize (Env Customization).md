# 쿠버네티스

## Helm vs Kustomize (환경별 커스터마이징 관점)

### 개요
- 두 도구 모두 환경별로 매니페스트 값을 달리 적용하려는 요구를 해결하지만 접근 방식이 다름.
- 선택 시 팀 역량, 복잡도, 재사용성, 가독성 등을 고려해야 함.

### Helm의 접근(Go 템플릿 + values)
- 템플릿 언어: Go 템플릿을 사용해 매니페스트에 변수 삽입(`{{ ... }}` 구문).
- 값 주입: `values.yaml`에서 변수 값을 정의하고 배포 시 렌더링.
- 환경 분리: 환경별 값 파일을 분리해 사용.
  - 예: `values.dev.yaml`, `values.staging.yaml`, `values.prod.yaml`
  - 배포 시 `-f`로 파일 선택: `helm install app ./chart -f values.staging.yaml`

### 예시: replicaCount, image.tag
- 템플릿(예: `templates/deployment.yaml`)
```yaml
spec:
  replicas: {{ .Values.replicaCount }}
  template:
    spec:
      containers:
        - name: app
          image: {{ .Values.image.repository }}:{{ .Values.image.tag }}
```
- 기본 값(`values.yaml`)
```yaml
replicaCount: 1
image:
  repository: nginx
  tag: 2.4.4
```
- 스테이징 값(`values.staging.yaml`)
```yaml
replicaCount: 2
image:
  tag: 2.5.0
```
- 프로덕션 값(`values.prod.yaml`)
```yaml
replicaCount: 5
image:
  tag: 2.5.0
```

### Helm 차트 구조(요약)
```
mychart/
  Chart.yaml
  values.yaml
  templates/
    deployment.yaml
    service.yaml
environments/
  values.dev.yaml
  values.staging.yaml
  values.prod.yaml
```
- 배포 시: `helm install myapp ./mychart -f environments/values.staging.yaml`

### Helm의 추가 기능(패키지 관리자 역할)
- 조건/반복/함수/훅(install/upgrade/rollback 전후 작업) 등 고급 기능 제공.
- 아티팩트 허브/레포지토리 기반 배포, 릴리스/리비전 관리 등 풍부한 생태계.

### Helm의 단점(복잡도/가독성)
- 템플릿이 섞인 파일은 “순수 YAML”이 아니므로, 읽기/정적 검증이 어렵고 난이도가 올라감.
- 복잡한 차트는 변수와 조건이 많아 의도를 파악하기 어려울 수 있음.

### Kustomize의 접근(순수 YAML + Base/Overlay)
- 템플릿 언어 없이 Base를 공통으로 두고, Overlay에서 변경점만 패치.
- 장점: 가독성 높음, 표준 YAML 그대로 유지, `kubectl` 내장.
- 단점: 템플릿 로직(조건/반복/함수/훅) 같은 동적 기능은 없음(다른 방식으로 해결 필요).

### 선택 가이드(요지)
- 단순·명확·YAML 중심: Kustomize 유리.
- 패키지 배포/릴리스 관리와 풍부한 기능(훅/조건/반복)이 필요: Helm 유리.
- 팀의 선호/숙련도, 운영 복잡도, 배포 파이프라인과의 통합 방식을 고려해 결정.

### 요약
- Helm: 템플릿 + values 파일로 환경별 값 주입, 패키지 관리자 기능 풍부(복잡도↑).
- Kustomize: Base/Overlay로 차이만 선언, 순수 YAML(가독성↑, 동적 기능↓).
- 프로젝트 특성에 맞춰 둘 중 하나 또는 혼용 전략을 채택.
