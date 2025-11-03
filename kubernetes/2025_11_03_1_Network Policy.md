# 쿠버네티스

## Network Policy

### 기본 네트워킹 예시

간단한 구성은 다음과 같습니다.
- 웹 서버(Web Server) — 사용자 요청을 처리 (`포트 80`)
- 앱 서버(App Server) — 백엔드 API 처리 (`포트 5000`)
- 데이터베이스 서버(Database Server) — 데이터 저장 및 접근 (`포트 3306`)

#### 트래픽 흐름
1. 사용자 → 웹 서버 (`포트 80`)
2. 웹 서버 → API 서버 (`포트 5000`)
3. API 서버 → 데이터베이스 서버 (`포트 3306`)
4. 데이터베이스 서버 → API 서버 → 웹 서버 → 사용자

---

### 인그레스(Ingress)와 이그레스(Egress) 트래픽

- 인그레스(Ingress): 해당 컴포넌트로 들어오는 트래픽  
- 이그레스(Egress): 해당 컴포넌트에서 나가는 트래픽

예시
- 웹 서버
  - 인그레스: 사용자 → 웹 서버 (포트 80)
  - 이그레스: 웹 서버 → API 서버 (포트 5000)

- API 서버
  - 인그레스: 웹 서버 → API 서버 (포트 5000)
  - 이그레스: API 서버 → DB 서버 (포트 3306)

- DB 서버
  - 인그레스: API 서버 → DB 서버 (포트 3306)

#### 필요한 규칙 요약

| 구성 요소 | 유형 | 규칙 |
|------------|------|------|
| 웹 서버 | 인그레스 | 포트 80에서 HTTP 허용 |
| 웹 서버 | 이그레스 | API 서버 포트 5000으로 허용 |
| API 서버 | 인그레스 | 포트 5000에서 허용 (웹 서버에서) |
| API 서버 | 이그레스 | 포트 3306으로 허용 (DB 서버로) |
| DB 서버  | 인그레스 | 포트 3306에서 허용 (API 서버에서) |

---

#### 쿠버네티스의 네트워킹 구조

쿠버네티스 클러스터는 다음 요소들로 구성됩니다:
- 노드(Node), 파드(Pod), 서비스(Service) — 각각 고유한 IP를 가짐
- 파드들은 기본적으로 서로 자유롭게 통신할 수 있음

#### 기본 동작
- 쿠버네티스는 모든 파드 간 통신을 허용하는 기본 규칙을 가지고 있다.
- 즉, 인그레스와 이그레스 트래픽이 모두 허용된 상태

---

### 트래픽 제한: 네트워크 정책(Network Policy)

특정 통신을 제한하려면 네트워크 정책을 사용한다

예시:  
> 프론트엔드(웹) 파드가 DB 파드에 직접 접근하지 못하게 하고 싶을 때,  
> 오직 API 파드만 DB 파드에 접근하도록 제한한다.

이때 NetworkPolicy를 사용한다.

---

### 네트워크 정책 개념

- NetworkPolicy는 쿠버네티스 객체(Object) 중 하나로, Pod, ReplicaSet, Service처럼 동작한다.
- 레이블(Labels)과 셀렉터(Selectors)를 이용해 특정 파드에 정책을 적용한다.
- 정책에는 인그레스(Ingress), 이그레스(Egress) 규칙을 정의할 수 있다.

### 예시 규칙
- DB 파드에 API 파드로부터 포트 3306의 인그레스만 허용

---

### YAML 예시

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: db-policy
spec:
  podSelector:
    matchLabels:
      role: db
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          role: api
    ports:
    - protocol: TCP
      port: 3306
```

### 설명
- `podSelector`: 정책이 적용될 DB 파드를 지정  
- `policyTypes`: Ingress 또는 Egress 중 어떤 규칙을 설정할지 지정  
- `ingress`: `role=api` 라벨을 가진 파드에서 오는 `3306` 포트의 트래픽만 허용

---

### 정책 적용 방법

명령어 실행:
```bash
kubectl apply -f db-policy.yaml
```

### 주의사항
- 네트워크 정책은 CNI 플러그인(Network Solution) 에 의해 적용된다.
- 모든 CNI가 NetworkPolicy를 지원하는 것은 아니다.

지원되는 CNI:
- Calico
- Cilium
- Weave Net
- Kube-Router
- Romana

Flannel은 NetworkPolicy를 지원하지 않는다.  
지원하지 않는 CNI에서도 객체 생성은 가능하지만, 정책은 적용되지 않는다.

---

### 요약

| 개념 | 설명 |
|------|------|
| Ingress | 파드로 들어오는 트래픽 |
| Egress | 파드에서 나가는 트래픽 |
| Network Policy | 파드 간 통신을 제어하는 정책 |
| Labels/Selectors | 정책 적용 대상 파드 지정 |
| 기본 동작 | 모든 파드 간 통신 허용 |
| 지원 CNI | Calico, Cilium, Kube-Router, Weave Net, Romana |