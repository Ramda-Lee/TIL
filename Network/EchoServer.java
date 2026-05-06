import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
    public static void main(String[] args) throws IOException{
      
      // 서버 소켓 생성 및 9090 포트 대기
      ServerSocket serverSocket = new ServerSocket(9090);
      
      while (true) {
        // 연결 요청 받으면 새 socket 생성
        Socket clientSocket = serverSocket.accept();  
        System.out.println("클라이언트 연결: " + clientSocket.getInetAddress());
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String line;
        while ((line = in.readLine()) != null) {
          System.out.println("받은 메시지: " + line);
          out.println("Echo: " + line);  // 받은 메시지를 그대로 클라이언트로 전송
          if (line.isEmpty()) {
            break;  // 빈 줄이 오면 연결 종료
          }
        }
        clientSocket.close();  // 클라이언트 소켓 닫기
      }
    }
}