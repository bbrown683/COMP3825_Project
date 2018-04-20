import java.io.IOException;
import java.net.*;

public class SelRepClient {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private int windowSize;
    private int sequenceNumber;
    private boolean transferComplete;

    SelRepClient(InetAddress serverAddress, int serverPort, int windowSize) {
        this.windowSize =  windowSize;
        this.sequenceNumber = 0;
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

    private DatagramPacket interpret(DatagramPacket incoming) {
        // Packet Format
        // Byte 0-8: Command   
        //      FILE - File Request
        //      ACK - Requires Acknowledgement.
        // Byte 8-24: Sequence Number
        // Byte 10-1000: Payload
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        packet.setAddress(incoming.getAddress());
        packet.setPort(incoming.getPort());

        String decoded = decode(incoming.getData());
        
        String command = decoded.substring(0, 8).trim();
        if(command.contains("FIN")) {
            transferComplete = true;
            return null;
        }

        int seqNumber = Integer.parseInt(decoded.substring(8, 24).trim());

        String packetData = constructPacketData("ACK", null);
        packet.setData(packetData.getBytes());
        packet.setLength(packetData.getBytes().length);
        return packet;
    }

    private String constructPacketData(String command, String payload) {
        StringBuilder builder = new StringBuilder();
        builder.append(command);
        for(int i = builder.toString().length(); i < 8; i++)
            builder.append(" ");
        builder.append(sequenceNumber);
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
        DatagramPacket request = requestFilePacket(filename);
        printPacketContents(request);
        send(request);        
        sequenceNumber++;
        
        while(true) {
            DatagramPacket incoming = receive();
            printPacketContents(incoming);
            DatagramPacket outgoing = interpret(incoming);
            printPacketContents(outgoing);
            if(transferComplete)
                break;
            send(outgoing);
            sequenceNumber++;
        }
    }
}