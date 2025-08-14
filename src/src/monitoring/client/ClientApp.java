package monitoring.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ClientApp {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Uso: ClientApp <mcastGroup> <mcastPort>");
            return;
        }
        String group = args[0];
        int port = Integer.parseInt(args[1]);
        try (MulticastSocket socket = new MulticastSocket(port)) {
            socket.joinGroup(InetAddress.getByName(group));
            System.out.println("[Client] Aguardando mensagens em " + group + ":" + port);
            byte[] buf = new byte[8192];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[Client] Recebido: " + msg);
            }
        }
    }
}