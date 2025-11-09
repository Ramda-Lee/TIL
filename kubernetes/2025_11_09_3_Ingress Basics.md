# 쿠버네티스

## Ingress Basics

### 서비스 한계와 문제 정의
- 애플리케이션을 NodePort/LoadBalancer 서비스로 외부에 노출하면 각 서비스마다 포트·공인 IP·클라우드 L4 로드밸런서가 필요해 비용·운영 부담이 커진다.
- 포트 번호(30080 등)를 기억해야 하고, SSL 종료 지점이나 URL 기반 라우팅(예: `/watch`, `watch.myonlinestore.com`)을 일관되게 구성하기 어렵다.
- 서비스가 늘어날수록 외부 프록시/방화벽/로드밸런서를 매번 재구성해야 하며, 팀 간 조율과 인증서 관리가 분산된다.

### Ingress 개념
- Ingress = 쿠버네티스 내부에 배포하는 L7(HTTP/HTTPS) 로드밸런서.
- 두 컴포넌트:
  1. Ingress Controller: nginx, GCE, Contour, HAProxy, Traefik, Istio 등. 클러스터 이벤트를 감시해 실제 프록시 구성을 갱신.
  2. Ingress Resource: URL/호스트별 라우팅, SSL, 기본 백엔드 등을 정의하는 Kubernetes 오브젝트(`kind: Ingress`).
- Controller는 기본으로 포함되지 않으므로, 직접 배포해야 ingress 리소스가 동작한다.

### Controller 배포 개요 (nginx 예)
1. `Deployment`: 특수 빌드의 `nginx-ingress-controller` 이미지를 1+ replica로 실행. 커맨드는 `/nginx-ingress-controller`.
2. `ConfigMap`: nginx 설정값(로그, keepalive, SSL 옵션 등)을 decouple. 처음엔 비어 있어도 되고 추후 수정이 쉽다.
3. 환경 변수: Pod 이름/네임스페이스를 주입해 런타임 설정에 활용.
4. Service: NodePort(또는 LoadBalancer)로 80/443을 노출해 외부 트래픽을 한 번만 받아오도록 구성.
5. ServiceAccount + (Cluster)RoleBinding: 컨트롤러가 Ingress 리소스를 watch하고 관련 오브젝트를 읽을 수 있도록 권한 부여.

### Ingress 리소스 구조
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-ingress
spec:
  defaultBackend:
    service:
      name: default-http-backend
      port:
        number: 80
  rules:
  - host: myonlinestore.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: web-service
            port:
              number: 80
      - path: /watch
        pathType: Prefix
        backend:
          service:
            name: video-service
            port:
              number: 80
  - host: wear.myonlinestore.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: apparel-service
            port:
              number: 80
```
- `rules[].host`: 도메인별 라우팅, 여러 DNS 레코드가 같은 Ingress IP/포트를 가리키도록 설정.
- `paths`: URL 경로 기반 라우팅. `Prefix`로 `/watch` 이하 전체를 비디오 서비스로 전달.
- `defaultBackend`: 어떤 rule/path에도 맞지 않는 요청 처리(예: 404 페이지).
- TLS 섹션을 추가하면 인증서/secret을 한 곳에서 관리하여 HTTPS 종료를 통합할 수 있다.

### 버전 변화와 스펙 업데이트
- Kubernetes 1.19 이전: `extensions/v1beta1` 또는 `networking.k8s.io/v1beta1` API를 사용하고, 백엔드 지정 시 `serviceName`, `servicePort` 필드를 직접 작성했다.
- 1.19+ (1.22부터 베타 API 제거): `networking.k8s.io/v1` 가 기본이며, 백엔드는 `service.name`, `service.port.number/name` 또는 `resource` 블록으로 명시한다. `pathType`(`Prefix`, `Exact`, `ImplementationSpecific`) 선언이 필수다.
- 기존 매니페스트를 최신 버전으로 마이그레이션할 때는 apiVersion 변경, `backend.service` 블록 구조 적용, `pathType` 추가 여부를 확인해야 한다.

### Annotations와 rewrite-target
- 서로 다른 앱을 `/watch`, `/wear` 경로로 노출하되, 백엔드 앱은 루트(`/`)로만 서비스되는 경우 URL을 "재작성(rewrite)"해야 한다.
- rewrite가 없으면 `/watch`가 그대로 백엔드에 전달되어 `http://watch-service/watch`가 되어 404가 난다.
- NGINX Ingress Controller에서 `nginx.ingress.kubernetes.io/rewrite-target` 어노테이션으로 경로를 치환한다.

간단히 접두사 제거(Strip prefix) 예:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: watch-wear
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - http:
      paths:
      - path: /watch
        pathType: Prefix
        backend:
          service:
            name: watch-service
            port:
              number: 80
      - path: /wear
        pathType: Prefix
        backend:
          service:
            name: wear-service
            port:
              number: 80
```
- 효과: `http://<ingress>/watch` → 내부적으로 `http://watch-service/`로 전달, `http://<ingress>/wear` → `http://wear-service/`로 전달.

정규식으로 세밀 제어(잔여 경로 보존) 예:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rewrite-regex
  annotations:
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rewrite-target: /$2
spec:
  rules:
  - host: rewrite.bar.com
    http:
      paths:
      - path: /something(/|$)(.*)
        pathType: ImplementationSpecific
        backend:
          service:
            name: http-svc
            port:
              number: 80
```
- 여기서 `/$2`는 캡처 그룹을 활용해 `/something` 접두사만 제거하고 나머지 경로를 유지한다.
- 정규식 매칭을 쓰려면 `use-regex: "true"`와 `pathType: ImplementationSpecific` 조합을 권장한다.

### 명령형(Inperative) 생성 예시
- 1.20+에서는 단순 규칙을 빠르게 만들 수 있는 `kubectl create ingress` 명령이 추가되었다.
- 형식: `kubectl create ingress <name> --rule="host/path=service:port"`
- 예시: `kubectl create ingress ingress-test --rule="wear.my-online-store.com/wear*=wear-service:80"`
  - `wear*`처럼 패턴을 주어 Prefix 매칭을 간단히 표현 가능(내부적으로 `Prefix` pathType).
  - 생성 후 `kubectl edit ingress`로 TLS, 여러 규칙 등을 확장하거나 GitOps용 선언적 매니페스트로 변환해 관리한다.

### 운영 포인트
- Ingress는 하나의 외부 엔드포인트로 묶고, 내부 서비스 간 트래픽 분배·SSL·리다이렉션을 선언적으로 관리한다.
- Controller는 여전히 NodePort/LoadBalancer 중 하나로 노출해야 하며, 이후엔 Ingress 리소스만 추가/수정하면 된다.
- `kubectl get ingress`/`describe ingress`로 라우팅 규칙과 기본 백엔드, TLS 상태를 확인하고, Controller 로그에서 nginx 재로딩 여부를 점검한다.
