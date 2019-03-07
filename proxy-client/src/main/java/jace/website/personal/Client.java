package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
    private Config config = null;
    private ServerSocket serverSocket;
    private ArrayList<Socket> remoteSockets;

    private static final Logger logger = LogManager.getLogger(Client.class);

    public Client(Config config) {
        this.config = config;
    }

    public void Start() {
        logger.debug("Client started on port: " + config.getRemotePort());
        try {
            serverSocket = new ServerSocket(config.getServerPort());
            Socket socket = new Socket(config.getRemoteIp(), config.getRemotePort());

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            String line = "Maximum connections: " + config.getNumberOfConnections();
            out.writeUTF(line);

            String ports = in.readUTF();

            in.close();
            out.close();
            socket.close();

            //TODO: manipulate ports
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
