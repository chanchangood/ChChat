package ArgExam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {
    public static void main(String[] args) {

        try (
                //1 서버 소켓 생성
                ServerSocket serverSocket = new ServerSocket(12345);
        ) {
            //2 accept() 를 통해서 소켓을 얻어옴. (여러명의 클라이언트와 접속할 수 있도록 구현) + thread 이용
            //3 여러명의 클라이언트를 기억할 수 있는 자료구조가 필요, 맵 사용
            System.out.println("서버가 준비 되었습니다.");
            Map<String, PrintWriter> chatClients = new HashMap<>();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ChatThread(clientSocket, chatClients).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}

class ChatThread extends Thread {
    private Socket clientSocket;
    private String id;
    private Map<String, PrintWriter> chatClients;
    //다른 클라이언트에 귓속말을 보내기 위해 목록을 받음

    BufferedReader in = null;
    PrintWriter out;

    //생성자로 클라이언트 소켓을 얻어옴
    public ChatThread(Socket clientSocket, Map<String, PrintWriter> chatClients) {
        this.clientSocket = clientSocket;
        this.chatClients = chatClients;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            id = in.readLine();
            //이때 모든 사용자에게 id님이 입장했다는 정보를 알려줌
            broadcast(id + "님이 입장했습니다.");
            System.out.println("새로운 사용자 " + id + "님이 채팅 서버에 입장하였습니다.");

            synchronized (chatClients) {
                chatClients.put(this.id, out);
            }//동시 접근을 막기 위한 동기화 블록

        } catch (Exception e) {
            e.printStackTrace();
        }
    }//여기까지 생성자


    @Override
    public void run() {
        //run
        //각각 클라이언트와 통신할 수 있는 스트림 얻어옴
        //모든 클라이언트에게 전송하기 위해서(브로드캐스팅) 무엇을 해야할지 고민

        String msg = null;
        try {
            while ((msg = in.readLine()) != null) {
                if ("/quit".equalsIgnoreCase(msg))
                    break;

                if (msg.indexOf("/to") == 0) {
                    sendMsg(msg);
                } else
                    broadcast(id + " : " + msg);

            }

        } catch (IOException e) {
            System.out.println(e);
        } finally {
            synchronized (chatClients) {
                chatClients.remove(id);
            }
            broadcast(id + "님이 채팅에서 나갔습니다.");

            if (in != null) {
                try {
                    in.close();

                } catch (IOException e) {
                    System.out.println(e);
                }
            }

            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }

        }


    }

    //전체 사용자에게 새 클라이언트가 들어왔다는 것을 알려주는 메소드
    public void broadcast(String msg) {
        for (PrintWriter out : chatClients.values()) {
            out.println(msg);
        }
    }

    //메시지를 특정 사용자에게 보내는 메서드
    public void sendMsg(String msg) {
        int firstSpaceIndex = msg.indexOf(" ");
        if (firstSpaceIndex == -1) return; //공백이 없다면 실행 안함, 아이디 판별 불가

        int secondSpaceIndex = msg.indexOf(" ", firstSpaceIndex + 1);
        if (secondSpaceIndex == -1) return; // 두번째 공백이 없다는 것도 메시지가 잘못된 것이다.

        String to = msg.substring(firstSpaceIndex + 1, secondSpaceIndex);
        String message = msg.substring(secondSpaceIndex + 1);

        //to(수신자에게 메시지 전송)
        PrintWriter pw = chatClients.get(to);
        PrintWriter me = chatClients.get(this.id);
        if (pw != null) {
            pw.println(id + "님으로부터 온 비밀 메시지: " + message);
            me.println(id + "님에게 보낸 비밀 메시지: " + message);
            System.out.println(id + "님이 " + to + "님에게 비밀 메시지를 전송했습니다.");
        } else {
            System.out.println("오류 : 수신자 " + to + "님을 찾지 못했습니다.");
        }
    }
}
