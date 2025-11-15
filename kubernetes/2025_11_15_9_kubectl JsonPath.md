# 쿠버네티스

## kubectl JsonPath 활용

### 개요
대규모 클러스터에서 수백/수천 개 리소스의 특정 필드만 요약해 보고 싶을 때 `kubectl -o jsonpath`가 유용하다. JSON 출력 구조를 이해하고 JsonPath로 필요한 값을 추출·포매팅한다.

### 왜 JsonPath인가
- 기본 출력 또는 `-o wide`만으로는 원하는 필드 조합을 보기 어렵다.
- 원하는 필드만 뽑아 표 형태로 정리하거나 필터링/정렬이 가능하다.

### 사용 절차
1) 대상 리소스 명령 결정: `kubectl get nodes`, `kubectl get pods -A` 등  
2) JSON 구조 파악: `-o json`으로 전체 구조 확인  
3) JsonPath 작성/검증: 예시 출력으로 경로를 맞춘다  
4) 명령에 적용: `-o jsonpath='{...}'`

### 기본 예시
노드 이름 목록
```bash
kubectl get nodes -o jsonpath='{.items[*].metadata.name}'
```
노드 아키텍처
```bash
kubectl get nodes -o jsonpath='{.items[*].status.nodeInfo.architecture}'
```
Pod와 이미지 목록(현재 네임스페이스)
```bash
kubectl get pods -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].image}{"\n"}{end}'
```

### 포매팅(개행/탭)
- 개행: `{"\n"}`  
- 탭: `{"\t"}`

예: 노드 이름과 CPU 용량을 탭으로 구분해서 한 줄씩
```bash
kubectl get nodes -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.capacity.cpu}{"\n"}{end}'
```

### range 루프
리스트를 순회하며 각 항목의 여러 필드를 출력
```bash
kubectl get pods -A \
  -o jsonpath='{range .items[*]}{.metadata.namespace}{"/"}{.metadata.name}{"\t"}{.status.phase}{"\n"}{end}'
```

### custom-columns로 표 만들기
간단한 표 형태는 `-o custom-columns=`가 더 읽기 쉽다.
```bash
kubectl get nodes \
  -o custom-columns=NODE:.metadata.name,CPU:.status.capacity.cpu,ARCH:.status.nodeInfo.architecture
```
주의: custom-columns는 각 항목에 대해 경로를 해석하므로 `items[*]`는 생략한다.

### 정렬(sort-by)
특정 필드를 기준으로 정렬
```bash
# 이름순
kubectl get nodes --sort-by=.metadata.name

# CPU 용량순 (숫자 비교가 필요한 경우는 별도 도구 활용 고려)
kubectl get nodes --sort-by=.status.capacity.cpu
```

### 실전 팁
- 따옴표 이스케이프에 주의: 셸에 따라 `'{...}'` 내부의 `"\n"` 같은 이스케이프가 필요하다.
- 먼저 `-o json`으로 구조를 확인하고 JsonPath를 점진적으로 확장한다.
- 컬럼 혼합이 많다면 custom-columns, 복잡한 포매팅은 jsonpath의 range/개행을 조합한다.

