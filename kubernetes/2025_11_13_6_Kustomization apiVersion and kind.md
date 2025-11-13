# 쿠버네티스

## kustomization.yaml의 apiVersion/kind

### 왜 명시해야 하나
- `kustomization.yaml`도 다른 리소스 파일처럼 `apiVersion`과 `kind`를 설정할 수 있음.
- 일부 버전에서는 생략해도 동작하지만, 도구(특히 `kubectl -k`/독립형 kustomize) 버전 차이로 인한 호환성 이슈를 피하려면 고정(pinning) 권장.

### 권장 표기(안정적)
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - deployment.yaml
  - service.yaml

commonLabels:
  app.kubernetes.io/part-of: myapp
```
- 현재(일반적 환경) `v1beta1` + `Kustomization` 조합이 가장 널리 사용되고 호환성 좋음.
- kustomize 독립 실행형과 `kubectl kustomize` 모두 인식.

### 호환성 팁
- 생략 시 도구가 기본값을 추정하지만, 미래 변경으로 깨질 수 있음 → 명시 추천.
- 팀/CI 환경이 여러 버전의 `kubectl`/`kustomize`를 혼용한다면 반드시 명시.

### 확인 방법
```bash
kustomize build path/ | head -n 1 >/dev/null  # 렌더 성공 여부
kubectl kustomize path/ >/dev/null            # kubectl 내장과도 호환 확인
```

### 요약
- `apiVersion: kustomize.config.k8s.io/v1beta1`, `kind: Kustomization`을 kustomization.yaml 상단에 고정해 호환성 확보.
- 생략 가능하더라도, 미래 깨짐 방지를 위해 명시하는 습관 권장.
