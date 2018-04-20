import java.net.InetAddress;

class Client {
    public static void main(String[] args) {
        SelRepClient client = new SelRepClient(InetAddress.getLoopbackAddress(), 5000, 4);
        client.run("mobydick.txt");
    }
}