# 쿠버네티스

## Container Storage Interface

### 배경: 컨테이너 런타임 표준화의 필요성 (CRI)

과거 Kubernetes는 컨테이너 런타임 엔진으로 containerd 만을 사용했고, containerd와 상호작용하기 위한 코드가 Kubernetes 소스 코드 내부에 내장되어 있었다. 이후 rkt(rocket), CRI-O 같은 다른 컨테이너 런타임이 등장하면서, 특정 런타임에 종속되지 않고 여러 런타임을 쉽게 연동할 수 있는 방식이 필요해졌다.

이러한 요구로 Container Runtime Interface(CRI) 가 탄생했다. CRI는 Kubernetes 같은 오케스트레이션 솔루션이 Docker, containerd 등 컨테이너 런타임과 통신하는 표준을 정의한다. 새로운 컨테이너 런타임이 등장하더라도 CRI 표준만 따르면 Kubernetes의 소스 코드를 건드리거나 Kubernetes 팀과 별도로 협업하지 않아도 연동이 가능하다.

---

### 네트워킹 표준: CNI

네트워킹에서도 같은 이유로 Container Networking Interface(CNI) 가 도입되었다. 새로운 네트워킹 벤더들은 CNI 표준에 맞춘 플러그인을 개발함으로써, Kubernetes 등과 손쉽게 통합할 수 있게 되었다.

---

### 스토리지 표준: CSI

스토리지 분야에서도 다양한 솔루션을 지원하기 위해 Container Storage Interface(CSI) 가 개발되었다. CSI를 통해 각 스토리지 벤더는 자사 스토리지에 맞는 드라이버를 구현하여 Kubernetes와 연동할 수 있다.

예시(일부):

- Portworx
- Amazon EBS
- Azure Disk
- Dell EMC: Isilon, PowerMax, Unity, XtremIO
- NetApp
- Nutanix
- HPE
- Hitachi
- Pure Storage

> 중요: CSI는 Kubernetes 전용 표준이 아니다. 보편적(universal) 표준으로, 이를 구현하면 어떤 컨테이너 오케스트레이션 도구도 지원 플러그인만 있으면 어떤 스토리지 벤더와도 연동할 수 있다. 현재 Kubernetes, Cloud Foundry, Mesos가 CSI를 채택하고 있다.

---

### CSI의 형태: RPC 호출 집합

CSI는 컨테이너 오케스트레이터가 호출해야 하는 RPC(원격 프로시저 호출) 세트를 정의하며, 스토리지 드라이버가 이를 구현해야 한다.

예를 들어, Pod가 생성되면서 볼륨이 필요한 경우 오케스트레이터(여기서는 Kubernetes)는 `CreateVolume` RPC를 호출하고 볼륨 이름 등 세부 정보를 전달한다. 스토리지 드라이버는 이 RPC를 구현하여 스토리지 어레이에 새 볼륨을 프로비저닝하고 결과를 반환해야 한다.

마찬가지로 볼륨 삭제 시에는 `DeleteVolume` RPC를 호출하고, 스토리지 드라이버는 해당 호출을 받아 어레이에서 볼륨을 해제/삭제하는 로직을 수행한다.

사양(spec)은 요청 시 어떤 파라미터를 보내야 하는지, 응답으로 무엇을 받아야 하는지, 어떤 에러 코드를 교환해야 하는지를 정확하게 규정한다. 자세한 내용은 GitHub의 CSI 사양에서 확인할 수 있다(원문에서 URL 언급).

---

### 정리

- Kubernetes는 다양한 구현체와의 결합도를 낮추고 생태계를 확장하기 위해 표준 인터페이스(CRI, CNI, CSI)를 도입했다.
- CSI는 스토리지 연동을 위한 표준으로, 오케스트레이터 ↔ 스토리지 드라이버 사이의 RPC 계약을 정의한다.
- 이를 통해 여러 클라우드/온프레미스 스토리지 벤더가 자체 드라이버(CSI 플러그인) 를 구현하여 컨테이너 오케스트레이터와 상호운용할 수 있다.