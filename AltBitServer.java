import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class AltBitServer {
    private DatagramSocket socket;
    private boolean bit;
    private LinkedList<byte[]> payloads;

    AltBitServer(int port) {
        this.bit = false;
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
            byte[] buffer = new byte[1000];
            int offset = 0;
            do {
                int pos = is.read(buffer, 0, 1000);
                offset += pos;
                // Add to stack and reset for next run.
                payloads.offer(buffer);
                buffer  = new byte[1000];
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
        return new String(data, 0, data.length).trim();
    }

    private DatagramPacket receive() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
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

    private DatagramPacket interpret(DatagramPacket incoming, DatagramPacket resend) {
                // Packet Format
        // Byte 0-8: Command   
        //      FIN - File transfer complete.
        //      ACK - Requires Acknowledgement.
        // Byte 8-24: Bit
        // Byte 24-1000: Payload
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        packet.setAddress(incoming.getAddress());
        packet.setPort(incoming.getPort());

        String decoded = decode(incoming.getData());
        
        // Check for a FILE command.
        String command = decoded.substring(0, 8).trim();
        if(command.contains("FILE"))
            fileToChunks(decoded.substring(24, decoded.length()));
        String packetData = "";
        // Check to see if there is any data to submit.
        if(payloads.isEmpty())
            packetData = constructPacketData("FIN", null); 
        if(command.contains("ACK")) {
            boolean ackBit = Integer.parseInt(decoded.substring(8, 9).trim()) != 0;
            if(ackBit != bit) {
                return resend;
            }
            packetData = constructPacketData("ACK", decode(payloads.poll()));   
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

    void run() {
        DatagramPacket resend = new DatagramPacket(new byte[1024], 1024);
        while(true) {
            DatagramPacket incoming = receive();
            printPacketContents(incoming);
            DatagramPacket outgoing = interpret(incoming, resend);
            printPacketContents(outgoing);
            send(outgoing);
            resend = outgoing;
        }
    }
}
