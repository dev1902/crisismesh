import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * BACKEND: networking + mesh logic.
 * - UDP sockets on the local network (hotspot/Wi-Fi), no internet needed.
 * - Listener thread receives packets; HELLO thread announces this node.
 * - Mesh relay: every node rebroadcasts unseen messages with TTL-1.
 */
public class MeshNode {
    public interface Listener { void onMessage(String type, String sender, String text); void onPeersChanged(Collection<String> peers); }

    static final int PORT = 9876;
    private final String name;
    private final Listener listener;
    private DatagramSocket socket;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();          // dedupe
    private final Map<String, Long> peers = new ConcurrentHashMap<>();       // peer -> last seen
    private volatile boolean running = true;

    public MeshNode(String name, Listener l) { this.name = name; this.listener = l; }

    public void start() throws Exception {
        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.setBroadcast(true);
        socket.bind(new InetSocketAddress(PORT));
        new Thread(this::listen, "listener").start();
        new Thread(this::hello, "hello").start();
    }

    private void listen() {
        byte[] buf = new byte[2048];
        while (running) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                handle(new String(p.getData(), 0, p.getLength(), "UTF-8"));
            } catch (Exception e) { if (running) e.printStackTrace(); }
        }
    }

    // format: TYPE|id|ttl|sender|text
    private void handle(String raw) {
        String[] f = raw.split("\\|", 5);
        if (f.length < 5) return;
        String type = f[0], id = f[1], sender = f[3], text = f[4];
        int ttl = Integer.parseInt(f[2]);
        if (sender.equals(name)) return;                       // ignore own packets
        if (type.equals("HELLO")) {
            if (peers.put(sender, System.currentTimeMillis()) == null) listener.onPeersChanged(peers.keySet());
            return;
        }
        if (!seen.add(id)) return;                             // already handled
        listener.onMessage(type, sender, text);
        if (ttl > 1) send(type, id, ttl - 1, sender, text);    // RELAY = mesh behaviour
    }

    private void hello() {
        while (running) {
            send("HELLO", UUID.randomUUID().toString(), 1, name, "");
            peers.values().removeIf(t -> System.currentTimeMillis() - t > 10000);
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
        }
    }

    public void broadcast(String type, String text) {
        String id = UUID.randomUUID().toString();
        seen.add(id);
        send(type, id, 4, name, text);                         // TTL 4 hops
    }

    private void send(String type, String id, int ttl, String sender, String text) {
        try {
            byte[] d = (type + "|" + id + "|" + ttl + "|" + sender + "|" + text).getBytes("UTF-8");
            socket.send(new DatagramPacket(d, d.length, InetAddress.getByName("255.255.255.255"), PORT));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String getName() { return name; }
    public void stop() { running = false; socket.close(); }
}
