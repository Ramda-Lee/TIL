# 쿠버네티스

## Helm Lifecycle Management(수명 주기 관리)

### 개요
- 릴리스(Release)는 차트를 한 번 설치한 인스턴스로, 여러 쿠버네티스 오브젝트의 묶음.
- Helm은 릴리스별로 관련 오브젝트를 추적하므로, 업그레이드/다운그레이드/삭제를 안전하게 수행.
- 리비전(Revision)으로 과거/현재 상태를 기록해 이력 조회와 롤백이 가능.

### 특정 버전으로 설치(예시)
```bash
# 저장소가 없다면 먼저 추가
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# NGINX를 특정 차트 버전으로 설치 (예: my-nginx)
helm install my-nginx bitnami/nginx --version <chart-version>
```

### 현재 애플리케이션 버전 확인(예시)
```bash
kubectl get pods -l app.kubernetes.io/instance=my-nginx
kubectl describe pod <pod-name>
# 출력에서 컨테이너 이미지 태그(예: nginx:1.19.2 등)를 확인
```

### 업그레이드(Upgrade)
```bash
# 차트 최신 버전 또는 지정 버전으로 업그레이드
helm upgrade my-nginx bitnami/nginx --version <new-chart-version>

# values를 사용 중이었다면 동일하게 전달 (-f/--set)
helm upgrade my-nginx bitnami/nginx -f values.yaml
```
- 업그레이드가 성공하면 리비전이 증가(예: 1 → 2).
- 파드가 교체되므로 새 파드 이름으로 다시 확인.

### 릴리스 목록/이력
```bash
helm list
helm history my-nginx
```
- history 출력에서 각 리비전의 차트/앱 버전, 생성 액션(install/upgrade/rollback) 확인 가능.

### 롤백(Rollback)
```bash
# 리비전 1로 롤백
helm rollback my-nginx 1
```
- 주의: 실제로 리비전 1로 “이동”하는 것이 아니라, 동일 구성을 가진 새로운 리비전이 생성됨(예: 3).  
  즉, `install=1, upgrade=2, rollback(to 1)=3`와 같이 이력이 쌓임.

### 차트별 추가 요구사항(예: WordPress)
- 일부 패키지는 업그레이드 시 추가 매개변수나 자격 증명(예: DB/앱 관리자 비밀번호)이 필요할 수 있음.
- 업그레이드 실패 시 차트의 README/NOTES가 안내하는 파라미터를 추가로 넘겨 해결.
  - 예: `--set mariadb.auth.rootPassword=... --set wordpressPassword=...` 등(차트에 따라 상이).

### 롤백 범위와 한계(데이터)
- Helm의 롤백은 “선언(매니페스트)” 상태 복원에 가깝고, 애플리케이션 데이터(PV 데이터/외부 DB)는 포함하지 않음.
- DB/파일 등 영속 데이터는 별도 백업/복구 도구 필요.
- 일관된 업그레이드/롤백을 위해 차트 훅(Hooks)으로 사전/사후 작업(백업, 마이그레이션 등)을 연계할 수 있음.

### 요약
- 릴리스는 차트 설치 단위, 리비전은 시점 스냅샷. `upgrade`, `history`, `rollback`으로 수명 주기를 관리.
- `helm upgrade <release> <chart> [--version ...]`로 손쉽게 갱신, `helm rollback <release> <rev>`로 되돌림.
- 롤백은 새 리비전을 만들며(복원점 기록), 데이터 복구는 별도 체계가 필요.
- 차트별 요구 파라미터(특히 자격 증명)는 README/NOTES를 참고해 함께 전달.
