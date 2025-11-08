# 쿠버네티스

## Persistent Volume Claims

### 개요

Persistent Volume Claim(PVC) 을 생성하여 파드가 실제 스토리지를 사용할 수 있도록 만드는 방법을 배워보자

PV와 PVC는 서로 다른 쿠버네티스 오브젝트이다

| 구성 요소 | 역할 |
|-----------|------|
| PV (Persistent Volume) | 관리자가 미리 생성해둔 스토리지 |
| PVC (Persistent Volume Claim) | 사용자가 스토리지를 요청하는 객체 |

PVC가 생성되면 쿠버네티스는 PVC의 조건에 맞는 PV를 찾아 자동으로 바인딩(Bound) 한다.

---

### PVC와 PV의 관계

- PVC 하나는 오직 PV 하나에만 바인딩됩니다 (1:1 관계)
- PV 용량이 더 크더라도 나머지 용량은 다른 PVC가 사용할 수 없다.
- 매칭 조건:
  ✅ 요청 용량  
  ✅ Access Mode  
  ✅ Storage Class  
  ✅ Volume Mode

여러 PV가 조건을 만족하더라도, 라벨/셀렉터를 사용해 특정 PV를 선택할 수 있다.

#### 작은 PVC가 큰 PV에 바인딩될 수 있음
예: PVC 요청 500MB → PV 용량 1GB  
→ 다른 PV가 없다면 1GB PV에 자동 바인딩

---

### PV가 없을 때

PVC는 생성되지만 PV가 없거나 조건이 맞지 않으면:
➡ PVC 상태 = Pending

새로운 PV가 생성되면 PVC는 자동으로 바인딩됨.

---

### PVC 예시 생성 YAML

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: myclaim
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 500Mi
```

#### PVC 생성
```bash
kubectl apply -f myclaim.yaml
```

#### PVC 상태 확인
```bash
kubectl get pvc
```

초기에는 `Pending` → 이후 PV가 매칭되면 `Bound` 로 변경됨.

---

### PVC 삭제 시 PV의 동작 (Reclaim Policy)

PVC 삭제 후 PV 처리 방식은 Reclaim Policy 로 결정됩니다:

| 정책 | 설명 | 특징 |
|------|------|------|
| Retain (기본값) | PVC 삭제 후에도 PV는 남아있음 | 데이터 유지, 관리자 수동 정리 필요 |
| Delete | PVC 삭제 시 PV도 자동 삭제 | 클라우드 스토리지에서 자주 사용 |
| Recycle (Deprecated) | PV 데이터를 삭제 후 재사용 | 현재 폐기됨, 사용 권장 X |

#### 왜 Recycle이 폐기(deprecated) 되었나?
과거에는 내부적으로 작은 컨테이너가 떠서 다음 명령을 실행함:

```bash
rm -rf /volume/*
```

하지만 다음과 같은 문제점이  있었다:

- 파일 삭제만으로는 완전한 데이터 삭제 보장 불가  
- inode/메타데이터 잔존  
- 스냅샷, 암호화, 퍼미션, provider-level 삭제 불가  
- 클라우드 스토리지(EBS/GCE/Azure 등)에서는 제대로 적용되지 않음

➡ 최신 방식은 CSI + StorageClass를 통한 동적 프로비저닝

---

## 6. 요약

| 항목 | 설명 |
|------|------|
| PV | 미리 준비된 스토리지 |
| PVC | 사용자가 스토리지 요청 |
| 상태 흐름 | Pending → Bound |
| Reclaim 정책 | Retain / Delete (Recycle은 Deprecated) |
| 매칭 기준 | 용량, AccessMode, StorageClass, 라벨/셀렉터 |
| 최신 방식 | StorageClass + CSI 기반 동적 생성 |

---


### Pod에서 PVC 사용
PersistentVolumeClaim(PVC)을 생성한 후, Pod 내부에서 스토리지를 사용하기 위해서는 `volumes` 섹션에 PVC를 지정하고 `volumeMounts` 로 컨테이너 파일시스템에 마운트한다.

#### Pod 예시

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: mypod
spec:
  containers:
  - name: myfrontend
    image: nginx
    volumeMounts:
    - mountPath: "/var/www/html"
      name: mypd
  volumes:
  - name: mypd
    persistentVolumeClaim:
      claimName: myclaim
```

- `claimName`: 사용하려는 PVC 이름과 일치해야 한다.
- `mountPath`: 컨테이너 내부에서 볼륨이 연결되는 경로.
- `volumes[].name` 과 `volumeMounts[].name` 은 동일해야 한다.

#### Deployment 및 ReplicaSet 적용
동일한 방식으로 사용하며, `Pod Template` 내부에 정의한다.

```yaml
spec:
  template:
    spec:
      containers:
      - name: myfrontend
        image: nginx
        volumeMounts:
        - mountPath: "/var/www/html"
          name: mypd
      volumes:
      - name: mypd
        persistentVolumeClaim:
          claimName: myclaim
```
