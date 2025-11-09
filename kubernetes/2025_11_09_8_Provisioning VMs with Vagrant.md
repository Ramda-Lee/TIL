# 쿠버네티스

## VirtualBox + Vagrant로 VM 프로비저닝

### 개요
- 목표: 1 마스터(`kubemaster`) + 2 워커(`kubenode01`, `kubenode02`) VM을 동일한 설정으로 신속히 생성.
- 도구: VirtualBox(하이퍼바이저) + Vagrant(자동화/프로비저닝).

### 사전 준비
- VirtualBox 설치: virtualbox.org → OS별 설치 파일 다운로드.
- Vagrant 설치: vagrantup.com 문서에 따라 OS별 설치.
- 코스 저장소 클론(예: Vagrantfile 제공 리포지토리):
  ```bash
  git clone <COURSE_REPO_URL>
  cd <REPO_DIR>
  ls  # Vagrantfile 존재 확인
  ```

### Vagrantfile 개요(예시)
- 구성: 마스터 1대 + 워커 2대, 호스트온리 네트워크 `192.168.56.0/24` 사용.
- 베이스 박스: `ubuntu/bionic64` 등.
- 메모리/CPU, 고정 IP, 호스트네임 등을 통일해 정의.

### 기본 명령어
- 상태 확인:
  ```bash
  vagrant status
  ```
- VM 생성/부팅(모든 노드 일괄):
  ```bash
  vagrant up
  ```
  - 최초 실행 시 베이스 이미지 다운로드 후 각 VM 프로비저닝(시간 소요).
- 접속(예: 마스터):
  ```bash
  vagrant ssh kubemaster
  logout  # 종료
  ```
- 다른 노드 접속:
  ```bash
  vagrant ssh kubenode01
  vagrant ssh kubenode02
  ```
- 재확인:
  ```bash
  vagrant status  # running 상태 확인
  ```

### 자주 쓰는 운영 커맨드
- 중지/재부팅:
  ```bash
  vagrant halt [name]
  vagrant reload [name]
  ```
- 삭제(디스크 포함):
  ```bash
  vagrant destroy -f [name]
  ```

### 네트워크 참고
- Vagrantfile에 정의된 IP는 VM(호스트) IP로, 쿠버네티스 Pod/Service CIDR과는 별개다.
- 이후 kubeadm 부트스트랩 시 CNI의 Pod CIDR을 별도로 설정한다.

### 다음 단계
- 모든 VM이 running 상태가 되면 마스터에 접속하여 kubeadm으로 컨트롤 플레인 초기화 후, 워커에서 `kubeadm join` 실행.
- 세부 절차는 `2025_11_09_7_kubeadm Bootstrap.md` 참고.

### 참고 자료
- Vagrantfile 리포지토리: https://github.com/kodekloudhub/certified-kubernetes-administrator-course
- kubeadm 설치 문서: https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/
