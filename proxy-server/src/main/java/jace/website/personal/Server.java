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
    private Selector selector;

    private List<Task> tasks = new ArrayList<>();

    private ByteBuffer buffer = ByteBuffer.allocate(10240);

    public Server(Config config) {
        this.port = config.getPort();
        this.servicePort = config.getServicePort();
        this.numberOfConnections = config.getNumberOfConnections();
    }

    public void start() {
        try {
            selector = Selector.open();

            // Create proxy server
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

//            // Connect to service
            SocketChannel serviceSocketChannel = SocketChannel.open();
            serviceSocketChannel.configureBlocking(false);
            serviceSocketChannel.connect(new InetSocketAddress("www.calgary.ca", servicePort));
            serviceSocketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            serviceSocketChannel.socket().setKeepAlive(true);
            Data.getInstance().serviceSocketChannel = serviceSocketChannel;

            while (true) {
                if (selector.select() > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isConnectable()) processConnection(key);
                        if (key.isWritable()) processWrite(key);
                        if (key.isAcceptable()) processAccept(key);
                        if (key.isReadable()) processRead(key);
                    }
                }
                Thread.sleep(10);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel sc = serverSocketChannel.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        Data.getInstance().clientSocketChannels.add(sc);
        logger.debug("Connection accepted: " + sc.getRemoteAddress());
    }

    private void processRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        if (sc.read(buffer) < 0) {
            Data.getInstance().clientSocketChannels.remove(sc);
            logger.debug("Connection disconnected from " + sc.getRemoteAddress());
            sc.close();
        }

        System.out.println(new String(buffer.array()));

        if (sc == Data.getInstance().serviceSocketChannel){
            this.tasks.add(new Task(Config.Target.CLIENT, buffer.array().clone()));
        }
        else {
            this.tasks.add(new Task(Config.Target.SERVICE, buffer.array().clone()));
        }

    }

    private void processWrite(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        Iterator<Task> iterator = tasks.iterator();
        while(iterator.hasNext()){
            Task task = iterator.next();
            if (task.getTarget() == sc) {
                iterator.remove();
                System.out.println("write!!!");
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
}
