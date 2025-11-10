# 쿠버네티스

## Helm CLI 기본과 저장소(Repositories)

### Helm CLI 도움말
- Helm의 모든 작업은 CLI로 수행.
- 기본 도움말: 
```bash
helm help
```
- 하위 명령 도움말:
```bash
helm <subcommand> --help
helm repo help
```
- 예: 실패한 업그레이드 후 이전 상태로 복구하려면 `helm rollback` (헷갈리기 쉬운 `restore` 아님).

### 차트 검색: Hub vs Repo
- Artifact Hub에서 검색:
```bash
helm search hub wordpress
```
  - Hub = artifacthub.io에 등록된 다수 저장소의 메타를 통합 검색.
  - 공식/검증 배지(verified) 여부를 확인해 품질 높은 차트 우선 사용.
- 로컬에 추가된 저장소에서 검색:
```bash
helm search repo wordpress
```

### 저장소 추가/조회/갱신
- 저장소 추가(Bitnami 예시):
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
```
- 저장소 목록:
```bash
helm repo list
```
- 메타데이터 갱신(apt의 `apt-get update` 유사):
```bash
helm repo update
```

### 차트 설치/목록/삭제(WordPress 예시)
- 설치(릴리스 생성):
```bash
helm install my-release bitnami/wordpress
```
  - 설치 완료 후 차트가 출력하는 NOTES를 참고(접속 방법, 초기 자격증명 등).
- 릴리스 목록 조회:
```bash
helm list
```
- 삭제(릴리스 및 관련 리소스 제거):
```bash
helm uninstall my-release
```

### 실습 팁
- 네임스페이스 지정 및 생성:
```bash
helm install my-release bitnami/wordpress -n blog --create-namespace
```
- 값 오버라이드:
```bash
helm install my-release bitnami/wordpress -f custom-values.yaml
helm install my-release bitnami/wordpress --set service.type=NodePort
```
- 특정 차트 버전 지정:
```bash
helm install my-release bitnami/wordpress --version <chart-version>
```

### 요약
- `helm help`와 `--help`로 명령과 옵션을 빠르게 확인.
- `helm search hub`는 Artifact Hub, `helm search repo`는 로컬에 추가된 저장소를 검색.
- `helm repo add/list/update`로 저장소 관리 후 `helm install`로 배포.
- `helm list`로 릴리스 확인, `helm uninstall`로 깔끔하게 제거.
