package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private int port;
    private int numberOfConnections;
    private static final Logger logger = LogManager.getLogger(Server.class);
    private ServerSocket serverSocket;
    private Map<Integer, Socket> sockets;

    public Server (Config config) {
        this.port = config.getPort();
        this.numberOfConnections = config.getNumberOfConnections();

        sockets = new HashMap<>();
    }

    public void handshake(Socket socket) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String hello = in.readUTF();
            if (!hello.startsWith("Maximum connections: ")) {
                socket.close();
                return;
            }
            int remoteNumberOfConnections = Integer.valueOf(hello.split(":")[1].replace(" ", ""));
            int resultNumberOfConnections = remoteNumberOfConnections > numberOfConnections ? numberOfConnections : remoteNumberOfConnections;
            out.writeInt(resultNumberOfConnections);
            int id = Data.getInstance().getNewId();
            sockets.put(id, socket);
            logger.debug("A client connected to server through port " + socket.getPort());

            for (int i = 1; i < resultNumberOfConnections; i++) {
                Socket anotherSocket = serverSocket.accept();
                logger.debug("A client connected to server through port " + socket.getPort());
                sockets.put(id, anotherSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        logger.debug("Server started on port: " + port);

        try{
            serverSocket = new ServerSocket(port);

            Socket socket = serverSocket.accept();
            handshake(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
