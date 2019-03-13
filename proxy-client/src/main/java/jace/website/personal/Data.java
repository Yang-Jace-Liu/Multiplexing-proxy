package jace.website.personal;

import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

public class Data {
    private static Data instance = null;
    private int id = 0;

    public SocketChannel clientSocketChannel;
    public Set<SocketChannel> proxySocketChannels = new HashSet<>();

    private Data(){}

    public static Data getInstance(){
        if (instance == null) {
            instance = new Data();
        }
        return instance;
    }

    public int getNewId(){
        id += 1;
        return id - 1;
    }
}
