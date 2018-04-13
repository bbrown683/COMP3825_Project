import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AltBitServer {
    private DatagramSocket socket;
    private int port;

    AltBitServer(int port) {
        this.port = port;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private String fileToString(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String();
    }

    private void printPacketContents(DatagramPacket packet) {
        // Not formatted as of yet.
        if(packet != null) {
            System.out.println("====================================");
            System.out.println("Source: " + packet.getAddress());
            System.out.println("Payload: " + packet.getData());
            System.out.println("====================================");
        }
        else
            System.out.println("Uninitialized Packet!");
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
        while(true) {
            DatagramPacket packet = receive();
            printPacketContents(packet);
        }
    }
}
