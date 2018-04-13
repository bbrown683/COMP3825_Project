import java.net.InetAddress;

class Client {
    public static void main(String[] args) {
        AltBitClient client = new AltBitClient(InetAddress.getLoopbackAddress(), 5000);
        client.run("mobydick.txt");
    }
}