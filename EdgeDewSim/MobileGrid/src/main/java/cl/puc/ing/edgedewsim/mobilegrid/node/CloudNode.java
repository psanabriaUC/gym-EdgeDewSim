package cl.puc.ing.edgedewsim.mobilegrid.node;

import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import cl.puc.ing.edgedewsim.mobilegrid.network.NetworkModel;
import cl.puc.ing.edgedewsim.simulator.Entity;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CloudNode extends Entity implements Node {
    private static final Object lock = new Object();
    private static final ConcurrentHashMap<UUID, CloudNode> cloudNodes = new ConcurrentHashMap<>();

    private CloudNode(Simulation simulation) {
        super("Cloud Node", simulation);

        simulation.addEntity(this);
        NetworkModel.getModel(simulation).addNewNode(this);
    }

    public static CloudNode getCloudNode(Simulation simulation) {
        UUID id = simulation.getId();
        if (cloudNodes.get(id) == null) {
            synchronized (lock) {
                if (cloudNodes.get(id) == null) {
                    cloudNodes.put(id, new CloudNode(simulation));
                }
            }
        }
        return cloudNodes.get(id);
    }

    public static void removeInstance(Simulation simulation) {
        UUID uuid = simulation.getId();
        synchronized (lock) {
            if (cloudNodes.get(uuid) != null) {
                cloudNodes.remove(uuid);
            }
        }
    }

    public static void resetInstance(Simulation simulation) {
        UUID uuid = simulation.getId();
        synchronized (lock) {
            if (cloudNodes.get(uuid) != null) {
                cloudNodes.remove(uuid);
            }
            cloudNodes.put(simulation.getId(), new CloudNode(simulation));
        }
    }

    @Override
    public void incomingData(@NotNull Node scr, int id) {

    }

    @Override
    public <T> void onMessageReceived(@NotNull Message<T> message) {

    }

    @Override
    public <T> void onMessageSentAck(@NotNull Message<T> message) {

    }

    @Override
    public <T> void fail(@NotNull Message<T> message) {

    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public void startTransfer(@NotNull Node dst, int id, @Nullable Object data) {

    }

    @Override
    public void failReception(@NotNull Node scr, int id) {

    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }

    @Override
    public boolean isSending() {
        return false;
    }

    @Override
    public boolean isReceiving() {
        return false;
    }

    @Override
    public void processEvent(Event event) {

    }
}
