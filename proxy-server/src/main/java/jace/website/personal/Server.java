package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private int port;
    private int numberOfConnections;
    private static final Logger logger = LogManager.getLogger(Server.class);

    public Server (Config config) {
        this.port = config.getPort();
        this.numberOfConnections = config.getNumberOfConnections();
    }

    public void Start() {
        logger.debug("Server started on port: " + port);

        try{
            ServerSocket server = new ServerSocket(port);

            Socket socket = server.accept();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String req = in.readUTF();
            out.writeUTF("OK");

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
