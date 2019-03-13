package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    private int port; // proxy server port
    private int servicePort;
    private int numberOfConnections; // maximum number of connections
    private ServerSocketChannel serverSocketChannel;

    private List<Task> tasks = new ArrayList<>();

    private ByteBuffer buffer = ByteBuffer.allocate(10240);

    public Server(Config config) {
        this.port = config.getPort();
        this.servicePort = config.getServicePort();
        this.numberOfConnections = config.getNumberOfConnections();
    }

    public void start() {
        try {
            Selector selector = Selector.open();

            // Create proxy server
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // Connect to service
            SocketChannel serviceSocketChannel = SocketChannel.open();
            serviceSocketChannel.configureBlocking(false);
            serviceSocketChannel.connect(new InetSocketAddress("localhost", servicePort));
            serviceSocketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            Data.getInstance().serviceSocketChannel = serviceSocketChannel;

            while (true) {
                if (selector.select() > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isConnectable()) processConnection(key);
                        if (key.isReadable()) processRead(key);
                        if (key.isWritable()) processWrite(key);
                        if (key.isAcceptable()) processAccept(key, selector);
                    }
                }
                Thread.sleep(10);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel sc = serverSocketChannel.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        logger.debug("Connection accepted: " + sc.getRemoteAddress());
    }

    private void processRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        if (sc.read(buffer) <= 0) {
            sc.close();
            Data.getInstance().clientSocketChannels.remove(sc);
        }

        int target;
        if (sc == Data.getInstance().serviceSocketChannel) target = Config.Target.CLIENT;
        else target = Config.Target.SERVICE;

        addTask(new Task(target, buffer.array().clone()));
    }

    private void processWrite(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        for (Task task : this.tasks) {
            if (task.getTarget() == sc) {
                sc.write(ByteBuffer.wrap(task.getPayload()));
            }
        }
    }

    private void processConnection(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        while (sc.isConnectionPending()) {
            try {
                sc.finishConnect();
                logger.debug("Established a connection to " + sc.getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
                logger.debug("Failed to establish a connection");
            }
        }
    }

    private void addTask(Task task) {
        this.tasks.add(task);
    }
}
