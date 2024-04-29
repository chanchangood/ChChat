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
            ArrayList<ChatThread.ChattingRoom> chattingRooms = new ArrayList<>();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ChatThread(clientSocket, chatClients, chattingRooms).start();
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
    private ArrayList<ChattingRoom> chattingRooms;
    int roomNumber = 0;

    BufferedReader in = null;
    PrintWriter out;

    public ChatThread(Socket clientSocket, Map<String, PrintWriter> chatClients, ArrayList<ChattingRoom> chattingRooms) {
        this.clientSocket = clientSocket;
        this.chatClients = chatClients;
        this.chattingRooms = chattingRooms;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            //닉네임 중복방지 로직
            while (true) {
                nickname = in.readLine();
                if (chatClients.containsKey(nickname)) {
                    out.println("이미 사용되고 있는 닉네임 입니다. 새로운 닉네임을 입력해 주세요: ");
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
            int inRoom = -1;

            while ((msg = in.readLine()) != null) {

                if ("/bye".equalsIgnoreCase(msg)) {
                    break;
                } else if ("/exit".equalsIgnoreCase(msg)) {
                    out.println("로비로 나갑니다.");
                    inRoom = -1;
                    chattingRooms.get(roomNumber).broadcastRoom(nickname + " 닉네임의 사용자가 채팅방을 나갔습니다.");
                    chattingRooms.get(roomNumber).exitRoom(msg);
                }

                if (msg.indexOf("/create") == 0) {
                    chattingRooms.add(roomNumber, new ChattingRoom(msg));
                    chattingRooms.get(roomNumber).joinRoom(roomNumber);
                    inRoom = 1;

                } else if (msg.indexOf("/list") == 0) {
                    out.println("0번 부터 " + (chattingRooms.size() - 1) + "번 까지의 방이 있습니다.");

                } else if (msg.indexOf("/join") == 0) {
                    if (getRoom(msg) == -1) {
                        out.println("잘못된 명령어 입니다.");
                    } else {
                        roomNumber = getRoom(msg);
                        chattingRooms.get(roomNumber).joinRoom(roomNumber);
                    }
                    inRoom = 1;
                } else if (msg.indexOf("/whisper") == 0) {
                    whisper(msg);
                } else if (inRoom == -1) {
                    broadcast("(로비) " + nickname + " : " + msg);
                } else if (inRoom == 1) {
                    chattingRooms.get(roomNumber).broadcastRoom("(" + roomNumber + "번 채팅룸) " + nickname + " : " + msg);
                }
            }
        } catch (
                IOException e) {
            System.out.println(e);
        } finally {
            synchronized (chatClients) {
                chatClients.remove(nickname);
            }
            broadcast(nickname + " 닉네임의 사용자가 연결을 끊었습니다.");
            System.out.println(nickname + " 닉네임의 사용자가 연결을 끊었습니다.");

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


    public int getRoom(String msg) {
        try {
            int firstSpaceIndex = msg.indexOf(" ");
            if (firstSpaceIndex == -1)
                return -1;

            int testNum = Integer.parseInt(msg.substring(firstSpaceIndex + 1).trim());
            roomNumber = testNum;
            return roomNumber;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }


    class ChattingRoom {
        private int roomNumber;
        private Map<String, PrintWriter> roomClients;
        private String nickName;

        public ChattingRoom(String msg) {
            this.nickName = nickname;

            if (roomNumber != 0) {
                roomNumber = 1;
                while (chattingRooms.size() < roomNumber + 1) {
                    roomNumber++;
                }
            }
            roomClients = new HashMap<>();


            out.println("채팅 룸 " + roomNumber + " 번을 생성하고 입장하셨습니다.");
            System.out.println(nickname + " 님이 " + roomNumber + "번 채팅룸을 생성했습니다.");
        }

        public void joinRoom(int roomNumber) {
            try {
                synchronized (roomClients) {
                    roomClients.put(nickname, chatClients.get(nickname));
                }
                roomClients.get(nickname).println(roomNumber + "번 방에 " + nickname + " 님이 입장하였습니다.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void exitRoom(String msg) {
            try {
                synchronized (roomClients) {
                    roomClients.remove(nickname, chatClients.get(nickname));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void broadcastRoom(String msg) {
            for (PrintWriter out : roomClients.values()) {
                out.println(msg);
            }

        }


    }


}

