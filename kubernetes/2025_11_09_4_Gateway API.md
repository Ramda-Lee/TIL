# 쿠버네티스

## Gateway API 소개

### 왜 Gateway API인가
- 멀티테넌시: 하나의 Ingress 리소스를 여러 팀이 공유·경합해야 하는 문제를 해소. 역할/경계를 분리해 충돌을 줄임.
- 범용성: Ingress는 HTTP 중심 + 컨트롤러별 어노테이션 의존. Gateway API는 L4/L7(HTTP, HTTPS, TCP, UDP, TLS, gRPC)까지 표준 스펙으로 지원.
- 이식성: 컨트롤러 특화 어노테이션 없이 공통 스펙으로 선언 → 검증/가독성/재사용성 향상.

### 역할과 오브젝트
- GatewayClass(인프라 제공자): 어떤 컨트롤러/인프라를 사용할지 정의.
- Gateway(클러스터 운영자): 리스너(포트/프로토콜/TLS)와 라우트 수용 정책 관리.
- HTTPRoute 등(앱 개발자): 호스트/경로 매칭, 필터(리다이렉트, 리라이트, 헤더, CORS), 백엔드 서비스 참조, 트래픽 분할.
- 주의: Ingress와 마찬가지로 Gateway API 컨트롤러를 별도로 배포해야 동작한다.

### 기본 예시
GatewayClass
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: GatewayClass
metadata:
  name: example-class
spec:
  controllerName: example.net/gateway-controller
```

Gateway (HTTP 80, HTTPS 443 리스너)
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: example-gateway
spec:
  gatewayClassName: example-class
  listeners:
  - name: web-https
    hostname: myonlinestore.com
    port: 443
    protocol: HTTPS
    tls:
      mode: Terminate
      certificateRefs:
      - kind: Secret
        name: myonlinestore-tls
    allowedRoutes:
      kinds:
      - kind: HTTPRoute
  - name: web-http
    hostname: myonlinestore.com
    port: 80
    protocol: HTTP
    allowedRoutes:
      kinds:
      - kind: HTTPRoute
```

HTTPRoute (호스트/경로 매칭 → 서비스 라우팅)
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: store-routes
spec:
  hostnames: ["myonlinestore.com"]
  parentRefs:
  - name: example-gateway
  rules:
  - matches:
    - path:
        type: PathPrefix
        value: "/"
    backendRefs:
    - name: web-service
      port: 80
```

### HTTP → HTTPS 리다이렉트 (어노테이션 없이)
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: http-redirect
spec:
  hostnames: ["myonlinestore.com"]
  parentRefs:
  - name: example-gateway
    sectionName: web-http
  rules:
  - filters:
    - type: RequestRedirect
      requestRedirect:
        scheme: https
        statusCode: 301
```

### 트래픽 분할 (카나리/블루그린)
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: app-split
spec:
  hostnames: ["myonlinestore.com"]
  parentRefs:
  - name: example-gateway
  rules:
  - matches:
    - path:
        type: PathPrefix
        value: "/"
    backendRefs:
    - name: app-v1
      port: 80
      weight: 80
    - name: app-v2
      port: 80
      weight: 20
```

### URL Rewrite (Ingress `rewrite-target` 대체)
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: rewrite-watch-wear
spec:
  parentRefs:
  - name: example-gateway
  hostnames: ["myonlinestore.com"]
  rules:
  - matches:
    - path: { type: PathPrefix, value: "/watch" }
    filters:
    - type: URLRewrite
      urlRewrite:
        path:
          type: ReplacePrefixMatch
          replacePrefixMatch: "/"
    backendRefs:
    - name: watch-service
      port: 80
  - matches:
    - path: { type: PathPrefix, value: "/wear" }
    filters:
    - type: URLRewrite
      urlRewrite:
        path:
          type: ReplacePrefixMatch
          replacePrefixMatch: "/"
    backendRefs:
    - name: wear-service
      port: 80
```

### CORS/헤더 조작 (컨트롤러 중립 필터)
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: headers-cors
spec:
  parentRefs:
  - name: example-gateway
  hostnames: ["api.myonlinestore.com"]
  rules:
  - filters:
    - type: ResponseHeaderModifier
      responseHeaderModifier:
        add:
        - name: Access-Control-Allow-Origin
          value: "https://app.myonlinestore.com"
        - name: Access-Control-Allow-Methods
          value: "GET, POST, PUT, DELETE, OPTIONS"
    backendRefs:
    - name: api-service
      port: 80
