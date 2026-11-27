package br.com.wdc.cube.backend.cluster;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.After;
import org.junit.Test;

import io.javalin.Javalin;

/**
 * The instances' mesh, with real servers on free ports: they find each other from one list (each in it), tell each
 * other, refuse an instance with another key, and catch up a peer that comes back.
 */
public class ClusterMeshTest {

    /** Short retries and pings: a slow machine must not outlast the waits below. */
    private static final ClusterMesh.Timing FAST = new ClusterMesh.Timing(Duration.ofMillis(100), Duration.ofMillis(500),
            Duration.ofMillis(500));

    private static final class Node {

        final ClusterMesh mesh;
        final Javalin server;
        final List<String> received = new CopyOnWriteArrayList<>();
        final AtomicInteger missed = new AtomicInteger();

        Node(ClusterMesh mesh, Javalin server) {
            this.mesh = mesh;
            this.server = server;
            mesh.on("test", received::add);
            mesh.onMissed(missed::incrementAndGet);
        }

        String address() {
            return "ws://127.0.0.1:" + server.port();
        }

        void stop() {
            mesh.close();
            server.stop();
        }
    }

    private final List<Node> nodes = new ArrayList<>();

    @After
    public void stopAll() {
        nodes.forEach(Node::stop);
    }

    private Node node(byte[] key, int port) {
        var mesh = new ClusterMesh(key, UUID.randomUUID().toString(), FAST);
        var server = Javalin.create(mesh::configure)
                .start("127.0.0.1", port);
        var node = new Node(mesh, server);
        nodes.add(node);
        return node;
    }

    private Node node() {
        return node(ClusterKey.from("secret"), 0);
    }

    private static void waitFor(String what, BooleanSupplier condition) throws InterruptedException {
        var until = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < until) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        fail("timed out waiting: " + what);
    }

    @Test
    public void twoInstances_fromOneList_tellEachOther() throws InterruptedException {
        var a = node();
        var b = node();
        var peers = List.of(a.address(), b.address());
        a.mesh.start(peers);
        b.mesh.start(peers);

        waitFor("both connected", () -> a.mesh.readyLinks() == 1 && b.mesh.readyLinks() == 1);
        assertEquals("only the other one, not itself", Set.of(b.mesh.nodeId()), a.mesh.connectedPeers());
        assertEquals(Set.of(a.mesh.nodeId()), b.mesh.connectedPeers());

        a.mesh.publish("test", "from-a");
        b.mesh.publish("test", "from-b");
        b.mesh.publish("other", "not-listened");

        waitFor("both received", () -> b.received.contains("from-a") && a.received.contains("from-b"));
        assertEquals(List.of("from-a"), b.received);
        assertEquals("nobody hears its own messages", List.of("from-b"), a.received);
    }

    @Test
    public void anInstanceWithAnotherKey_isRefused_bothWays() throws InterruptedException {
        var a = node();
        var stranger = node(ClusterKey.from("another secret"), 0);
        a.mesh.start(List.of(stranger.address()));
        stranger.mesh.start(List.of(a.address()));

        Thread.sleep(1500);
        assertEquals(0, a.mesh.readyLinks());
        assertEquals(0, stranger.mesh.readyLinks());
        assertEquals(Set.of(), a.mesh.connectedPeers());
        assertEquals(Set.of(), stranger.mesh.connectedPeers());

        stranger.mesh.publish("test", "intruder");
        Thread.sleep(300);
        assertEquals(List.of(), a.received);
    }

    @Test
    public void aPeerThatComesBack_isConnectedAgain_andTakesItAsMissed() throws InterruptedException {
        var a = node();
        var b = node();
        var port = b.server.port();
        a.mesh.start(List.of(b.address()));
        waitFor("a connected to b", () -> a.mesh.readyLinks() == 1);
        assertEquals("a new connection: b may have missed what a sent before", 1, b.missed.get());

        b.stop();
        nodes.remove(b);
        waitFor("a saw b go", () -> a.mesh.readyLinks() == 0);
        a.mesh.publish("test", "while-b-was-down");

        var b2 = node(ClusterKey.from("secret"), port);
        waitFor("a connected to b again", () -> a.mesh.readyLinks() == 1);
        assertEquals(1, b2.missed.get());

        a.mesh.publish("test", "after");
        waitFor("b received", () -> b2.received.contains("after"));
        assertEquals("what was published while it was down waited, in order", List.of("while-b-was-down", "after"),
                b2.received);
    }

    @Test
    public void aPeerAddress_isAWebSocket() {
        assertEquals("ws://store-b:8080/cluster", ClusterMesh.peerUri("ws://store-b:8080").toString());
        assertEquals("wss://store-b/cluster", ClusterMesh.peerUri(" wss://store-b/ ").toString());
        assertEquals("ws://store-b:8080/mesh", ClusterMesh.peerUri("ws://store-b:8080/mesh").toString());
        assertThrows(IllegalArgumentException.class, () -> ClusterMesh.peerUri("http://store-b:8080"));
        assertArrayEquals(ClusterKey.from("x"), ClusterKey.from("x"));
    }
}
