package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private Config config = null;
    private ServerSocket serverSocket;
    private List<Socket> remoteSockets;

    private static final Logger logger = LogManager.getLogger(Client.class);

    public Client(Config config) {
        this.config = config;
        remoteSockets = new ArrayList<>();
    }

    public void handshake() {
        Socket socket;
        try {
            socket = new Socket(config.getRemoteIp(), config.getRemotePort());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            String line = "Maximum connections: " + config.getNumberOfConnections();
            out.writeUTF(line);

            remoteSockets.add(socket);

            int numberOfNewConnections = in.readInt();
            for (int i = 0; i < numberOfNewConnections; i++) {
                remoteSockets.add(new Socket(config.getRemoteIp(), config.getRemotePort()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Start() {
        logger.debug("Client started on port: " + config.getRemotePort());
        try {
            serverSocket = new ServerSocket(config.getServerPort());
            handshake();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
