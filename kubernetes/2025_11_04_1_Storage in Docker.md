# 쿠버네티스

## Storage in Docker

### Docker가 데이터를 저장하는 위치

Docker를 시스템에 설치하면 다음과 같은 디렉터리 구조가 생성된다:

```
/var/lib/docker
    ├─ aufs
    ├─ containers
    ├─ image
    ├─ volumes
    └─ 기타 폴더들
```

이 위치가 Docker가 데이터를 저장하는 기본 경로이다.

- `containers` → 실행 중인 컨테이너 관련 파일 저장
- `image` → 이미지 관련 파일 저장
- `volumes` → 컨테이너가 사용하는 볼륨 저장

(볼륨은 뒤에서 다시 설명)

---

### Docker 이미지가 파일을 저장하는 방식 – 레이어 구조

Docker는 레이어(layer) 아키텍처로 이미지를 구성한다.  
Dockerfile의 각 명령은 새로운 레이어를 생성하며, 각 레이어는 이전 레이어와 달라진 변경분만 저장한다.

예시:

1. `FROM ubuntu` → 우분투 기반 이미지 (약 120MB)
2. `RUN apt install ...` → apt 패키지 설치 (약 300MB)
3. `RUN pip install ...` → Python 종속성 추가
4. `COPY source code`
5. `ENTRYPOINT` 설정

각 레이어는 이전 레이어의 변경분만 포함하므로 용량이 작게 유지된다.

---

### 레이어 재사용과 캐싱

다른 애플리케이션을 빌드할 때, 동일한 기반 이미지나 동일한 레이어가 있으면 Docker는 새로 만들지 않고 이미 존재하는 레이어를 재사용한다.

✅ 빌드 속도 향상  
✅ 코드 일부만 수정한 경우, 변경된 레이어만 재생성

---

### 컨테이너 실행 시의 구조

이미지는 다음처럼 읽기 전용(Read-Only) 레이어들로 구성된다:

```
Base Layer (Ubuntu)
Packages
Dependencies
Source Code
Entry Point
```

컨테이너를 실행하면 Docker는 그 위에 쓰기 가능(Read-Write) 레이어를 추가한다:

```
RW Layer (Container Layer)
↑
Image Layers (Read-Only)
```

RW 레이어에는 다음 내용이 저장된다:

- 애플리케이션 로그 파일
- 임시 파일
- 사용자가 컨테이너 내부에서 생성한 파일

컨테이너가 제거되면 RW 레이어도 함께 삭제된다.

---

### Copy-On-Write

이미지 레이어는 수정할 수 없다.  
그러나 컨테이너 내부에서 이미지 레이어의 파일을 수정하려 하면, Docker는 해당 파일을 RW 레이어로 복사하고 그 복사본을 수정하도록 한다.

→ 이것을 Copy-on-Write(COW)라고 한다  
→ 이미지 레이어는 변하지 않고 그대로 유지된다

---

### 컨테이너 데이터가 삭제되지 않게 저장하려면? (Persistent Data)

컨테이너가 삭제되면 RW 레이어도 삭제되므로 데이터가 사라진다.  
따라서 DB 같은 데이터를 보존하려면 볼륨(volume)을 사용해야 한다.

#### (1) Docker 볼륨 생성

```sh
docker volume create data_volume
```

→ `/var/lib/docker/volumes/data_volume/` 디렉터리 생성

#### (2) 컨테이너 실행 시 볼륨 마운트

```sh
docker run -v data_volume:/var/lib/mysql mysql
```

이제 MySQL 데이터는 컨테이너가 삭제돼도 남아 있다.

#### (3) 볼륨 자동 생성

볼륨을 미리 만들지 않아도:

```sh
docker run -v data_volume2:/var/lib/mysql mysql
```

→ Docker가 자동으로 `data_volume2` 생성

---

### Bind Mount (호스트 특정 디렉터리 마운트)

이미 `/data/mysql` 같은 경로가 있다면:

```sh
docker run -v /data/mysql:/var/lib/mysql mysql
```

→ 이것을 bind mount라고 한다  
✅ 원하는 물리 경로 사용 가능  
✅ 외부 스토리지 사용 가능

---

### -v 대신 --mount 사용 (신규 방식)

```sh
docker run   --mount type=bind,source=/data/mysql,target=/var/lib/mysql   mysql
```

`--mount`가 더 명시적이고 정확하여 최신 방식으로 추천된다.

---

### Storage Driver의 역할

레이어 구조를 관리하고 다음을 처리하는 핵심 기술:

✅ 레이어 저장  
✅ RW 레이어 생성  
✅ Copy-on-Write  
✅ 파일 관리

대표적인 스토리지 드라이버:

- AUFS
- Btrfs
- ZFS
- Device Mapper
- Overlay
- Overlay2

운영체제에 따라 기본 드라이버가 다르다.

| OS | 기본 드라이버 |
|----|---------------|
| Ubuntu | AUFS 또는 Overlay2 |
| Fedora / CentOS | AUFS 미지원 → Device Mapper 또는 Overlay2 |

---

### 결론

- Docker 이미지 = 여러 개의 읽기 전용 레이어
- 컨테이너 = 이미지 위에 쓰기 가능한 레이어가 추가된 형태
- 컨테이너 삭제 시 RW 레이어도 함께 삭제 → 데이터 사라짐
- 데이터를 유지하려면 볼륨 또는 바인드 마운트 사용
- 모든 레이어 관리 및 COW 작업은 Storage Driver가 처리