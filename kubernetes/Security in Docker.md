# 쿠버네티스

## Security in Docker

### Docker와 호스트의 관계

Docker가 설치된 호스트(Host) 는 여러 프로세스를 가지고 있다.  
예
- 운영체제(OS) 프로세스들  
- Docker 데몬(`dockerd`)  
- SSH 서버 등이 있습니다.

이제 이 호스트 위에서 `ubuntu` 이미지를 기반으로 한 Docker 컨테이너를 실행시킨다.  
컨테이너는 단순히 `sleep 1h` 명령을 실행.

### 컨테이너의 격리 구조 — Namespace 
컨테이너는 가상 머신(VM)처럼 완전히 분리된 존재가 아니다.  
컨테이너와 호스트는 동일한 커널(kernel)을 공유한다.

컨테이너의 격리는 Linux Namespace 기능으로 이루어진다.
- 호스트에는 자체 네임스페이스가 있고  
- 각 컨테이너는 자신만의 별도 네임스페이스를 가진다.

컨테이너 내부에서 실행되는 모든 프로세스는 사실상 호스트 위에서 실행되는 것이지만, 서로 다른 네임스페이스에 속한다.

컨테이너 내부에서는 자기 자신의 프로세스만 보인다.  
외부(호스트나 다른 컨테이너)의 프로세스는 볼 수 없다.

예를 들어,
- 컨테이너 내부에서 `ps` 명령을 실행하면 `sleep` 프로세스가 PID 1로 표시된다.  
- 하지만 호스트에서 보면, 같은 프로세스는 다른 PID로 나타난다.

이것이 Docker가 컨테이너를 격리하는 방법, 즉 프로세스 격리(Process Isolation)다.

### 사용자(User) 관점의 보안

호스트에는 여러 사용자가 있다:
- `root` 사용자
- 일반(non-root) 사용자들

기본적으로 Docker는 컨테이너 내 프로세스를 root 권한으로 실행한다. 컨테이너 안과 밖(호스트 양쪽)에서 보면 해당 프로세스는 root로 실행되고 있다.

그러나 보안을 강화하기 위해,  
컨테이너 내 프로세스를 root로 실행하지 않도록 설정할 수 있다.

### 사용자 지정 실행 예시

```bash
docker run --user 1000 ubuntu sleep 3600
```

이렇게 하면 프로세스는 `UID=1000` 사용자로 실행된다.

### 이미지 레벨에서 사용자 지정

이미지를 빌드할 때 `USER` 명령을 사용하면  
컨테이너 실행 시점에 따로 지정하지 않아도 된다.
```dockerfile
FROM ubuntu
USER 1000
CMD ["sleep", "3600"]
```
이후 이 이미지를 실행하면,  
프로세스는 자동으로 UID 1000으로 실행된다.

### 컨테이너의 root는 위험하지 않을까?
“컨테이너 안의 root가 호스트의 root와 동일한 권한을 가지는가?”  
→ No.  
Docker는 Linux Capabilities를 사용해 root 권한을 제한한다.  
컨테이너 내부의 root는 호스트 root만큼 강력하지 않는다.

### Linux Capabilities란?
리눅스에서는 root 사용자에게 모든 권한이 있다.  
예를 들어:
- 파일 및 권한 변경  
- 프로세스 생성 및 종료  
- 네트워크 포트 바인딩  
- 시스템 클록 변경  
- 시스템 재부팅  
- 그룹/사용자 ID 변경 등

이러한 권한들은 각각 Capability(기능)로 나뉘어 있다.

> 전체 목록은 Linux 시스템의 `/usr/include/linux/capability.h` 등에서 확인할 수 있다.

Docker는 이 중 일부만을 컨테이너에 허용한다.  
그래서 컨테이너의 root는 호스트를 재부팅하거나 네트워크를 제어할 수 없다.

### 권한 제어 방법
- 추가 권한 부여
  ```bash
  docker run --cap-add=NET_ADMIN ubuntu
  ```
  → `NET_ADMIN` 권한 추가

- 권한 제거
  ```bash
  docker run --cap-drop=CHOWN ubuntu
  ```

- 모든 권한 허용 (위험함)
  ```bash
  docker run --privileged ubuntu
  ```

### 정리
| 항목 | 설명 |
|------|------|
| Namespace | 프로세스 및 리소스 격리를 위한 Linux 기능 |
| root 실행 기본값 | Docker는 기본적으로 root로 실행함 |
| USER 지정 | 컨테이너 내 사용자 UID 설정 가능 |
| Capabilities | root 권한을 세분화하여 제어 |
| --cap-add / --cap-drop | 필요한 기능만 추가/제거 |
| --privileged | 모든 권한 허용 (보안상 비추천) |

### 결론
- Docker 컨테이너는 완전한 VM이 아니며, 커널을 공유합니다.  
- Namespace로 프로세스 격리, Capabilities로 권한 격리를 수행합니다.  
- root로 실행되더라도 호스트 시스템 전체 권한은 가질 수 없습니다.  
- 필요 시 `--user`, `--cap-add`, `--cap-drop` 등을 통해 세밀하게 제어할 수 있습니다.
