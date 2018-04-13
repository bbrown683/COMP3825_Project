import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class AltBitClient {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;

    AltBitClient(InetAddress serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void printPacketContents(DatagramPacket packet) {
        // Not formatted as of yet.
        System.out.println(packet.getData());
    }

    private DatagramPacket receive() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packet;
    }

    private void send(byte[] buffer, InetAddress address, int port) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(packet);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    void run() {
        send("FILE".getBytes(), serverAddress, serverPort);
    }
}