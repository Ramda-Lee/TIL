# 쿠버네티스

## Volume Driver Plugins in Docker

### 스토리지 드라이버 vs 볼륨 드라이버

- 스토리지 드라이버(Storage Drivers): 이미지와 컨테이너의 저장(레이어드 파일시스템)을 관리한다.
- 볼륨(Volumes): 데이터를 영구(persistent) 보관하고자 할 때 사용한다.
- 중요: 볼륨은 스토리지 드라이버가 아니라, 볼륨 드라이버 플러그인(Volume Driver Plugins) 이 처리한다.

---

### 기본 볼륨 드라이버: `local`

- 기본(volume) 드라이버 플러그인은 `local` 이다.
- `local` 드라이버는 Docker 호스트에 볼륨을 생성하고 데이터를 아래 경로에 저장한다:
  - ```
    /var/lib/docker/volumes
    ```

---

### 서드파티 볼륨 드라이버의 예

다양한 서드파티(3rd party) 볼륨 드라이버 플러그인을 통해, 외부 스토리지를 볼륨으로 사용할 수 있다. 예:

- Azure File Storage
- convoy
- DigitalOcean
- Block Storage Locker
- Google Compute Persistent Disks
- GlusterFS
- NetApp X-ray
- Portworx
- VMware vSphere Storage

> 위 목록은 일부 예시이며, 지원되는 드라이버는 매우 많다.

---

### 멀티 클라우드/다양한 공급자 지원 드라이버 예

일부 볼륨 드라이버는 여러 스토리지 제공자를 지원한다.  
예를 들어, X-ray storage driver는 다음과 같은 스토리지에 프로비저닝할 수 있다:

- AWS: EBS, S3
- EMC 스토리지: Isilon, ScaleIO
- Google: Persistent Disk
- OpenStack: Cinder

---

## 5) 컨테이너 실행 시 특정 볼륨 드라이버 사용

컨테이너를 실행할 때, 예를 들어 X-ray EBS 드라이버를 지정하여 Amazon EBS로부터 볼륨을 프로비저닝할 수 있다.  
이렇게 하면 컨테이너가 생성될 때 클라우드의 볼륨이 연결된다. 컨테이너가 종료되더라도 데이터는 클라우드에 보존된다.

> (개념 설명) 실제 사용 시에는 `--mount` 또는 `-v`와 드라이버/옵션을 함께 지정하여 드라이버별 가이드에 맞춰 설정한다.