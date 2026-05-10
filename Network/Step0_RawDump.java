import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Step0_RawDump {
    public static void main(String[] args) throws IOException {
      ServerSocket serverSocket = new ServerSocket(9090);

      while (true) {
        Socket clinetSocket = serverSocket.accept();
        System.out.println("클라이언트 연결: " + clinetSocket.getInetAddress());
        try(clinetSocket;
            InputStream in = clinetSocket.getInputStream();
            OutputStream out = clinetSocket.getOutputStream()){
          
          byte[] buffer = new byte[1024]; // 일단 1024 바이트 버퍼로 읽어서 그대로 출력
          int n = in.read(buffer);
          System.out.println("받은 데이터: " + new String(buffer, 0, n)); // 받은 데이터를 문자열로 출력
          System.out.println("======== 끝 (" + n + " bytes) ========");

          // 아무 응답이나 보내고 끊기
          String reponse = "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n";
          out.write(reponse.getBytes());
          out.flush();
        }
      }
    }
}