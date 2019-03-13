package jace.website.personal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Client {
    private Config config = null;
    private List<SocketChannel> remoteSocketChannels;
    private ByteBuffer buffer = ByteBuffer.allocate(10240);

    private static final Logger logger = LogManager.getLogger(Client.class);

    public Client(Config config) {
        this.config = config;
        remoteSocketChannels = new ArrayList<>();
    }


    public void Start() {
        logger.debug("Trying to connect to remote port: " + config.getRemotePort());
        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("localhost"), this.config.getRemotePort());
            Selector selector = Selector.open();

            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(address);
            sc.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            while (true) {
                if (selector.select() > 0) {
                    processReadySet(selector.selectedKeys());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processReadySet(Set<SelectionKey> readySet) {
        Iterator<SelectionKey> iterator = readySet.iterator();

        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();

            if (key.isConnectable()) processConnection(key);
            if (key.isReadable()) processRead(key);
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

    private void processRead(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            int length = sc.read(buffer);
            if (length <= 0){
                logger.debug("Remote closed the connection");
                sc.close();
                return;
            }
            String result = new String(buffer.array()).trim();
            System.out.println("Message received from Server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processHandshake(SelectionKey key) {

    }
}
