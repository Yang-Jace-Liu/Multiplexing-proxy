package jace.website.personal;

import java.nio.channels.SocketChannel;
import java.util.Random;

public class Task {
    private SocketChannel target;
    private byte[] payload;
    private Random random = new Random();

    public Task(int target, byte[] payload) {
        this.target = schedule(target);
        this.payload = payload;
    }

    public Task(int target, String payload) {
        this(target, payload.getBytes());
    }

    public SocketChannel getTarget() {
        return target;
    }

    public byte[] getPayload() {
        return payload;
    }

    private SocketChannel schedule(int target) {
        if (target == Config.Target.CLIENT) return Data.getInstance().clientSocketChannel;
        else {
            // random
            int size = Data.getInstance().proxySocketChannels.size();
            int ind = random.nextInt(size);
            return (SocketChannel) Data.getInstance().proxySocketChannels.toArray()[0];
        }
    }
}
