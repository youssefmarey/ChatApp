import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MultiClientServer {
    private static final int PORT = 1236;
    private static Map<String, PrintWriter> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws IOException {
        System.out.println("✅ Server started on port " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            new ClientHandler(serverSocket.accept()).start();
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private String name;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("ENTER_NAME");
                name = in.readLine();
                synchronized (clients) {
                    while (clients.containsKey(name)) {
                        out.println("NAME_EXISTS");
                        name = in.readLine();
                    }
                    clients.put(name, out);
                }

                broadcastUserList();
                broadcastMessage("[🔔] " + name + " has joined the chat.");
                sendOnlineCount();

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.equalsIgnoreCase("/quit")) {
                        break;
                    }
                    if (msg.startsWith("/pm ")) {
                        String[] parts = msg.split(" ", 3);
                        if (parts.length >= 3) {
                            String target = parts[1];
                            String message = parts[2];
                            PrintWriter pw = clients.get(target);
                            if (pw != null) {
                                String time = timestamp();
                                pw.println("[PM from " + name + "] " + time + ": " + message);
                                out.println("[PM to " + target + "] " + time + ": " + message);
                            } else {
                                out.println("[Server]: User not found.");
                            }
                        }
                    } else {
                        String time = timestamp();
                        broadcastMessage(name + " " + time + ": " + msg);
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Error: " + e.getMessage());
            } finally {
                if (name != null) {
                    clients.remove(name);
                    broadcastMessage("[❌] " + name + " has left the chat.");
                    broadcastUserList();
                    sendOnlineCount();
                }
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void broadcastMessage(String message) {
            for (PrintWriter writer : clients.values()) {
                writer.println(message);
            }
        }

        private void broadcastUserList() {
            StringBuilder sb = new StringBuilder("USERS:");
            for (String user : clients.keySet()) {
                sb.append(user).append(",");
            }
            for (PrintWriter writer : clients.values()) {
                writer.println(sb.toString());
            }
        }

        private void sendOnlineCount() {
            String count = "ONLINE_COUNT:" + clients.size();
            for (PrintWriter writer : clients.values()) {
                writer.println(count);
            }
        }

        private String timestamp() {
            return "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]";
        }
    }
}