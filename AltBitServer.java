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

    private DatagramPacket interpret(DatagramPacket incoming, DatagramPacket resendPacket) {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        packet.setAddress(incoming.getAddress());
        packet.setPort(incoming.getPort());

        // Get first byte of payload. This will give us the ACK code (0 or 1).
        String bitChar = decode(incoming.getData()).substring(0, 4);

        // Get next 4 bytes to see if this is a file request.
        String command = decode(incoming.getData()).substring(4, 8);
        System.out.println(command);
        if(command.equals("FILE")) {
            // I am using the pipe ("|") character to signify the end of data in a buffer since Java
            // does not use whitespace.
            String payload = decode(incoming.getData());
            String cleaned = payload.substring(8, payload.indexOf("|"));
            fileToChunks(cleaned);
        }

        if(bitChar.equals("ACK0")) {
            // Does not match what came in.
            // Resend packet.
            if(bit)
                return resendPacket;
            String payload = "ACK1";
            if(payloads.isEmpty())
                payload += "FIN";
            else
                payload += decode(payloads.poll());
            payload += "|";
            bit = true;
            packet.setData(payload.getBytes());
            packet.setLength(payload.getBytes().length);
        }
        else {
            // Does not match what came in.
            // Resend packet.
            if(!bit)
                return resendPacket;
            String payload = "ACK0";
            if(payloads.isEmpty())
                payload += "FIN";
            else
                payload += decode(payloads.poll());
            // Signifies end of data in the buffer.
            payload += "|";
            // Flip bit.
            bit = false;
            // Send data.
            packet.setData(payload.getBytes());
            packet.setLength(payload.getBytes().length);
        }
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
