package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Client {
    private Config config = null;
    private ByteBuffer buffer = ByteBuffer.allocate(10240);
    private List<Task> tasks = new ArrayList<>();
    private ServerSocketChannel serverSocketChannel;

    private static final Logger logger = LogManager.getLogger(Client.class);

    public Client(Config config) {
        this.config = config;
    }


    public void Start() {
        logger.debug("Trying to connect to remote port: " + config.getRemotePort());
        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("localhost"), this.config.getRemotePort());
            Selector selector = Selector.open();

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", config.getServerPort()));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            for (int i = 0; i < config.getNumberOfConnections(); i++) {
                SocketChannel sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.connect(address);
                sc.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                Data.getInstance().proxySocketChannels.add(sc);
            }

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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel sc = serverSocketChannel.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        logger.debug("Connection accepted: " + sc.getRemoteAddress());

        if (Data.getInstance().clientSocketChannel != null) {
            Data.getInstance().clientSocketChannel.close();
            Data.getInstance().clientSocketChannel = sc;
        }
    }

    private void processConnection(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            while (sc.isConnectionPending()) {
                sc.finishConnect();
                logger.debug("Connected to remote");
            }
        } catch (IOException e) {
            key.cancel();
            e.printStackTrace();
        }
    }

    private void processRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        int length = sc.read(buffer);
        if (length <= 0) {
            logger.debug("Remote closed the connection");
            sc.close();
            return;
        }
        if (sc == Data.getInstance().clientSocketChannel) tasks.add(new Task(Config.Target.PROXY, buffer.array().clone()));
        else tasks.add(new Task(Config.Target.CLIENT, buffer.array().clone()));
    }

    private void processWrite(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        for (Task task : tasks) {
            if (task.getTarget() == sc) {
                sc.write(ByteBuffer.wrap(task.getPayload()));
            }
        }
    }
}
