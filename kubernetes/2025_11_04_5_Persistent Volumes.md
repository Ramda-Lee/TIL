# 쿠버네티스

## Persistent Volumes

### 왜 Persistent Volume인가?

이전에는 파드 정의 파일(Pod manifest) 안에서 볼륨을 직접 구성했다.  
즉, 볼륨을 위한 스토리지 설정 정보가 파드 정의 파일 내부에 모두 들어갔다.

그러나 사용자가 많은 대규모 환경에서는, 각 사용자가 많은 파드를 배포할 때마다 매번 스토리지를 개별 파드마다 설정해야 한다. 특정 스토리지 솔루션을 사용할 때에도, 그 설정을 모든 파드 정의에 반복적으로 반영해야 하며, 변경이 필요할 때마다 모든 파드에 일괄 수정을 해야 한다.

이 대신 스토리지를 중앙에서 관리하고 싶다. 관리자가 큰 스토리지 풀을 먼저 만들어 두고, 사용자는 필요할 때마다 그 풀에서 일부분을 할당(청구)받아 쓰는 방식이 이상적이다.  
이때 도움이 되는 개념이 Persistent Volume(PV) 이다.

---

### Persistent Volume(PV)란?

Persistent Volume은 클러스터 전체에서 공유되는 스토리지 볼륨의 풀(pool)로, 관리자가 미리 구성해 둔다.  
클러스터에서 애플리케이션을 배포하는 사용자는 Persistent Volume Claim(PVC) 을 통해 이 풀에서 스토리지를 선택/할당받는다.

---

### 예시: Persistent Volume 생성

기본 템플릿에서 시작해 API 버전을 지정하고, `kind`를 `PersistentVolume` 으로 설정한 뒤, 이름을 `pv-vol-1` (예: PV Vol one)로 지정한다.

`spec` 섹션에서 다음을 정의한다.

#### (1) accessModes
볼륨을 호스트에 어떤 모드로 마운트할지를 지정한다(읽기 전용/읽기-쓰기 등).  
지원되는 값(표준 Kubernetes 표기)은 다음과 같다.

- `ReadOnlyMany`
- `ReadWriteOnce`
- `ReadWriteMany`

#### (2) capacity
해당 PV에 예약할 스토리지 용량을 지정한다. (예: `1Gi`)

#### (3) volume type
처음에는 노드의 로컬 디렉터리를 사용하는 `hostPath` 옵션을 예시로 들 수 있다.  
> 단, 프로덕션 환경에서는 권장되지 않는다.

PV를 생성하려면 다음과 같이 실행한다.

```bash
kubectl create -f pv.yaml
```

생성된 PV를 조회하려면:

```bash
kubectl get persistentvolume
```

이후 `hostPath` 대신, 이전 강의에서 살펴본 지원되는 스토리지 솔루션(예: AWS EBS, 등)의 필드를 사용하도록 교체할 수 있다.