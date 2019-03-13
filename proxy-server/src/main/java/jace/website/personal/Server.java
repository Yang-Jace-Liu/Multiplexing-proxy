package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Server {
    private int port;
    private int numberOfConnections;
    private static final Logger logger = LogManager.getLogger(Server.class);
    private ServerSocketChannel serverSocketChannel;
    private Map<String, Integer> ip2id;

    public Server(Config config) {
        this.port = config.getPort();
        this.numberOfConnections = config.getNumberOfConnections();

        ip2id = new HashMap<>();
    }

    public void start() {
        logger.debug("Server started on port: " + port);

        try {
            Selector selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            SelectionKey key = null;
            while (true) {
                if (selector.select() <= 0) continue;
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        SocketChannel sc = serverSocketChannel.accept();
                        sc.configureBlocking(false);
                        sc.register(selector, SelectionKey.OP_READ);

                        String remoteIp = sc.getRemoteAddress().toString().split(":")[0].replace("/", "");
                        if (!ip2id.containsKey(remoteIp))
                            ip2id.put(remoteIp, Data.getInstance().getNewId());
                        logger.debug("Connection Accepted: " + sc.getRemoteAddress());
                    }

                    if (key.isReadable()) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(10240);
                        int length = sc.read(buffer);
                        if (length <= 0) {
                            logger.debug("Connection Closed: " + sc.getRemoteAddress());
                            sc.close();
                        } else {
                            String result = new String(buffer.array()).trim();
                            System.out.println("Message received: " + result + " length: " + result.length());
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
