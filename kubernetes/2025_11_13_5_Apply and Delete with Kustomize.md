# 쿠버네티스

## Apply/Delete with Kustomize

### 개요
- `kustomize build`는 리소스를 “적용”하지 않고 최종 매니페스트를 출력만 함.
- 실제 배포/삭제는 `kubectl`과 함께 사용해 수행.

### 적용 방법 1) 파이프 사용(kustomize → kubectl)
```bash
kustomize build k8s | kubectl apply -f -
```
- 좌측(`kustomize build k8s`) 출력이 파이프로 우측(`kubectl apply -f -`)의 입력으로 전달됨.
- `-f -`는 표준입력(stdin)을 의미.

### 적용 방법 2) kubectl 네이티브(-k)
```bash
kubectl apply -k k8s
```
- `-k`는 디렉터리를 받아 `kustomization.yaml`을 인식해 렌더 후 적용.

### 삭제 방법 1) 파이프 사용
```bash
kustomize build k8s | kubectl delete -f -
```

### 삭제 방법 2) kubectl 네이티브(-k)
```bash
kubectl delete -k k8s
```

### 유용한 팁
- 사전 확인(dry-run):
```bash
kustomize build k8s | kubectl apply -f - --dry-run=client -o yaml
```
- 적용 후 확인:
```bash
kubectl get all -l company=KodeKloud
```

### 요약
- build는 출력만, 실제 배포/삭제는 `kubectl apply/delete`로 수행.
- 두 가지 방식 모두 지원: 파이프(`-f -`) 또는 네이티브 `-k`.
