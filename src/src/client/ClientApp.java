package client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClientApp {
    public static void main(String[] args) throws IOException {
        if (args.length < 6) {
            System.out.println("Uso: ClientApp <mcastGroup> <mcastPort> <authHost> <authPort> <user> <pass>");
            return;
        }
        String group = args[0];
        int port = Integer.parseInt(args[1]);
        String authHost = args[2];
        int authPort = Integer.parseInt(args[3]);
        String user = args[4];
        String pass = args[5];

        // 1) Autenticar e obter token
        String token = authenticate(authHost, authPort, user, pass);
        if (token == null) {
            System.err.println("[Client] Falha ao autenticar.");
            return;
        }
        System.out.println("[Client] Autenticado. Token: " + token);

        // 2) Escutar multicast e filtrar por token
        try (MulticastSocket socket = new MulticastSocket(port)) {
            socket.joinGroup(InetAddress.getByName(group));
            System.out.println("[Client] Aguardando mensagens em " + group + ":" + port + " (token-protected)");

            byte[] buf = new byte[8192];
            String prefix = "[TOKEN:" + token + "] ";
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (msg.startsWith(prefix)) {
                    String payload = msg.substring(prefix.length());
                    System.out.println("[Client] Recebido: " + payload);
                } else {
                    // Ignora mensagens destinadas a outros tokens
                }
            }
        }
    }

    private static String authenticate(String host, int port, String user, String pass) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 2000);
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                out.println("LOGIN " + user + " " + pass);
                String reply = in.readLine();
                if (reply != null && reply.startsWith("TOKEN ")) {
                    return reply.substring("TOKEN ".length()).trim();
                }
            }
        } catch (IOException e) {
            System.err.println("[Client] Erro de autenticação: " + e.getMessage());
        }
        return null;
    }
}