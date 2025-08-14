package leader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

/*
 Protocolo de linha simples:
 *    - LOGIN <user> <pass>   -> responde: TOKEN <token>\n  ou ERR\n
 *    - LOGOUT <token>        -> responde: OK\n ou ERR\n
 *    - PING                  -> responde: PONG\n
 */
public class AuthTcpServer implements Runnable {
    private final int port;
    private final AuthService auth;
    private volatile boolean running = true;

    public AuthTcpServer(int port, AuthService auth) {
        this.port = port;
        this.auth = auth;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[Auth] Server escutando na porta " + port);
            while (running) {
                Socket s = server.accept();
                new Thread(() -> handleClient(s), "auth-client").start();
            }
        } catch (IOException e) {
            System.err.println("[Auth] Erro no servidor: " + e.getMessage());
        }
    }

    private void handleClient(Socket s) {
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true)) {

            String line = in.readLine();
            if (line == null) {
                out.println("ERR");
                return;
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0) { out.println("ERR"); return; }

            String cmd = parts[0].toUpperCase(Locale.ROOT);

            switch (cmd) {
                case "LOGIN": {
                    if (parts.length < 3) { out.println("ERR"); break; }
                    String user = parts[1];
                    String pass = parts[2];
                    String token = auth.login(user, pass);
                    if (token != null) out.println("TOKEN " + token);
                    else out.println("ERR");
                    break;
                }
                case "LOGOUT": {
                    if (parts.length < 2) { out.println("ERR"); break; }
                    boolean ok = auth.logout(parts[1]);
                    out.println(ok ? "OK" : "ERR");
                    break;
                }
                case "PING": {
                    out.println("PONG");
                    break;
                }
                default:
                    out.println("ERR");
            }

        } catch (IOException ignored) {}
    }
}