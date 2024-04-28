package Chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {
    public static void main(String[] args) {

        try (
                ServerSocket serverSocket = new ServerSocket(12345);
        ) {
            System.out.println("서버가 준비 되었습니다");
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
    private String nickname;
    private Map<String, PrintWriter> chatClients;
    private ArrayList<ChattingRoom> chattingRooms = new ArrayList<>();
    int roomNumber = 1;

    BufferedReader in = null;
    PrintWriter out;

    public ChatThread(Socket clientSocket, Map<String, PrintWriter> chatClients) {
        this.clientSocket = clientSocket;
        this.chatClients = chatClients;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            //닉네임 중복방지 로직
            while (true) {
                nickname = in.readLine();
                if (chatClients.containsKey(nickname)) {
                    out.print("이미 사용되고 있는 닉네임 입니다. 새로운 닉네임을 입력해 주세요: ");
                } else
                    break;
            }


            synchronized (chatClients) {
                chatClients.put(this.nickname, out);
            }//동시 접근을 막기 위한 동기화 블록

            //모든 사용자에게 nickname님이 입장했다는 정보를 알려줌
            broadcast(nickname + " 닉네임의 사용자가 연결했습니다.");
            out.println("방 목록 보기 : /list\n" +
                    "방 생성 : /create\n" +
                    "방 입장 : /join [방번호]\n" +
                    "방 나가기 : /exit\n" +
                    "접속종료 : /bye\n" +
                    "귓속말 : /whisper\n");
            System.out.println("새로운 사용자 " + nickname + " 님이 채팅 서버에 입장하였습니다. 서버 로비로 이동하였습니다.");
            InetAddress address = clientSocket.getInetAddress();
            System.out.println(nickname + " 님의 IP주소: " + address);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }//여기까지 생성자


    @Override
    public void run() {

        String msg = null;
        try {
            while ((msg = in.readLine()) != null) {
                if ("/bye".equalsIgnoreCase(msg)) {
                    System.out.println(nickname + " 닉네임의 사용자가 연결을 끊었습니다.");
                    break;
                }

                if (msg.indexOf("/create") == 0) {
                    chattingRooms.add(roomNumber - 1, new ChattingRoom(roomNumber, clientSocket, chatClients));
                    out.println("채팅 룸 " + roomNumber + " 번을 생성하고 입장하셨습니다.");
                    System.out.println(nickname + " 님이 " + roomNumber + "번 채팅룸을 생성했습니다.");
                    chattingRooms.get(roomNumber - 1).joinRoom(msg);
                    roomNumber++;
                }

                if (msg.indexOf("/list") == 0) {
                    out.println(chattingRooms.size() + "번 까지의 방이 있습니다.");

                }

                if (msg.indexOf("/exit") == 0) {
                    chattingRooms.get(roomNumber).exitRoom();

                }

                if (msg.indexOf("/join") == 0) {

                }

                if (msg.indexOf("/whisper") == 0) {
                    whisper(msg);
                } else {
                    broadcast(nickname + " : " + msg);
                }


            }

        } catch (IOException e) {
            System.out.println(e);
        } finally {
            synchronized (chatClients) {
                chatClients.remove(nickname);
            }
            broadcast(nickname + " 닉네임의 사용자가 연결을 끊었습니다.");

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
    public void whisper(String msg) {
        int firstSpaceIndex = msg.indexOf(" ");
        if (firstSpaceIndex == -1) return; //공백이 없다면 실행 안함, 아이디 판별 불가

        int secondSpaceIndex = msg.indexOf(" ", firstSpaceIndex + 1);
        if (secondSpaceIndex == -1) return; // 두번째 공백이 없다는 것도 메시지가 잘못된 것이다.

        String to = msg.substring(firstSpaceIndex + 1, secondSpaceIndex);
        String message = msg.substring(secondSpaceIndex + 1);

        //to(수신자에게 메시지 전송)
        PrintWriter pw = chatClients.get(to);
        PrintWriter me = chatClients.get(this.nickname);
        if (pw != null) {
            pw.println(nickname + "님으로부터 온 비밀 메시지: " + message);
            me.println(nickname + "님에게 보낸 비밀 메시지: " + message);
            System.out.println(nickname + "님이 " + to + "님에게 비밀 메시지를 전송했습니다.");
        } else {
            System.out.println("오류 : 수신자 " + to + "님을 찾지 못했습니다.");
        }
    }
}

class ChattingRoom {
    private int roomNumber;
    private Map<String, PrintWriter> chatClients;
    private Socket clientSocket;
    private Map<String, PrintWriter> roomClients;


    public ChattingRoom(int roomNumber, Socket clientSocket, Map<String, PrintWriter> chatClients) {
        this.roomNumber = roomNumber;
        this.chatClients = chatClients;
        this.clientSocket = clientSocket;
    }


    public void joinRoom(String msg) {
        try {
            while ((msg = in.readLine()) != null) {


            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

    }

    public void exitRoom() {

    }

    public void broadcastRoom() {
//        for (PrintWriter out : roomClients.values()) {
//            out.println(msg);
//        }

    }

}