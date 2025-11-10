# 쿠버네티스

## Helm 구성 요소(Components)와 개념

### 개요
- Helm은 로컬의 CLI, 차트(Chart), 릴리스(Release), 리비전(Revision), 차트 저장소(Repository), 그리고 메타데이터로 구성됨.
- 차트는 여러 쿠버네티스 오브젝트를 한꺼번에 정의/설치하기 위한 설계도이며, 릴리스는 그 차트를 한 번 설치한 인스턴스를 의미.

### 주요 구성 요소
- Helm CLI: 로컬에서 `install`, `upgrade`, `rollback` 등 Helm 작업을 실행하는 명령행 도구.
- Chart: 애플리케이션 설치에 필요한 매니페스트 템플릿과 헬퍼 파일 묶음.
- Release: 특정 차트를 클러스터에 설치한 결과물(인스턴스). 동일 차트로 여러 릴리스 생성 가능.
- Revision: 릴리스의 시점 스냅샷. 설치/업그레이드/롤백 등 의미있는 변경 시 새 리비전 생성.
- Repository: 공개 차트가 호스팅되는 저장소. 다양한 공급자가 제공하며 아티팩트 허브에서 검색 가능.
- Metadata: 릴리스/차트/리비전 상태 등 Helm이 남기는 이력 데이터. 클러스터의 Kubernetes Secret에 저장됨.

### Chart, 템플릿, values.yaml
- 차트는 다수의 오브젝트(예: Deployment, Service, Secret, ConfigMap, PV/PVC 등)를 템플릿으로 정의.
- 템플릿 내 값은 보통 `values.yaml`로 외부화되어 배포 시 손쉽게 커스터마이징.
- 예) 간단한 Hello World 웹앱:
  - Deployment(NGINX 이미지, `replicas` 등)를 템플릿 변수로 정의
  - Service(NodePort 등)로 노출
- 대부분의 경우 차트 자체를 수정하기보다, `values.yaml`만 수정해 원하는 설정으로 배포.

### Release와 Revision
- `helm install <release> <chart>` 형태로 릴리스를 생성.
  - 예: `helm install my-site bitnami/wordpress`
  - 같은 차트로 `my-SECOND-site` 등 복수 릴리스 설치 가능 → 서로 독립적으로 추적/변경.
- 리비전은 설치(1), 업그레이드(2), 롤백(3)처럼 변경 시마다 증가하며, 특정 시점 상태로의 복원이 가능.

### 차트 저장소와 Artifact Hub
- 공개 차트는 전 세계 다양한 저장소(예: Bitnami, AppsCode, Community Operators, TrueCharts 등)에 존재.
- Artifact Hub(artifacthub.io)에서 여러 저장소의 차트를 한곳에서 검색/탐색 가능.
- 공식/검증 퍼블리셔 배지가 있는 차트를 우선적으로 사용하는 것이 바람직.

### 메타데이터 저장: Kubernetes Secret
- Helm은 릴리스/차트/리비전 등 메타데이터를 클러스터 내부의 Secret에 저장.
- 팀원이 어디서든 같은 클러스터에 접근하면 동일한 릴리스 이력을 공유 → 업그레이드/롤백 등 협업이 용이.

### 요약
- Helm은 차트(템플릿+values), 릴리스(설치 인스턴스), 리비전(스냅샷), 저장소(공개 차트), 메타데이터(Secret)로 구성됨.
- 차트는 앱을 쿠버네티스 오브젝트 묶음으로 정의하고, `values.yaml`로 설정을 외부화.
- 동일 차트로 여러 릴리스를 독립적으로 운영할 수 있고, 리비전을 통해 변경 이력/롤백을 관리.
- 공개 차트는 Artifact Hub에서 찾고, Helm 메타데이터는 클러스터 Secret에 저장되어 팀 협업에 유리.
