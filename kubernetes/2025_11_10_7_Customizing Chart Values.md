# 쿠버네티스

## Customizing Chart Values (설치 시 값 커스터마이징)

### 개요
- 기본 설치는 차트의 `values.yaml` 기본값을 사용.
- 실제 운영에서는 블로그 이름, 사용자 이메일 등 다양한 값을 바꿔야 함.
- 값은 템플릿을 통해 환경변수/필드로 주입되며, `values.yaml`에서 읽어감.

### 방법 1) `--set` 옵션으로 바로 오버라이드
```bash
helm install my-wp bitnami/wordpress \
  --set wordpressBlogName="Helm Tutorials" \
  --set wordpressEmail=john@example.com
```
- `--set <키>=<값>` 형식으로 여러 번 지정 가능.
- 공백이 있는 값은 따옴표로 감싸기.
- 차트마다 키 이름은 다를 수 있으므로 해당 차트의 README/values.yaml 확인.

### 방법 2) 사용자 정의 values 파일 사용
`custom-values.yaml` 예시:
```yaml
wordpressBlogName: "Helm Tutorials"
wordpressEmail: john@example.com
```
적용:
```bash
helm install my-wp bitnami/wordpress -f custom-values.yaml
```
- YAML이므로 `키: 값` 형태 사용(=`=` 아님).
- 여러 파일을 `-f`로 중복 지정 가능(뒤에 오는 파일이 앞을 덮어씀). `--set`이 파일보다 우선.

### 방법 3) 차트를 풀어 로컬에서 값 수정 후 설치
```bash
helm pull bitnami/wordpress --untar
# ./wordpress/values.yaml 직접 편집
helm install my-wp ./wordpress
```
- `--untar`로 압축 해제된 차트 디렉터리가 생성됨(예: `./wordpress`).
- 이후 설치 시 원격 차트 이름 대신 로컬 경로를 지정.

### 동작 배경
- 예: WordPress 배포의 블로그 이름은 Deployment 템플릿의 환경변수로 정의되고, 값은 `values.yaml`에서 주입.
- 설치 중 오버라이드한 값이 최종 매니페스트에 반영되어 클러스터에 적용됨.

### 요약
- 빠르게 바꿀 땐 `--set`, 값이 많으면 `-f custom-values.yaml` 권장.
- 차트 자체를 커스터마이즈하려면 `helm pull --untar` 후 로컬 경로로 설치.
- 키 이름은 차트마다 다르므로 README/values.yaml로 확인하고 적용.
