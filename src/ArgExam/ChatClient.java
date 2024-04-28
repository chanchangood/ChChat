package ArgExam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {

    public static void main(String[] args) {
        //아이디가 처음에 입력되게 하기 위해서 args[0] 에서 받아오는 것으로 구현한다.
        if (args.length != 1) {
            System.out.println("사용법: java ArgExam.ChatClient id");
            System.exit(1);
        }

        try (
                Socket clientSocket = new Socket("localhost", 12345);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        ) {
            //접속되면 id를 서버에 보낸다. 서버와의 약속한 것! 클라이언트는 아규먼트값(id)를 가지고 들어와야한다.
            out.println(args[0]);

            //네트워크에서 입력을 담당하는 부분을 Thread로
            new InputThread(clientSocket, in).start();

            //키보드로부터 입력받은 내용을 서버로 출력하는 코드
            String msg = null;
            while ((msg = keyboard.readLine()) != null) {
                out.println(msg);

                if ("/quit".equalsIgnoreCase(msg))
                    break;
            }

        } catch (Exception e) {
            System.out.println(e);
        }

    }
}

class InputThread extends Thread {
    private Socket clientSocket;
    private BufferedReader in;

    InputThread(Socket clientSocket, BufferedReader in) {
        this.clientSocket = clientSocket;
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String msg = null;
            while ((msg = in.readLine())!= null){
                System.out.println(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}