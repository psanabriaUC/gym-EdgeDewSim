package cl.puc.ing.edgedewsim.mobilegrid.network;

import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.simulator.Entity;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class of all possible models that simulate the network through which all entities in the grid are connected.
 * This class is in charge of relaying messages from one entity to another and simulating their links.
 */
public abstract class NetworkModel {
    private static final String NETWORK_ENTITY_NAME = "network";
    /**
     * Singleton holder.
     */
    private static final ConcurrentHashMap<UUID, NetworkModel> networkModels = new ConcurrentHashMap<>();
    /**
     * Lock for implementing double checked locking pattern.
     */
    private static final Object sLock = new Object();
    private static double AckMessageSizeInBytes = 0; //2346; //http://stackoverflow.com/questions/5543326/what-is-the-total-length-of-pure-tcp-ack-sent-over-ethernet
    protected final Simulation simulation;
    private final NetworkDelayEntity networkDelayEntity;
    /**
     * Might Cause IllegalArgumentException if other model was already created
     * and the simulation was not reset.
     */
    protected NetworkModel(@NotNull Simulation simulation) {
        super();
        this.networkDelayEntity = new NetworkDelayEntity(NETWORK_ENTITY_NAME, simulation);
        this.simulation = simulation;
        this.simulation.addEntity(this.networkDelayEntity);
    }

    /**
     * Singleton pattern implementation.
     *
     * @param simulation simulation model
     * @return The concrete network model implementation.
     */
    public static NetworkModel getModel(Simulation simulation) {
        if (networkModels.get(simulation.getId()) == null) {
            synchronized (sLock) {
                if (networkModels.get(simulation.getId()) == null) {
                    networkModels.put(simulation.getId(), new SimpleNetworkModel(simulation));
                }
            }
        }

        return networkModels.get(simulation.getId());
    }

    public static void resetModel(Simulation simulation) {
        UUID uuid = simulation.getId();
        synchronized (sLock) {
            if (networkModels.get(uuid) != null) {
                networkModels.remove(uuid);
            }
            networkModels.put(simulation.getId(), new SimpleNetworkModel(simulation));
        }
    }

    public static void removeModel(@NotNull Simulation simulation) {
        UUID uuid = simulation.getId();
        synchronized (sLock) {
            if (networkModels.get(uuid) != null) {
                networkModels.remove(uuid);
            }
        }
    }

    public abstract long getTransmissionTime(Node scr, Node dst, long totalTransferTime, int messageSize);

    /**
     * Sends a message through the network.
     *
     * @param <T>               The type of the payload object.
     * @param source            The sender of the message.
     * @param destination       The recipient of the message
     * @param id                An id for the message.
     * @param totalTransferSize Total transfer size
     * @param length            The size of the message in bytes.
     * @param data              The payload of the message.
     * @param offset            The order of a fragmented message. Used to reconstitute larger messages decomposed into several
     *                          smaller packages.
     * @param lastMessage       A flag indicating whether additional messages should be expected containing more data that
     *                          should be appended to this message's payload.
     * @return                  The time of the simulation at which the sent message is expected to be
     *                          received by the receiver.
     */
    public abstract <T> long send(Node source, Node destination, int id, long totalTransferSize, int length, T data, int offset, boolean lastMessage);

    /**
     * Adds a new node
     *
     * @param n The node to add.
     */
    public abstract void addNewNode(Node n);

    /**
     * Adds new link
     *
     * @param l The link to add.
     */
    public abstract void addNewLink(Link l);

    /**
     * Removes a node and all its associated links
     *
     * @param n The node to be removed.
     */
    public abstract void removeNode(Node n);

    /**
     * Removes a link
     *
     * @param l The link to be removed.
     */
    public abstract void removeLink(Link l);

    /**
     * Gets the nodes in the network
     *
     * @return The nodes in the network.
     */
    public abstract Set<Node> getNodes();

    public double getAckMessageSizeInBytes() {
        return AckMessageSizeInBytes;
    }

    public void setAckMessageSizeInBytes(double ackMessageSizeInBytes) {
        AckMessageSizeInBytes = ackMessageSizeInBytes;
    }

    protected int getNetworkDelayEntityId() {
        return networkDelayEntity.getId();
    }

    /**
     * Proxy that simulates the overhead associated with data transmissions. When re-routing messages through this
     * entity, the {@link Node#onMessageReceived(Message)} method will be invoked on the destination device before
     * relaying the respective message. As for the source, either {@link Node#onMessageSentAck(Message)} or
     * {@link Node#fail(Message)} will be invoked to notify if the transfer was successful.
     */
    protected static class NetworkDelayEntity extends Entity {

        NetworkDelayEntity(String name, Simulation simulation) {
            super(name, simulation);
        }

        /**
         * Events defined by the {@link Message} class are relayed to both the sender and receiver so they can act upon
         * them and invoke custom message handling events. This serves to emulate an ACK/NACK for the sender, and to
         * emulate energy consumption due to network transmission overhead. The amount of energy used for this purpose
         * is to be defined by each {@link Entity} implementation.<br/>
         *
         * @param event The event that will be processed.
         */
        @Override
        public void processEvent(Event event) {
            Message<?> message = event.getData();

            if ((Objects.requireNonNull(message).getSource().isOnline()) && (message.getDestination().isOnline())) {
                message.getDestination().onMessageReceived(message);
                message.getSource().onMessageSentAck(message);
            } else {
                message.getSource().fail(message);
                message.getDestination().failReception(message.getSource(), message.getId());
            }
        }
    }

}
