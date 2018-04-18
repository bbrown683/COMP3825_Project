import java.io.IOException;
import java.net.*;

public class SelRepClient {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private boolean transferComplete;

    SelRepClient(InetAddress serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.transferComplete = false;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void printPacketContents(DatagramPacket packet) {
        if(packet != null) {
            System.out.println("====================================");
            System.out.println("Source: " + packet.getAddress());
            System.out.println("Port:" + packet.getPort());
            System.out.println("Time:" + System.currentTimeMillis());
            System.out.println("Payload: " + decode(packet.getData()));
            System.out.println("====================================");
        }
    }

    private DatagramPacket receive(DatagramPacket old) {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return packet;
    }

    private String decode(byte[] data) {
        return new String(data, 0, data.length);
    }

    private void send(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private DatagramPacket interpret(DatagramPacket incoming, DatagramPacket resend) {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        packet.setAddress(incoming.getAddress());
        packet.setPort(incoming.getPort());
        return packet;
    }

    void run(String filename) {
        // Send initial request for file download.
        DatagramPacket requestPacket = new DatagramPacket(new byte[1024], 1024);
        String payload = "ACK0FILE" + filename + "|";
        requestPacket.setData(payload.getBytes());
        requestPacket.setLength(payload.getBytes().length);
        requestPacket.setAddress(serverAddress);
        requestPacket.setPort(serverPort);
        printPacketContents(requestPacket);
        send(requestPacket);

        // Set the resend packet to the original packet.
        DatagramPacket resendPacket = requestPacket;
        while(true) {
            DatagramPacket incomingPacket = receive(resendPacket);
            printPacketContents(incomingPacket);
            DatagramPacket outgoingPacket = interpret(incomingPacket, resendPacket);
            printPacketContents(outgoingPacket);
            if(transferComplete)
                break;
            send(outgoingPacket);
            // Update resend packet each time we send something.
            resendPacket = outgoingPacket;
        }
    }
}