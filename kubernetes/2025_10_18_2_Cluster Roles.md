# 쿠버네티스

## Cluster Roles

### Namespace 범위 vs Cluster 범위
- Namespace 리소스
  - 특정 네임스페이스에 속하며, 그 안에서만 접근 제어된다.
  - 예시 리소스: Pods, ReplicaSets, Deployments, Services, Secrets, Jobs
- Cluster-scoped 리소스
  - 네임스페이스에 속하지 않고 클러스터 전체에 걸쳐 존재
  - Nodes, PersistentVolumes, ClusterRoles, ClusterRoleBindings, CertificateSigningRequests, Namespaces 등
- 네임스페이스를 지정하지 않으면 default 네임스페이스에 생성된다.
- 전체 목록 확인 명령어
  ```
    kubectl api-resources --namespaced=false
  ```
### ClusterRole
- Role과 구조는 동일하지만, 클러스터 전체 범위의 리소스를 제어할 때 사용
- 네임스페이스에 속하지 않음
- 예시
  - ClusterAdminRole: 노드 조회, 생성, 삭제, 권한 부여
  - StorageAdminRole: PersistenVolume 및 PVC 생성 권한 부여
```
  #ClusterRole 생성 예시
  kubectl create clusterrole cluster-admin --verb=get, list, create, delete --resource=nodes
```

### ClusterRoleBinding 
- ClusterRole을 특정 사용자 또는 서비스 계정에 연결하는 객체
- 구조
  - kind: ClusterRoleBinding
  - Subjects: 사용자 정보(Cluster-admin-user)
  - roleRef: 연결할 ClusterRole 정보
```
  # ClusterRoleBinding 생성 예시
  kubectl create clusterrolebinding limit-cluster-admin-role-binding --clusterrole=cluster-admin --user=cluster-admin-user
```

### 네임스페이스 리소스에도 사용 가능
- ClusterRole은 클러스터 범위 리소스 전용이지만 네임스페이스 리소스에도 적용가능
- 이 경우 사용자는 모든 네임스페이스의 동일 리소스에 접근 가능
- 예시
  - Role 사용 시 -> 특정 네임스페이스의 pods 접근만 허용
  - ClusterRole 사용 시 -> 클러스터 전체의 Pods 접근 가능

### 기본 제공 ClusterRoles
- kubernetes는 클러스터 초기 설정 시 여러 개의 기본 ClusterRole를 자동 생성한다.
- 예: cluster-admin, system:node, view, edit, admin 등