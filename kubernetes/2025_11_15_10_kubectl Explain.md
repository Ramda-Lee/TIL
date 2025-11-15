# 쿠버네티스

## kubectl explain과 api-resources

### 개요
`kubectl explain`과 `kubectl api-resources`만으로도 리소스와 필드를 문서 없이 터미널에서 바로 탐색할 수 있다. 리소스 이름/shortName/API 버전이 헷갈리거나, YAML에 어떤 필드가 있는지 알고 싶을 때 유용하다.

### kubectl api-resources
클러스터에 등록된 모든 API 리소스를 나열한다.
```bash
kubectl api-resources
```
- 리소스 전체 이름, SHORTNAMES, APIVERSION, NAMESPACED 여부 등을 확인할 수 있다.
- 정의 파일에서 쓸 정확한 리소스 이름/대소문자/버전이 헷갈릴 때 참고한다.

### kubectl explain 기본 사용
리소스의 최상위 필드 구조를 확인한다.
```bash
kubectl explain pod
```
- `KIND`, `VERSION`, 그리고 `FIELDS` 아래에 `apiVersion`, `kind`, `metadata`, `spec`, `status` 등 top-level 필드와 타입/설명이 나온다.

특정 하위 필드를 보고 싶을 때는 점 표기법을 사용한다.
```bash
kubectl explain pod.spec
kubectl explain pod.spec.containers
kubectl explain pod.spec.containers.ports
```
- 각 필드가 어떤 타입인지, 무엇을 의미하는지, 필수인지 등을 상세히 설명해 준다.

### --recursive로 전체 필드 보기
YAML에 나올 수 있는 모든 필드를 한 번에 보고 싶다면 재귀 옵션을 사용한다.
```bash
kubectl explain pod.spec --recursive
```
- `spec` 아래 모든 중첩 필드를 트리 형태로 나열해 주기 때문에, 이 출력을 참고해 직접 매니페스트를 작성하기 좋다.

### 활용 팁
- 새 리소스를 처음 쓸 때: `kubectl api-resources` → `kubectl explain <resource> --recursive` 순서로 구조를 파악하고 YAML을 작성한다.
- 필드 이름/타입이 기억나지 않을 때: `kubectl explain <resource>.<field>...`로 바로 확인한다.
- 공식 문서를 열기 어려운 환경(원격 서버, 시험 환경 등)에서도 빠르게 스키마를 조회할 수 있는 도구로 익혀 두면 좋다.

