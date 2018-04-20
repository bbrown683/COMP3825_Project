class Server {
    public static void main(String[] args) {
        SelRepServer server = new SelRepServer(5000, 4);
        server.run();
    }
}