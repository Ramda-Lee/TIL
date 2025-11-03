# 쿠버네티스

## Image Security

### 이미지 이름 구조
- `nginx` 같은 이름은 Docker의 이미지 네이밍 규칙을 따름  
  → 실제로는 `library/nginx` 형태임  
- 구조:  
  ```
  <namespace>/<repository>:<tag>
  ```
- `library`는 기본 계정(namespace)으로, Docker의 공식 이미지들이 저장됨  
- 사용자가 직접 만든 경우 예:  
  ```
  mycompany/myapp:latest
  ```


### 이미지 저장소 (Registry)
- 이미지를 저장하고 배포하는 장소  
- 기본값: Docker Hub (`docker.io`)  
- 기타 예시
  - Google Container Registry → `gcr.io`
  - AWS Elastic Container Registry → `ECR`
  - Azure Container Registry → `ACR`
- Private Registry (사설 저장소)를 사용하면 내부 애플리케이션을 외부에 노출하지 않고 관리 가능

### Private Registry 접근
- Private 이미지 사용 시 인증이 필요함  
- Docker CLI에서는 다음 순서로 인증 및 실행

```bash
# 로그인
docker login <registry-server>

# 로그인 성공 후 이미지 실행
docker run <private-registry>/<image>:<tag>
```
## Kubernetes에서 Private Image 사용

### 개요
Kubernetes의 Pod가 Private Registry의 이미지를 가져오려면  
Docker 자격 증명 정보를 Kubelet에 전달해야 함.

이때 사용하는 것이 Secret (docker-registry 타입)

### Secret 생성 예시
```bash
kubectl create secret docker-registry myregistrysecret   --docker-server=<registry-server>   --docker-username=<username>   --docker-password=<password>   --docker-email=<email>
```

### Pod 정의 예시
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: myapp
spec:
  containers:
    - name: app
      image: myregistry.com/myimage:latest
  imagePullSecrets:
    - name: myregistrysecret
```

> Pod가 생성될 때, Kubelet은 Secret의 인증 정보를 사용하여 Private Registry에서 이미지를 Pull함.

### 핵심 요약
| 항목 | 설명 |
|------|------|
| 이미지 이름 구조 | `<namespace>/<repository>:<tag>` |
| 기본 Registry | `docker.io` (Docker Hub) |
| Private Registry | 인증 필요, Secret으로 처리 |
| Kubernetes 연동 방식 | `imagePullSecrets`로 Secret 지정 |
| Secret 타입 | `kubernetes.io/dockerconfigjson` (`docker-registry`) |

### 결론
Kubernetes 환경에서 보안을 강화하려면:
1. Private Registry를 사용하여 내부용 이미지를 외부로부터 보호하고,  
2. Docker 자격 증명을 Kubernetes Secret으로 안전하게 저장하며,  
3. Pod의 `imagePullSecrets` 항목에서 해당 Secret을 참조하도록 설정해야 한다.
