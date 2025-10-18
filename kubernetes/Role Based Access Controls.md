# 쿠버네티스

## Role Based Access Controls

### Role 생성
- 역할은 Role 객체를 정의해서 만듭니다.
  - apiVersion: rbac.authorization.k8s.io/v1
  - kind: Role
  - metadata.name: developer
- Role에는 rules를 지정하며, 각 rule은 세가지 섹션으로 구성된다.
  1. apiGroups: core 그룹은 비워두고, 다른 그룹은 이름을 지정
  2. resources: 접근할 리소스(예: pods, configmaps)
  3. verbs: 허용할 동작(예: get, list, create, delete)
- 개발자에게 pods에 대한 읽기/생성/삭제/권한을 주고, ConfigMap 생성도 허용하려면 두 개의 rule을 Role 안에 추가할 수 있다. -> 하나의 Role에 여러 rule을 담는 것이 가능하다
- Role은 다음 명령으로 생성
  ```
    kubectl create role developer --verb=list, get, create, delete --resource=pods
  ```

### RoleBinding (역할 연결)
- Role을 만들었다면 특정 사용자를 Role에 연결해야 한다 -> 이를 위해 RoleBinding 객체를 생성한다.
- RoleBinding은 User(or ServiceAccount)와 Role을 연결한다.
  - 이름: dev-user-to-developer-binding
  - kind: RoleBinding
  - subjects: 연결할 사용자 정보
  - roleRef: 연결할 Role의 이름과 API 버전
- RoleBinding 생성 명령어
  ```
    kubectl create rolebinding dev-user-to-developer-binding --role=developer --user=dev-user
  ```

### Namespace 범위
- Role과 RoleBinding은 namespace 단위로 작동한다. 즉, dev-user가 defalut 네임스페이스에서만 pods와 configmaps를 다룰 수 있다.
- 다른 네임스페이스에서 권한을 주려면 Role과 RoleBinding 정의의 metadata.namespace를 바꿔야 한다.

### Role / RoleBinding 조회
- Role 목록 보기
  ```
    kubectl get roles
  ```
- Role 상세 보기
  ```
    kubectl describe role developer
  ```
- RoleBinding 목록 보기
  ``` 
    kubectl get rolebindings
  ```
- RoleBinding 상세 보기
  ```
    kubectl describe rolebinding dev-user-to-developer-binding
  ```

### 권한 테스트
- 특정 사용자가 어떤 작업을 할 수 있는지 확인하려면
  ``` 
    kubectl auth can-i create deployments
  ```
- 관리자는 다른 사용자로 가상 실행도 가능하다
  ```
    kubectl auth can-i create pods --as dev-user
  ```
  - developer 역할에는 deployments 권한이 없으므로 "no"가 출력
  - 하지만 pods에 대해서는 "yes"가 출력
- 네임스페이스를 지정해서 확인할 수도 있다.
  ```
    kubectl auth can-i create pods -n test --as dev-user
  ```
  - test 네임스페이스에 권한이 없으면 "no"

### 리소스 이름 단위 제어
- 더 세밀하게 제어하고 싶으면 리소스이름 필드를 추가하여 제어한다.
```
  resourceNames:
    - blue
    - orange
```
- 예를 들어, 네임스페이스 내에 5개의 pod가 있을 때 특정 두개(blue, orange)만 접근하게 하려면 rule안에 resourceNames 필드를 추가한다.