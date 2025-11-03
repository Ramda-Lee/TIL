# 쿠버네티스

## Developing network policies

### 요구사항 정의

- 목표:  
  데이터베이스(DB) 파드는 API 파드만 접근 가능해야 하며, 포트는 `3306`으로 제한한다.  
  그 외 모든 파드(예: 웹 파드)는 접근할 수 없어야 한다.

- 가정:  
  - 웹 파드(Web Pod)와 API 파드는 트래픽 제한이 없음 (모든 방향 허용)  
  - 오직 DB 파드만 보호 대상임

---

### 초기 설정 및 차단 단계

기본적으로 쿠버네티스는 모든 파드 간 트래픽을 허용한다.  
따라서 첫 단계는 DB 파드로의 모든 트래픽을 차단하는 것

이를 위해 다음과 같이 네트워크 정책을 생성

#### Pod Selector
- `podSelector` 필드에 DB 파드의 라벨(`role=db`)을 지정
- 이 설정으로 DB 파드가 정책의 적용 대상이 된다.

이 시점에서 모든 트래픽이 차단

---

### API 파드에서 DB 파드로의 접근 허용

이제 API 파드에서 DB 파드로 `3306/TCP` 포트 접근만 허용

#### 정책 유형 결정
- DB 파드의 관점에서 접근은 들어오는 트래픽이므로 → Ingress Rule
- DB 파드가 API 파드에 응답하는 트래픽은 자동으로 허용됨 (별도 규칙 불필요)

따라서, 이 경우에는 Ingress Policy만 필요

---

### Ingress 규칙 정의

Ingress 규칙에는 두 가지 주요 필드가 있다.

| 필드 | 설명 |
|------|------|
| from | 허용할 트래픽의 출처 지정 (Pod/Namespace/IP 기반) |
| ports | 트래픽이 접근 가능한 포트 지정 |

#### YAML 예시
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

이 정책은 DB 파드에 대해 API 파드에서 오는 3306 포트 트래픽만 허용한다.

---

### 네임스페이스 기반 접근 제한 (Namespace Selector)

여러 네임스페이스(dev, test, prod)가 존재하고,  
각 네임스페이스에 동일한 라벨을 가진 API 파드가 있을 경우,  
현재 정책은 모든 네임스페이스의 API 파드를 허용한다.

이를 방지하려면 namespaceSelector를 함께 사용한다.

#### YAML 예시
```yaml
ingress:
- from:
  - podSelector:
      matchLabels:
        role: api
    namespaceSelector:
      matchLabels:
        env: prod
  ports:
  - protocol: TCP
    port: 3306
```

> `namespaceSelector`를 사용하려면 해당 네임스페이스에 반드시 라벨(`env: prod`)이 미리 설정되어 있어야 한다.

---

### IP 기반 접근 허용 (IP Block)

만약 쿠버네티스 외부의 백업 서버가 DB 파드에 접근해야 한다면,  
Pod이나 Namespace로 지정할 수 없다.
이때는 IP Block을 사용한다.

#### YAML 예시
```yaml
ingress:
- from:
  - ipBlock:
      cidr: 192.168.5.1/32
  ports:
  - protocol: TCP
    port: 3306
```

이 정책은 IP 주소 `192.168.5.1`에서 오는 트래픽만 허용한다.

---

### from 필드에서의 세 가지 Selector

| Selector | 설명 |
|-----------|------|
| podSelector | 특정 라벨을 가진 파드만 허용 |
| namespaceSelector | 특정 라벨을 가진 네임스페이스의 파드만 허용 |
| ipBlock | 지정된 IP 또는 CIDR 범위 허용 |

이 세 가지는 개별 규칙으로도, 하나의 규칙 내에서 조합해도 사용할 수 있다.

#### 조합 시 주의
- 같은 규칙 내(`- from:` 아래에 함께 정의됨): AND 관계
- 별도의 규칙(`-`으로 구분됨): OR 관계

즉,
- 같은 블록에 `podSelector`와 `namespaceSelector` → 둘 다 일치해야 통과  
- 서로 다른 블록에 정의 → 둘 중 하나라도 일치하면 통과

이 미세한 차이가 보안 정책에 큰 영향을 미칠 수 있음을 주의해야 한다.

---

### Egress 규칙

이제 반대로, DB 파드에서 외부로 나가는 트래픽(예: 백업 서버로 데이터 전송)을 허용해야 하는 경우를 봐보자

- 이 경우에는 Egress Policy가 필요하다.

#### YAML 예시
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: db-egress-policy
spec:
  podSelector:
    matchLabels:
      role: db
  policyTypes:
  - Egress
  egress:
  - to:
    - ipBlock:
        cidr: 192.168.5.1/32
    ports:
    - protocol: TCP
      port: 80
```

이 설정은 DB 파드에서 외부 백업 서버(192.168.5.1:80) 로의 트래픽만 허용

---

### 요약

| 구분 | 설명 |
|------|------|
| Ingress | 외부 → 파드로 들어오는 트래픽 허용 |
| Egress | 파드 → 외부로 나가는 트래픽 허용 |
| PodSelector | 특정 라벨을 가진 파드 지정 |
| NamespaceSelector | 특정 네임스페이스의 파드 지정 |
| IPBlock | 외부 IP/CIDR 범위 지정 |
| AND 조건 | 하나의 규칙 내부에서 여러 Selector 동시 사용 |
| OR 조건 | 여러 규칙을 `-`로 분리할 경우 |

---

### 결론

- Network Policy는 보안 제어의 핵심 도구이며, 세밀한 규칙 설계가 중요합니다.
- 잘못된 들여쓰기(`-`)나 Selector 조합은 의도치 않은 트래픽 허용을 초래할 수 있습니다.