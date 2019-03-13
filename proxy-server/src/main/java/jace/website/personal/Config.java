package jace.website.personal;

public class Config {
    private int port = 2000;
    private int numberOfConnections = 2;
    private int servicePort = 80;

    public Config() {}

    public Config(int port) {
        this.port = port;
    }

    public Config(int port, int numberOfConnections) {
        this.port = port;
        this.numberOfConnections = numberOfConnections;
    }

    public int getNumberOfConnections() {
        return numberOfConnections;
    }

    public int getPort() {
        return port;
    }

    public int getServicePort() {
        return servicePort;
    }

    public void setNumberOfConnections(int numberOfConnections) {
        this.numberOfConnections = numberOfConnections;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    public class Target{
        public static final int SERVICE = 1;
        public static final int CLIENT = 2;
    }
}