```

### 구현 현황 (요약)
- 다수의 컨트롤러가 Gateway API를 채택/지원(GA 포함): Envoy Gateway, Istio, NGINX Gateway/Fabric, GKE, HAProxy, Contour, Traefik 등.
- 컨트롤러별 세부 기능/제약은 다를 수 있으나, 리소스 스펙은 공통이므로 이전보다 이식성이 높다.

Gateway API를 사용하면 멀티테넌시, L4/L7 혼합 시나리오, 트래픽 분할, URL 재작성, 리다이렉트, 헤더/CORS 설정 등을 표준 스펙으로 선언할 수 있어 운영 복잡도를 줄이고 컨트롤러 종속성을 완화할 수 있다.

### Practical Guide (2025 업데이트 요약)
- 설치(NGINX Gateway Controller 예):
  - CRD 적용(standard/experimental) 후 헬름으로 컨트롤러 설치.
  ```bash
  kubectl kustomize "https://github.com/nginx/nginx-gateway-fabric/config/crd/gateway-api/standard?ref=v1.6.2" | kubectl apply -f -
  kubectl kustomize "https://github.com/nginx/nginx-gateway-fabric/config/crd/gateway-api/experimental?ref=v1.6.2" | kubectl apply -f -
  helm install ngf oci://ghcr.io/nginx/charts/nginx-gateway-fabric --create-namespace -n nginx-gateway
  ```
- GatewayClass: 컨트롤러를 지정해 Gateway의 “청사진”을 정의.
  ```yaml
  apiVersion: gateway.networking.k8s.io/v1
  kind: GatewayClass
  metadata: { name: nginx }
  spec: { controllerName: nginx.org/gateway-controller }
  ```
- Gateway(Listener): 포트/프로토콜/허용 라우트 네임스페이스를 선언.
  ```yaml
  kind: Gateway
  spec:
    gatewayClassName: nginx
    listeners:
    - name: http
      protocol: HTTP
      port: 80
      allowedRoutes: { namespaces: { from: All } }
  ```
- HTTPRoute 기본 라우팅: 경로 접두사 `/app` → `my-app:80`.
  ```yaml
  kind: HTTPRoute
  spec:
    parentRefs: [{ name: nginx-gateway }]
    rules:
    - matches: [{ path: { type: PathPrefix, value: /app } }]
      backendRefs: [{ name: my-app, port: 80 }]
  ```
- 리다이렉트: HTTP→HTTPS를 `RequestRedirect` 필터로 선언.
  ```yaml
  filters: [{ type: RequestRedirect, requestRedirect: { scheme: https } }]
  ```
- 리라이트: `URLRewrite.replacePrefixMatch`로 `/old`→`/new`.
  ```yaml
  filters:
  - type: URLRewrite
    urlRewrite: { path: { replacePrefixMatch: /new } }
  ```
- 헤더 수정: `RequestHeaderModifier`로 요청 헤더 추가/설정/삭제.
  ```yaml
  filters:
  - type: RequestHeaderModifier
    requestHeaderModifier: { add: [{ name: x-env, value: staging }] }
  ```
- 트래픽 분할: `backendRefs.weight`로 80/20 등 가중치 분배.
  ```yaml
  backendRefs:
  - { name: v1-service, port: 80, weight: 80 }
  - { name: v2-service, port: 80, weight: 20 }
  ```
- 리퀘스트 미러링: `RequestMirror`로 복제 트래픽을 미러 서비스로 전송.
  ```yaml
  filters:
  - type: RequestMirror
    requestMirror: { backendRef: { name: mirror-service, port: 80 } }
  ```
- TLS 종료: Gateway 리스너에서 `tls.mode: Terminate`, `certificateRefs`로 시크릿 참조.
  ```yaml
  listeners:
  - name: https
    protocol: HTTPS
    port: 443
    tls: { mode: Terminate, certificateRefs: [{ kind: Secret, name: tls-secret }] }
  ```
- L4 프로토콜: `Gateway`의 `protocol: TCP/UDP`로 3306/53 등 리스너 구성.
  ```yaml
  listeners: [{ name: tcp, protocol: TCP, port: 3306 }]
  listeners: [{ name: udp, protocol: UDP, port: 53 }]
  ```
- gRPC: 컨트롤러에 따라 `HTTPRoute` 또는 `GRPCRoute`로 서비스/메서드 매칭을 지원.

참고: 공식 문서 gateway-api.sigs.k8s.io 에서 컨트롤러 호환성, 필터 목록, Route 타입(HTTP/TCP/UDP/TLS/gRPC) 최신 상태를 확인한다.
