package leader;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class MulticastPublisher {
    private final String group;
    private final int port;

    public MulticastPublisher(String group, int port) {
        this.group = group;
        this.port = port;
    }

    public void publish(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, InetAddress.getByName(group), port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("[Multicast] Erro envio: " + e.getMessage());
        }
    }
}