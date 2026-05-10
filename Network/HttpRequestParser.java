import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

// 요청 라인의 HTTP 메서드, URL, 프로토콜 버전을 읽어서 출력하는 서버
public class HttpRequestParser {
  public static void main(String[] args) throws IOException{
    try(ServerSocket serverSocket = new ServerSocket(9090)){
      System.out.println("Listening on port http://localhost:9090");

      while (true) {
        try(Socket socket = serverSocket.accept();
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream()){
          // BufferedReader는 속에서 미리 많이 읽어버린다 
          // 나중에 바디를 InputStream으로 읽을 때 헤더 다음 바디 데이터가 BufferedReader의 내부 버퍼에 갇혀서 사라져버린다.
          // 그래서 \r\n 까지 한 줄을 바이트 단위로 직접 읽어서 처리
          ByteArrayOutputStream buf = new ByteArrayOutputStream();
          int prev = -1, cur;
          String requestLine = null;
          while ((cur = in.read()) != -1) {
            if (prev == '\r' && cur == '\n') {
              // CRLF(\r\n)로 줄이 끝났으므로 지금까지 읽은 바이트를 문자열로 변환해서 출력
              byte[] lineBytes = buf.toByteArray();
              // 마지막 \r 한 바이트 제외
              requestLine = new String(lineBytes, 0, lineBytes.length - 1, StandardCharsets.ISO_8859_1); // ISO_8859_1은 1바이트당 1문자 매핑, HTTP 헤더는 ASCII 기반이므로 적합
              break;
            }
            buf.write(cur);
            prev = cur;
          }
          if(requestLine == null || requestLine.isEmpty()) {
            System.out.println("빈 요청 라인");
            continue;
          }

          System.out.println("요청 라인: " + requestLine);

          // 공백으로 메서드/경로/버전 분리
          String[] tokens = requestLine.split(" ");
          if(tokens.length != 3) { // 요청 라인은 반드시 3개의 구성으로 이루어져 있어야한다.(공백으로 구분된다)
            System.out.println("잘못된 요청 라인 형식");
            continue;
          }

          String method = tokens[0];
          String path = tokens[1];
          String version = tokens[2];

          // 헤더 반복 파싱
          Map<String, String> headers = new java.util.HashMap<>();

          while (true) {
            // 헤더 한 줄씩 읽기
            buf = new ByteArrayOutputStream();
            prev = -1;
            String headerLine = null;
            while ((cur = in.read()) != -1) {
              if (prev == '\r' && cur == '\n') {
                byte[] lineBytes = buf.toByteArray();
                headerLine = new String(lineBytes, 0, lineBytes.length - 1, StandardCharsets.ISO_8859_1);
                break;
              }
              buf.write(cur);
              prev = cur;
            }
            // 빈 줄이 나왔다는 것은 헤더의 끝
            if(headerLine == null || headerLine.isEmpty()) {
              break;  
            }

            // : 기준으로 쪼개기
            int colon = headerLine.indexOf(':');
            if (colon < 0){
              // colon이 없는 경우 헤더는 비정상, 일단 무시
              System.out.println("잘못된 헤더 형식: " + headerLine);
              continue;
            }

            String key = headerLine.substring(0, colon).trim().toLowerCase();
            String value = headerLine.substring(colon + 1).trim();
            headers.put(key, value);
          }

          System.out.println("method: " + method);
          System.out.println("path: " + path);
          System.out.println("version: " + version);
          System.out.println("headers:");
          headers.forEach((k, v) -> System.out.println("  " + k + ": " + v));
          System.out.println();

          // 응답, 우선 OK만
          String response = "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n";
          out.write(response.getBytes(StandardCharsets.ISO_8859_1));
          out.flush();
        }
      }
    }
  }
}
