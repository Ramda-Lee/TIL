# 쿠버네티스

## Custom Controllers

### 개요

이전 강의에서 우리는 커스텀 리소스 정의(CRD) 와 커스텀 컨트롤러(Custom Controller) 를 따로 만들었다. 
지금까지는 이 둘을 별도로 관리해야 했다.

- CRD를 수동으로 생성하고  
- 해당 리소스를 생성한 뒤  
- 컨트롤러를 Pod 또는 Deployment로 직접 배포해야 했다.

하지만 Operator Framework를 사용하면  
이 두 요소를 하나의 단일 배포 단위로 묶어 자동으로 관리할 수 있다.

---

### Operator란 무엇인가?

오퍼레이터(Operator) 는 CRD와 그에 대응하는 컨트롤러를 하나의 패키지로 묶은 구조이다.
즉, 오퍼레이터를 배포하면 내부적으로 다음 작업이 자동으로 이루어진다:

1. Custom Resource Definition 생성
2. Custom Resource 및 관련 리소스 생성
3. Custom Controller를 Deployment로 배포

이 과정을 통해, 단 한 번의 오퍼레이터 배포로 전체 애플리케이션 로직이 구성된다.

---

### 실제 사례: etcd Operator

가장 널리 알려진 오퍼레이터 중 하나는 etcd Operator다. 
이 오퍼레이터는 쿠버네티스 내에서 etcd 클러스터를 배포 및 관리하는 역할을 한다.

#### etcd Operator 구성 요소

| 구성 요소 | 설명 |
|------------|------|
| CRD (etcdCluster) | etcd 클러스터 정의 |
| Custom Controller | CRD 감시 및 클러스터 생성 |
| Backup Operator | etcd 백업 수행 |
| Restore Operator | 백업 복원 수행 |

즉, 단순히 클러스터를 생성하는 것뿐 아니라,  
백업 및 복구(restore) 까지 자동화할 수 있다.

---

### 오퍼레이터의 역할

오퍼레이터는 실제로 사람 운영자(human operator) 가 수행하던 일들을 자동화한다.

예를 들어:
- 애플리케이션 설치 및 초기 설정
- 정기 백업 수행
- 장애 발생 시 자동 복구
- 버전 업그레이드 및 설정 변경

즉, 오퍼레이터는 애플리케이션 운영 자동화를 위한 컨트롤러 집합체라고 할 수 있다.

---

### Operator Hub

모든 주요 오퍼레이터는 [OperatorHub.io](https://operatorhub.io) 에서 제공됩니다.  
여기에는 다음과 같은 인기 오퍼레이터들이 포함되어 있다.

| 오퍼레이터 이름 | 주요 기능 |
|------------------|------------|
| etcd | etcd 클러스터 관리 및 백업/복구 |
| MySQL Operator | MySQL 클러스터 배포 및 관리 |
| Prometheus Operator | 모니터링 시스템 자동 구성 |
| Grafana Operator | 대시보드 관리 자동화 |
| Argo CD Operator | GitOps 기반 배포 자동화 |
| Istio Operator | 서비스 메쉬 관리 |

각 오퍼레이터 페이지에서는 설명, 설치 방법, CRD 예시 등을 볼 수 있다.

---

### 오퍼레이터 설치 과정

오퍼레이터 배포는 일반적으로 3단계로 구성된다.

1. Operator Lifecycle Manager (OLM) 설치  
   → 오퍼레이터의 생명주기(업데이트, 삭제 등)를 관리

2. 원하는 오퍼레이터 설치  
   → 예: etcd Operator, Prometheus Operator 등

3. CRD를 통해 리소스 생성  
   → 예: `EtcdCluster` 객체를 만들면 자동으로 클러스터 생성

이 과정을 통해 복잡한 애플리케이션도 간단히 배포할 수 있다.

---

### Operator의 내부 구조 요약

| 구성 요소 | 역할 |
|------------|------|
| CRD (Custom Resource Definition) | 사용자 정의 리소스 정의 |
| Controller (Custom Controller) | 리소스 상태 감시 및 조치 수행 |
| Operator Framework | 위 두 요소를 묶어 단일 배포 단위로 제공 |
| OLM (Operator Lifecycle Manager) | 오퍼레이터의 배포 및 업데이트 관리 |

---

### 학습 및 시험 관점

오퍼레이터는 매우 강력한 개념이지만,  
CKA나 KCNA 같은 공식 시험에서는 심층적으로 다루지 않는다.  
시험에서는 주로 CRD 관련 문제가 출제된다.

이 강의의 목적은 오퍼레이터의 기본 개념 이해에 있다.

---

### 요약

| 항목 | 설명 |
|------|------|
| Operator | CRD + Controller를 하나의 배포 단위로 묶은 프레임워크 |
| 예시 | etcd Operator, MySQL Operator, Prometheus Operator 등 |
| 기능 | 애플리케이션 설치, 백업, 복구, 업그레이드 등 자동화 |
| 배포 위치 | [operatorhub.io](https://operatorhub.io) |
| 시험 포인트 | CRD 개념 중심, Operator는 참고 수준 |

---

핵심 요약:  
> 오퍼레이터는 쿠버네티스에서 사람이 하던 애플리케이션 운영을 자동화하는 기술이며,  
> CRD와 컨트롤러를 하나의 배포 단위로 묶어 관리한다.