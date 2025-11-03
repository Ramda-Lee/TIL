# 쿠버네티스

## Security Contexts

### 개요
Docker 컨테이너를 실행할 때 사용자 ID나 Linux Capabilities(권한 기능) 등을 설정할 수 있다.  
Kubernetes에서도 동일하게 보안 설정(Security Context)을 적용할 수 있다.  
Kubernetes에서는 모든 컨테이너가 Pod 내에서 실행되므로,  
보안 컨텍스트는 Pod 수준 또는 Container 수준 중 하나에 설정할 수 있다.

### 보안 컨텍스트 적용 위치
| 수준 | 설명 |
|------|------|
| Pod 수준 | Pod 내 모든 컨테이너에 동일한 보안 설정이 적용됨 |
| Container 수준 | 특정 컨테이너에만 보안 설정 적용 |
| 동시에 설정 시 | Container 수준 설정이 Pod 수준 설정을 덮어씀 (override) |

### 예시: Pod 정의 파일

#### Pod 수준에서 설정
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
spec:
  securityContext:
    runAsUser: 1000
  containers:
    - name: ubuntu
      image: ubuntu
      command: ["sleep", "3600"]
```

→ Pod 내 모든 컨테이너가 UID 1000으로 실행된다.

#### Container 수준에서 설정
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-container-pod
spec:
  containers:
    - name: ubuntu
      image: ubuntu
      command: ["sleep", "3600"]
      securityContext:
        runAsUser: 1000
        capabilities:
          add: ["NET_ADMIN", "SYS_TIME"]
```

→ 해당 컨테이너만 `runAsUser=1000`, `NET_ADMIN`, `SYS_TIME` 등의 Capabilities를 가진다.

### 주요 옵션 정리
| 옵션 | 설명 |
|------|------|
| `runAsUser` | 컨테이너를 실행할 사용자 UID 설정 |
| `runAsGroup` | 실행 그룹(GID) 설정 |
| `fsGroup` | 마운트된 볼륨의 파일 권한을 설정 |
| `capabilities.add` | 컨테이너에 추가할 Linux Capability 목록 |
| `capabilities.drop` | 제거할 Capability 목록 |
| `privileged` | 모든 권한을 가진 컨테이너로 실행 (주의 필요) |

### 요약 정리

- Kubernetes에서는 Pod 또는 Container 단위로 보안 설정을 적용할 수 있다.  
- Container의 Security Context가 Pod의 설정보다 우선순위가 높음.  
- `runAsUser`, `capabilities`, `privileged` 등을 통해 세밀한 보안 제어 가능.  
- 보안 컨텍스트 설정은 Pod YAML 파일 내에서 직접 정의한다.

### 결론
Kubernetes의 Security Context는 컨테이너가 실행되는 보안 환경(사용자, 권한, 접근 범위) 을 정의하는 핵심 도구이다.  
보안 수준이 높은 운영 환경에서는 각 컨테이너에 최소 권한 원칙(Principle of Least Privilege)을 적용하도록 설정하는 것이 중요하다.
