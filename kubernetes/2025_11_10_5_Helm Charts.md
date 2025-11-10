# 쿠버네티스

## Helm Charts (차트 기본)

### 개요
- Helm은 CLI에서 단순 명령(install/upgrade/rollback/uninstall)만 주면, 다단계 작업을 자동 수행하는 도구.
- 그 “어떻게”를 알려주는 설계도가 Chart(차트). 여러 파일로 구성되며, 쿠버네티스 오브젝트를 만들 지침과 템플릿을 포함.

### 차트와 템플릿, values.yaml
- 차트의 템플릿(Deployment, Service 등)은 값 자리에 템플릿 변수를 사용하고, 실제 값은 `values.yaml`로 주입.
- 예: 간단한 웹앱 차트는 Deployment(이미지, replicas 등) + Service(NodePort 등)를 템플릿으로 정의하고, `values.yaml`로 커스터마이즈.
- 배포 커스터마이징 시 대부분 `values.yaml`만 변경하면 됨.

### release와 revision (개념 복습)
- 차트를 클러스터에 적용하면 릴리스(Release)가 생성됨: 특정 차트의 설치 인스턴스.
- 릴리스는 리비전(Revision) 이력을 가지며, 설치/업그레이드/롤백 등 의미 있는 변경 때마다 증가(스냅샷 역할).
- 동일 차트로 복수 릴리스 생성 가능: 예) `my-site`, `my-SECOND-site` 두 개의 워드프레스 사이트를 독립 운영/변경.

### chart.yaml 핵심 필드
- `apiVersion`: 차트 스펙 버전. Helm 3에서는 보통 `v2` 사용.
  - Helm 2 시절엔 없었고, Helm 3에서 `dependencies`, `type` 등 새 개념을 도입하며 구분 필요 → `v2`.
  - 오래된(Helm 2용) 차트는 이 값이 없거나 `v1`일 수 있음.
- `name`: 차트 이름(예: `wordpress`).
- `description`: 차트 설명.
- `type`: `application`(기본) 또는 `library`.
  - application: 실제 앱 배포용 차트.
  - library: 템플릿 헬퍼/유틸 제공(다른 차트에서 재사용).
- `version`: 차트 자체의 버전(차트 변경 추적용, 앱 버전과 독립).
- `appVersion`: 차트가 배포하는 애플리케이션의 버전(예: WordPress 버전). 정보 제공용.
- `dependencies`: 외부 차트 의존성 정의.
  - 예: WordPress 차트가 DB로 `mariadb` 차트를 의존성으로 선언해 함께 배포.
- `keywords`, `maintainers`, `home`, `icon`: 검색/문서/표시용 메타데이터.

### 차트 디렉토리 구조(예)
```
mychart/
  Chart.yaml          # 차트 메타데이터(apiVersion, name, version, appVersion, type, dependencies ...)
  values.yaml         # 기본 값(설정) 정의, 설치 시 -f로 오버라이드 가능
  templates/          # 쿠버네티스 매니페스트 템플릿들
    deployment.yaml
    service.yaml
    _helpers.tpl      # 템플릿 헬퍼(선택)
  charts/             # 의존성 차트가 vendor 형태로 담길 수 있음(선택)
  README.md           # 차트 사용법(선택)
  LICENSE             # 라이선스(선택)
```

### 동작 흐름 요약
1) 사용자는 원하는 결과(설치/업그레이드/롤백)를 Helm CLI에 지시.
2) Helm은 차트의 템플릿과 `values.yaml`을 렌더링해 최종 매니페스트 생성.
3) 쿠버네티스에 적용하여 필요한 오브젝트들을 생성/갱신.
4) 릴리스 메타데이터(리비전 등)는 클러스터 내 Secret에 저장되어 이력/협업이 가능.

### 요약
- 차트는 Helm이 “무엇을 어떻게 만들지” 알게 해주는 설계도이며, 템플릿+values로 구성.
- `chart.yaml`은 차트 메타데이터 중심(특히 `apiVersion v2`, `type`, `dependencies` 등)이고, `values.yaml`은 환경별 설정 입력 파일.
- 동일 차트로 여러 릴리스를 독립 운영 가능, 리비전으로 변경 이력과 롤백을 관리.
- 의존성 차트를 `dependencies`로 선언해 복잡한 앱(예: WordPress+MariaDB)을 모듈형으로 구성 가능.
