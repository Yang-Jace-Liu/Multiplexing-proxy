package jace.website.personal;

public class Config {
    private int port = 2000;
    private int numberOfConnections = 2;

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

    public void setNumberOfConnections(int numberOfConnections) {
        this.numberOfConnections = numberOfConnections;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
