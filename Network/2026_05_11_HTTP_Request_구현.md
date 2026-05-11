# HTTP Request 구조

## HTTP 요청라인 파싱
형식
```
Method SP Request-URI SP HTTP-Version CRLF
```
공백 2개로 정확히 3조각

### point
- 공백으로 split, 결과는 반드시 length == 3
  - length != 3 이면 명세 위반 → 400 Bad Request 에 해당
  - 검증 안 하면 ArrayIndexOutOfBoundsException 으로 서버가 죽거나 보안 취약점 (Request Smuggling)
- 메서드는 그냥 첫 단어, 자동 디폴트 없음
  - 클라이언트가 "GET" 안 보내면 GET 으로 자동 처리되는 거 아님
  - curl/브라우저가 알아서 GET 을 붙여서 보내는 것이지, 서버가 추론하는 게 아님
  - 소켓 = 바이트 통로일 뿐, HTTP 의미는 우리 코드가 부여함
- path 에는 query string 이 통째로 붙어있음
  - /search?q=hello&page=2 가 한 덩어리로 들어옴
  - 분리는 별도 처리

## Header 파싱
형식
```
field-name ":" OWS field-value OWS
```
OWS = Optional Whitespace.
### point
- 빈 줄을 만나면 헤더 끝
  - 라인 readLine 을 반복하다가 빈 문자열이 반환되면 break
  - 이 시점에 InputStream 의 커서는 정확히 바디의 첫 바이트 직전에 위치
  - 이게 다음 단계 (바디 읽기) 의 출발점
- 콜론은 indexOf(':') 로, 절대 split(":") 로 하지 않기
  - 헤더 값에 콜론이 들어갈 수 있음 (Host: localhost:9090)
  - 첫 번째 콜론 위치만 찾아서, 앞은 key, 뒤는 통째로 value
- key 는 소문자로 통일, value 는 그대로
  - HTTP 명세상 헤더 이름은 case-insensitive (Content-Type == content-type)
  - Map 에 넣을 때 소문자로 일관시켜야 조회가 일관됨
  - value 는 대소문자가 의미 있을 수 있으니 그대로 둠
- trim 으로 OWS 제거
  - Content-Type:application/json 도, Content-Type:   application/json    도 다 유효
  - 앞뒤 공백 제거해야 깔끔한 값을 얻음

## Query String 파싱
구조
```
/search?q=hello&page=2
   ↑    ↑    ↑
  path   ?    & 로 구분되는 파라미터
              = 로 key/value 분리
```

### point
- 첫 번째 ? 위치가 path/query 의 경계
  - indexOf('?') 사용
  - 없으면 query 자체가 없는 것
- & 로 split 은 안전, = 로는 indexOf
  - query 값 안에 & 가 들어가야 한다면 반드시 %26 으로 인코딩되어 옴 → raw & 는 항상 구분자
  - 반대로 값에 = 는 인코딩 없이 들어올 수 있음 (Base64 의 끝 == 등)
- key 와 value 는 둘 다 URLDecoder 로 디코딩
  - %EB%82%A8 → 남
  - + → 공백 (URLDecoder 가 query 영역 관습으로 자동 처리)
  - 반드시 charset 명시: URLDecoder.decode(s, StandardCharsets.UTF_8)
- = 없는 경우의 처리
  - ?debug 같이 값 없는 플래그형 파라미터
  - 보통 빈 문자열 값으로 처리

## Body 파싱
핵심 코드
```
byte[] body = new byte[contentLength];
int totalRead = 0;
while (totalRead < contentLength) {
    int n = in.read(body, totalRead, contentLength - totalRead);
    if (n == -1) break;  // 비정상 종료
    totalRead += n;
}
```

### point
- InputStream.read(buf, off, len) 은 요청한 만큼 다 안 읽을 수 있음
  - TCP 패킷이 쪼개져서 도착하기 때문
  - 작은 바디는 한 번에 오지만, 큰 바디는 여러 번 나눠서 옴
  - 반드시 누적 루프가 필요
- 누적 변수와 남은 바이트의 산수
  - totalRead — 지금까지 읽은 누적
  - contentLength - totalRead — 남은 만큼만 요청
  - 고정값으로 요청하면 다음 요청 데이터까지 빨아들임 (keep-alive 환경)
- Content-Length 가 바디의 정확한 경계
  - 더도 덜도 아니게 정확히 그 만큼만
  - 덜 읽으면 다음 요청이 깨지고, 더 읽으면 다음 요청을 먹어버림
- EOF (n == -1) 는 비정상
  - 클라이언트가 약속한 바이트 다 안 보내고 끊은 상황
  - 학습용 코드는 break, 실무 코드는 예외 처리
- 바디는 byte[] 로 받고, 디코딩은 별도
  - 바디는 텍스트일 수도 바이너리일 수도 있음
  - raw byte 로 일단 받아두고, 텍스트로 다루려면 Content-Type 의 charset 으로 디코딩
  - new String(body, StandardCharsets.UTF_8) 등