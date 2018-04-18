import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class SelRepServer {
    private DatagramSocket socket;
    private LinkedList<byte[]> payloads;

    SelRepServer(int port) {
        this.payloads = new LinkedList<>();
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void fileToChunks(String filename) {
        try {
            // Adds a file into a series of chunks with a size
            // of 1024 bytes (the size of our socket buffer.
            File file = new File(filename);
            long length = file.length();
            InputStream is = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int offset = 0;
            do {
                int pos = is.read(buffer, 0, 1024);
                offset += pos;
                // Add to stack and reset for next run.
                payloads.offer(buffer);
                buffer  = new byte[1024];
            } while(offset < length);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printPacketContents(DatagramPacket packet) {
        if(!packet.getData().toString().isEmpty()) {
            System.out.println("====================================");
            System.out.println("Source: " + packet.getAddress());
            System.out.println("Port:" + packet.getPort());
            System.out.println("Payload: " + decode(packet.getData()));
            System.out.println("====================================");
        }
    }

    private String decode(byte[] data) {
        return new String(data, 0, data.length);
    }

    private DatagramPacket receive() {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            // Resend packet if it times out.
            send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packet;
    }

    private void send(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private DatagramPacket interpret(DatagramPacket incoming, DatagramPacket resendPacket) {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        packet.setAddress(incoming.getAddress());
        packet.setPort(incoming.getPort());
        return packet;
    }

    void run() {
        DatagramPacket resendPacket = new DatagramPacket(new byte[1024], 1024);
        while(true) {
            DatagramPacket incomingPacket = receive();
            printPacketContents(incomingPacket);
            DatagramPacket outgoingPacket = interpret(incomingPacket, resendPacket);
            printPacketContents(outgoingPacket);
            send(outgoingPacket);
            resendPacket = outgoingPacket;
        }
    }
}
