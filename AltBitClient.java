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

    private DatagramPacket receive() {
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
        return new String(data, 0, data.length).trim();
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

        String decoded = decode(incoming.getData());
        
        // Check for a FILE command.
        String command = decoded.substring(0, 8).trim();
        if(command.contains("FIN")) {
            transferComplete = true;
            return null;
        }

        String packetData =  "";
        if(command.contains("ACK")) {
            boolean ackBit = Integer.parseInt(decoded.substring(8, 9).trim()) != 0;
            if(ackBit != bit) {
                return resend;
            }
            packetData = constructPacketData("ACK", null);   
            bit = !bit;      
        }

        packet.setData(packetData.getBytes());
        packet.setLength(packetData.getBytes().length);
        return packet;
    }

    private String constructPacketData(String command, String payload) {
        StringBuilder builder = new StringBuilder();
        builder.append(command);
        for(int i = builder.toString().length(); i < 8; i++)
            builder.append(" ");
        builder.append(bit);
        if(payload != null) {
            for(int i = builder.toString().length(); i < 24; i++)
                builder.append(" ");
            builder.append(payload);
        }
        return builder.toString();
    }

        private DatagramPacket requestFilePacket(String filename) {
        // Send initial request for file download.
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        String data = constructPacketData("FILE", filename);
        packet.setAddress(serverAddress);
        packet.setPort(serverPort);
        packet.setData(data.getBytes());
        packet.setLength(data.getBytes().length);
        return packet;
    }

    void run(String filename) {
        // Send initial request for file download.
        DatagramPacket request = requestFilePacket(filename);
        printPacketContents(request);
        send(request);
        bit = !bit;

        // Set the resend packet to the original packet.
        DatagramPacket resend = request;
        while(true) {
            DatagramPacket incoming = receive();
            printPacketContents(incoming);
            DatagramPacket outgoing = interpret(incoming, resend);
            printPacketContents(outgoing);
            if(transferComplete)
                break;
            send(outgoing);
            // Update resend packet each time we send something.
            resend = outgoing;
        }
    }
}