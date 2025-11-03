# 쿠버네티스

## Custom Resource Definition, CRD

### 리소스(Resource)란?

쿠버네티스에서 리소스란 클러스터 내에 존재하는 객체(Object) 로, 예를 들어 `Deployment`, `Pod`, `Service` 등이 있다.
리소스는 생성되면 etcd 데이터베이스에 저장된다.

예를 들어 다음과 같은 Deployment를 생성하면:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-deployment
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: nginx
        image: nginx
```

이를 생성(`kubectl create`)하면 쿠버네티스는 etcd에 Deployment 객체 정보를 저장한다. 
이후 `kubectl get deployments`, `kubectl delete deployment my-deployment` 명령으로 이 객체를 조회하거나 삭제할 수 있다.

---

### 컨트롤러(Controller)의 역할

Deployment를 생성하면 지정된 replica 수(예: 3) 만큼 Pod가 자동 생성된다.
이 Pod 생성은 누가 담당? → Deployment Controller

컨트롤러는 쿠버네티스 내부에서 백그라운드로 실행되는 프로세스이며,  
특정 리소스(예: Deployment)의 상태를 지속적으로 모니터링하고, 원하는 상태(desired state)에 맞게 클러스터를 조정한다.

예:
- Deployment 객체 생성 → ReplicaSet 생성
- ReplicaSet → 지정된 수의 Pod 생성

즉, 컨트롤러는 “리소스 상태를 유지하는 관리자”

---

### 리소스와 컨트롤러의 관계

| 리소스 | 컨트롤러 |
|--------|-----------|
| Deployment | Deployment Controller |
| ReplicaSet | ReplicaSet Controller |
| Job | Job Controller |
| CronJob | CronJob Controller |
| StatefulSet | StatefulSet Controller |

이 모든 컨트롤러들은 Kubernetes에 내장되어 있다.

---

### 새로운 리소스 만들기 (예시: Flight Ticket)

예시로, “항공권 예약”이라는 새로운 리소스를 만든다 가정해보자

```yaml
apiVersion: flight.com/v1
kind: FlightTicket
metadata:
  name: my-flight-ticket
spec:
  from: Mumbai
  to: London
  number: 2
```

이 객체를 생성하면 “항공권 예약 리소스”가 만들어진다.  
이후 `kubectl get flighttickets` 로 목록을 조회하거나, `kubectl delete` 로 삭제할 수 있다.

하지만 문제는 — 이 리소스는 단지 etcd에 저장될 뿐, 실제 항공권 예약을 수행하지 않는다.

---

### 실제 동작을 위한 컨트롤러 추가

실제 항공권 예약을 수행하려면, 다음처럼 API를 호출해야 한다:

> 예: `https://bookflight.com/api`

이 동작을 자동화하려면 커스텀 컨트롤러(Custom Controller) 가 필요

#### 컨트롤러의 역할
- FlightTicket 리소스 생성 시 → 외부 API 호출하여 항공권 예약
- FlightTicket 삭제 시 → 외부 API 호출하여 예약 취소

이때 생성된 객체(`FlightTicket`)는 커스텀 리소스(Custom Resource),  
이를 감시하고 조작하는 컨트롤러는 커스텀 컨트롤러(Custom Controller)다.

---

### 커스텀 리소스 정의 (CRD, Custom Resource Definition)

쿠버네티스는 기본적으로 임의의 리소스(예: FlightTicket)를 생성할 수 없다. 
먼저 쿠버네티스 API 서버에 “이런 리소스를 허용해라”라고 등록해야 한다. 
그 역할을 하는 것이 CustomResourceDefinition (CRD)다.

#### CRD의 구조
```yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: flighttickets.flights.com
spec:
  scope: Namespaced
  group: flights.com
  names:
    kind: FlightTicket
    plural: flighttickets
    singular: flightticket
    shortNames:
    - ft
  versions:
  - name: v1
    served: true
    storage: true
    schema:
      openAPIV3Schema:
        type: object
        properties:
          spec:
            type: object
            properties:
              from:
                type: string
              to:
                type: string
              number:
                type: integer
                minimum: 1
                maximum: 10
```

---

### 주요 필드 설명

| 필드 | 설명 |
|------|------|
| apiVersion | CRD의 API 버전 (`apiextensions.k8s.io/v1`) |
| kind | 객체 타입 — 항상 `CustomResourceDefinition` |
| metadata.name | `리소스이름.그룹명` 형식 (예: `flighttickets.flights.com`) |
| spec.scope | Namespaced / Cluster — 네임스페이스 범위 여부 |
| spec.group | API 그룹 이름 (`flights.com`) |
| spec.names | Kind, plural, singular, shortName 정의 |
| spec.versions | 여러 버전(`v1alpha1`, `v1beta1`, `v1`)을 관리 가능 |
| schema | 리소스의 필드 구조 및 데이터 타입 정의 |

---

### CRD 생성 및 사용

CRD 생성:
```bash
kubectl apply -f flightticket-crd.yaml
```

생성 후에는 이제 다음 명령들이 작동한다:
```bash
kubectl create -f my-flight-ticket.yaml
kubectl get flighttickets
kubectl delete flightticket my-flight-ticket
```

이제 쿠버네티스는 “FlightTicket”이라는 새로운 리소스 타입을 이해하게 된다.

---

### 한계와 다음 단계

CRD를 생성하면 쿠버네티스가 새로운 리소스를 인식하게 되지만,  
아직 컨트롤러가 없기 때문에 아무런 동작을 하지 않는다.

즉, FlightTicket 객체는 단순히 etcd에 저장된 데이터일 뿐 실제 예약은 수행되지 않는다.

이를 해결하려면 커스텀 컨트롤러(Custom Controller) 를 개발해야 한다. 
컨트롤러는 다음을 수행한다:

- 리소스 생성/변경/삭제 감지
- 외부 API 호출 (예: 예약, 취소 등)
- 상태(Status) 업데이트

---

### 요약

| 구분 | 역할 |
|------|------|
| Custom Resource (CR) | 사용자가 정의한 새 리소스 타입 |
| Custom Resource Definition (CRD) | 쿠버네티스에 새로운 리소스 타입을 등록 |
| Custom Controller | 리소스의 실제 동작(로직)을 수행 |
| etcd | 모든 리소스 객체가 저장되는 데이터베이스 |
| Scope | Namespaced 또는 Cluster 수준 정의 |