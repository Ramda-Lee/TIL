# 쿠버네티스

## Custom Controllers

### 개요

이전 강의에서 우리는 커스텀 리소스 정의(CRD, Custom Resource Definition) 를 생성
이제 `FlightTicket` 객체를 만들 수 있고, 그 데이터는 etcd에 저장

다음 단계는 etcd에 저장된 객체의 상태를 모니터링하고,  
필요할 때 항공권 예약 API를 호출하여 예약, 수정, 취소 등의 작업을 자동화하는 것
이 역할을 수행하는 것이 바로 커스텀 컨트롤러(Custom Controller)

---

### 컨트롤러란?

컨트롤러는 쿠버네티스 클러스터의 상태를 지속적으로 감시하는 루프 형태의 프로세스이다.
특정 리소스(여기서는 `FlightTicket`)가 생성, 수정, 삭제되는 이벤트를 감지하고 필요한 동작을 수행한다.

즉, 컨트롤러는 다음의 원리를 따릅니다:

> “Kubernetes 리소스의 상태를 감시하고, 원하는 상태(Desired State)에 맞게 자동 조정한다.”

---

### Python vs Go 언어

쿠버네티스 API 서버를 직접 호출하여 컨트롤러를 만드는 것은 어떤 언어로도 가능하다.

예를 들어 Python으로 다음을 할 수 있다:
1. Kubernetes API에 접근하여 `FlightTicket` 객체를 조회
2. 변경 사항을 감지(watch)
3. 외부 API 호출을 통해 시스템 변경

하지만 Python 방식에는 다음과 같은 한계가 있다:

| 한계 | 설명 |
|------|------|
| API 호출 비용 증가 | 주기적 Polling으로 인한 오버헤드 |
| 캐싱 및 큐잉 로직 직접 구현 필요 | 개발 난이도 증가 |

반면 Go 언어 + Kubernetes Client-Go를 사용하면 다음의 이점이 있다:

- Shared Informer 제공 → 자동 캐싱 및 큐 관리  
- Event 기반 감시 구조 지원  
- Kubernetes 공식 컨트롤러 구현 언어로 표준화됨

따라서 실무에서는 대부분 Go 언어 기반 컨트롤러를 사용한다.

---

### 커스텀 컨트롤러 개발 절차

#### (1) 준비 단계
- Go 언어가 설치되어 있어야 한다.

```bash
go version
```

없다면 Go를 설치

#### (2) 샘플 컨트롤러 클론
쿠버네티스 공식 샘플 컨트롤러 저장소를 클론

```bash
git clone https://github.com/kubernetes/sample-controller.git
cd sample-controller
```

#### (3) 컨트롤러 코드 수정
`controller.go` 파일을 열고, 커스텀 로직을 추가

예를 들어, `FlightTicket` 리소스를 감시하여 항공권 예약 API를 호출할 수 있다.

```go
// 예시 (의사 코드)
if flightTicket.Created {
    callExternalAPI("book-flight")
}
```

#### (4) 빌드 및 실행

```bash
go build -o flight-controller
./flight-controller --kubeconfig=$HOME/.kube/config
```

컨트롤러가 실행되면 쿠버네티스 API 서버에 연결하여 `FlightTicket` 객체의 변경 사항을 감시한다.

- 객체가 생성되면 → 항공권 예약 API 호출  
- 객체가 삭제되면 → 예약 취소 API 호출

---

### 컨트롤러 배포 방식

컨트롤러를 매번 로컬에서 실행하는 대신,  
Docker 이미지로 패키징하여 Pod 또는 Deployment 형태로 쿠버네티스 클러스터 내부에서 실행할 수 있다.

이 방식의 장점:
- 자동 재시작 및 장애 복구 지원
- 다중 컨트롤러 인스턴스 확장 가능
- 운영 환경에 통합 배포 용이

---

### 시험 및 실무 관점

| 항목 | 시험 출제 가능성 | 비고 |
|------|------------------|------|
| CRD 생성 및 수정 | 높음 | 커스텀 리소스 정의는 자주 출제됨 |
| CR 리소스 다루기 (생성/삭제) | 있음 | YAML 작성 중심 |
| 커스텀 컨트롤러 개발 | 낮음 | 코딩 지식 요구 |
| Operator 개념 이해 | 중요 | 고급 주제에서 다룸 |

즉, CRD 작성과 관리가 시험에서 핵심이며,  
컨트롤러는 개념적 이해 정도면 충분

---

### 요약

| 구분 | 설명 |
|------|------|
| Custom Resource (CR) | 사용자가 정의한 새로운 리소스 타입 |
| CRD (Custom Resource Definition) | 쿠버네티스에 새 리소스 타입을 등록 |
| Custom Controller | 리소스 변경 감지 및 동작 수행 로직 |
| 개발 언어 | Go (Client-Go + Informer 기반) |
| 배포 형태 | Docker 이미지 → Pod/Deployment |
| 주요 기능 | 생성/수정/삭제 이벤트 감시 및 외부 API 호출 |