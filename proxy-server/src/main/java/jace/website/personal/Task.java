package jace.website.personal;

import java.nio.channels.SocketChannel;
import java.util.Random;

public class Task {
    private int target;
    private byte[] payload;
    private Random random = new Random();

    public Task(int target, byte[] payload) {
        this.target = target;
        this.payload = payload;
    }

    public Task(int target, String payload) {
        this(target, payload.getBytes());
    }

    public SocketChannel getTarget() {
        return schedule(target);
    }

    public byte[] getPayload() {
        return payload;
    }

    private SocketChannel schedule(int target) {
        if (target == Config.Target.SERVICE) return Data.getInstance().serviceSocketChannel;
        else {
            // random
            int size = Data.getInstance().clientSocketChannels.size();
            int ind = random.nextInt(size);
            return (SocketChannel) Data.getInstance().clientSocketChannels.toArray()[0];
        }
    }
}
