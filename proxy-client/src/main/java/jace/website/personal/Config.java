package jace.website.personal;

public class Config {
    private int remotePort = 2000;
    private int serverPort = 3000;
    private int numberOfConnections = 2;
    private String remoteIp = "127.0.0.1";

    public Config() {}

    public Config(int remotePort) {
        this.remotePort = remotePort;
    }

    public Config(int remotePort, int numberOfConnections) {
        this.remotePort = remotePort;
        this.numberOfConnections = numberOfConnections;
    }

    public int getNumberOfConnections() {
        return numberOfConnections;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setNumberOfConnections(int numberOfConnections) {
        this.numberOfConnections = numberOfConnections;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }
}
