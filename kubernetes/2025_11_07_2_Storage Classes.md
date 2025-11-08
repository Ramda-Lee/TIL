# 쿠버네티스

## Storage Classes

### 수동(Static) 프로비저닝의 한계

이전 강의에서는 PV를 만들고, PVC로 해당 스토리지를 청구한 뒤, 파드 정의의 `volumes` 섹션에서 PVC를 참조하는 과정을 살펴봤다. 이 예시에서는 Google Cloud Persistent Disk로 PV를 생성했다. 문제는 PV를 정의하기 전에 GCP 콘솔에서 디스크를 직접 만들어 두어야 한다는 점이다. 애플리케이션이 스토리지를 필요로 할 때마다 다음과 같이 반복해야 한다.

1. GCP에서 디스크를 수동으로 프로비저닝
2. 동일한 디스크 이름으로 PersistentVolume 매니페스트 작성

이 방식이 바로 정적(Static) 프로비저닝이다. 늘 수동으로 손을 봐야 하므로 번거롭다.

---

### StorageClass로 동적(Dynamic) 프로비저닝

애플리케이션이 스토리지를 요청하는 즉시 자동으로 디스크를 만들고 PV까지 연결할 수 있다면 훨씬 좋을 것이다. 이러한 자동화를 제공하는 것이 StorageClass다.

- StorageClass를 정의하면서 GCE 같은 provisioner 를 지정하면, PVC가 생성될 때 필요한 디스크를 자동으로 만들고 PV까지 바인딩한다.
- 이를 동적(Dynamic) 프로비저닝이라고 한다.

StorageClass는 `apiVersion: storage.k8s.io/v1` 를 사용하고, `provisioner` 에 `kubernetes.io/gce` 와 같은 값을 설정한다.

---

### PVC와 StorageClass의 연동

기존 구조(파드 → PVC → PV)에 StorageClass를 추가하면 PV와 실제 스토리지를 일일이 만들 필요가 없다. 다만 PVC가 어떤 StorageClass를 사용할지 알려줘야 하므로 PVC 정의에 `storageClassName` 을 지정한다.

동작 순서는 다음과 같다.

1. 사용자가 PVC를 생성하고 `storageClassName` 을 지정한다.
2. 해당 StorageClass가 정의한 provisioner가 요청한 용량에 맞춰 GCP에 새 디스크를 만든다.
3. 새 디스크로 PersistentVolume을 자동 작성한다.
4. PVC가 새로 만든 PV에 자동으로 바인딩된다.

PVC 객체 자체는 여전히 존재하지만 사용자 관점에서는 “PVC/PV를 직접 만들지 않아도 된다”는 점이 가장 큰 차이이다.

---

### 다양한 Provisioner와 파라미터

GCE 말고도 AWS EBS, Azure File, Azure Disk, CIFS, Portworx, ScaleIO 등 다양한 provisioner가 있다. 그리고 각 provisioner마다 전달할 수 있는 추가 파라미터가 다르다.

예를 들어 Google Persistent Disk의 경우:
- `type`: `standard`, `ssd` 등을 선택
- `replication-type`: `none`, `regional-pd` 등을 지정

이처럼 StorageClass마다 서로 다른 디스크 특성을 지정해 둘 수 있다.

---

### StorageClass로 서비스 등급 정의

다양한 파라미터 조합으로 여러 StorageClass를 만들어 두면, PVC를 만들 때 원하는 “클래스”만 지정하면 된다.

- 예: `silver` 클래스 → 표준 디스크
- 예: `gold` 클래스 → SSD
- 예: `platinum` 클래스 → SSD + 복제

필요한 스토리지 등급을 PVC에서 선택하기만 하면, 나머지 프로비저닝은 StorageClass가 알아서 처리한다.
