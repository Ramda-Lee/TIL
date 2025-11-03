# 쿠버네티스

## Service Accounts

### Overview
Kubernetes에는 두 가지 계정 유형이 있다.
- User Account: 사람이 사용하는 계정 (관리자, 개발자 등)
- Service Account: 애플리케이션이나 시스템이 사용하는 계정 (예: Prometheus, Jenkins 등)

### Service Account의 역할
Service Account는 Pod나 애플리케이션이 Kubernetes API와 상호작용할 수 있도록 하는 ID
- 서비스 계정은 Token과 연결됨
- Token은 API 접근 시 Bearer Token 형식으로 인증에 사용됨
- 비유하자면, Service Account는 ‘신분증’, Token은 ‘신분증의 바코드’ 역할

### 기본 Service Account
- 모든 Namespace에는 기본적으로 `default` 서비스 계정이 존재함
- 명령어:
  ```bash
  kubectl get serviceaccount
  kubectl describe serviceaccount
  ```
- 모든 Pod는 자동으로 기본 서비스 계정을 할당받음
- Pod 내부의 `/var/run/secrets/kubernetes.io/serviceaccount/` 디렉토리에 Token이 Projected Volume으로 자동 마운트됨

### 커스텀 Service Account 생성

#### 명령어 방식
```bash
kubectl create serviceaccount dashboard-sa
```

#### YAML 방식
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: dashboard-sa
```
- 생성 후 확인:
  ```bash
  kubectl get serviceaccount
  kubectl describe serviceaccount dashboard-sa
  ```

### Pod에 Service Account 연결
Pod에 특정 Service Account를 지정하려면 `serviceAccountName` 필드를 추가합니다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: dashboard-pod
spec:
  serviceAccountName: dashboard-sa
  containers:
    - name: app
      image: nginx
```
- 이렇게 지정하면 Pod의 내부에 해당 SA의 Token이 자동으로 생성 및 마운트됨
- Token은 짧은 수명(short-lived) 을 가지며, Pod 삭제 시 자동으로 만료됨
- Kubelet이 Token을 주기적으로 자동 갱신(rotation) 함

### Token 자동 마운트 제어
기본적으로 Token은 자동으로 Pod에 마운트되지만, 필요 시 이를 비활성화할 수 있습니다.
```yaml
automountServiceAccountToken: false
```
- ServiceAccount 수준에 설정 시: 해당 SA를 사용하는 모든 Pod에 적용  
- Pod 수준에 설정 시: 특정 Pod에만 적용

### 외부 시스템에서의 Token 사용
외부 애플리케이션(CI/CD, 대시보드, 모니터링 도구 등)에서 사용할 Token을 생성할 수 있습니다.
```bash
kubectl create token dashboard-sa
```
- 기본 유효기간: 1시간
- 만료 시간을 늘리고 싶다면:
  ```bash
  kubectl create token dashboard-sa --duration=24h
  ```
- Token은 Secret으로 저장되지 않음
- Base64 디코딩 시 다음 정보 확인 가능:
  - `exp` (만료 시간)
  - `serviceaccount.name`
  - `namespace` 등

### Token을 이용한 API 호출 예시
```bash
curl -k https://<k8s-api-server>/api/v1/pods   --header "Authorization: Bearer <TOKEN>"
```
- 또는 대시보드 앱 내에서 Token을 붙여 인증할 수 있음

### 요약 정리
| 항목 | 설명 |
|-----|-----|
| Service Account | Pod나 앱이 Kubernetes API와 상호작용하기 위한 계정 |
| Token | SA의 인증 수단 (Bearer Token 형태) |
| 기본 SA | 각 Namespace에 `default` SA 자동 생성 |
| Pod 연결 | `serviceAccountName` 필드로 지정 |
| Token 수명 | Pod 삭제 시 만료, 자동 갱신됨 |
| 자동 마운트 제어 | `automountServiceAccountToken: false` |
| 외부용 Token 생성 | `kubectl create token <sa-name>` |

### 결론
- Service Account는 Kubernetes 내외부 애플리케이션의 인증 수단으로 사용됨  
- Pod 내부에서는 자동으로 Token이 관리되며,  
- 외부 시스템에서는 `kubectl create token` 명령어로 발급받은 Token을 사용해 인증 가능  
- 필요한 경우 Token 자동 마운트를 비활성화하거나, Token 유효 기간을 조정할 수 있음.