import java.io.IOException;
import java.net.*;

public class AltBitClient {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private boolean bit;
    private boolean transferComplete;

    AltBitClient(InetAddress serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.bit = false;
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
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
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
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        packet.setAddress(incoming.getAddress());
        packet.setPort(incoming.getPort());

        // Get first 4 bytes of payload. This will give us the ACK code (0 or 1).
        String bitChar = decode(incoming.getData()).substring(0, 4);

        // Check to see if packet is a FIN. If so we can stop.
        String command = decode(incoming.getData()).substring(4, 7);
        if(command.equals("FIN")) {
            this.transferComplete = true;
            return null;
        }

        if(bitChar.equals("ACK0")) {
            // If these do not match up something is out of sync. Resend packet.
            if(bit)
                return resend;
            // Flip bit and add payload.
            String payload = "ACK0|";
            packet.setData(payload.getBytes());
            packet.setLength(payload.getBytes().length);
            bit = true;
        }
        else {
            // If these do not match up something is out of sync. Resend packet.
            if(!bit)
                return resend;
            // Flip bit and add payload.
            String payload = "ACK1|";
            packet.setData(payload.getBytes());
            packet.setLength(payload.getBytes().length);
            bit = false;
        }
        return packet;
    }

    void run(String filename) {
        // Send initial request for file download.
        byte[] buffer = new byte[1024];
        DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
        String payload = "ACK0FILE" + filename + "|";
        requestPacket.setData(payload.getBytes());
        requestPacket.setLength(payload.getBytes().length);
        requestPacket.setAddress(serverAddress);
        requestPacket.setPort(serverPort);
        bit = true;
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