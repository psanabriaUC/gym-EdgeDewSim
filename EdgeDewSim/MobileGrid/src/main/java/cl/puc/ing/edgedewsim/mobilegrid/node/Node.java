package cl.puc.ing.edgedewsim.mobilegrid.node;


import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Node {

    /**
     * Gets the id of the node.
     *
     * @return The id of the node.
     */
    int getId();

    /**
     * Starts a transfer
     *
     * @param scr Source Node for the incoming data
     * @param id  id of the incoming data
     */
    void incomingData(@NotNull Node scr, int id);

    /**
     * Receive a job or update message from other node.
     *
     * @param message The received message
     */
    <T> void onMessageReceived(@NotNull Message<T> message);

    /**
     * ACK notification to the sender
     *
     * @param message the message that sends an ack notification
     */
    <T> void onMessageSentAck(@NotNull Message<T> message);

    /**
     * Fail notification to the sender
     *
     * @param message The message that failed to be received.
     */
    <T> void fail(@NotNull Message<T> message);

    boolean isOnline();

    /**
     * Notifies a transfer starting
     *
     * @param dst  Destiny node for the message
     * @param id   Id of the message
     * @param data Data to send
     */
    void startTransfer(@NotNull Node dst, int id, @Nullable Object data);

    /**
     * Notifies transfer error
     *
     * @param scr Source node where the message comes from
     * @param id  Id of the message
     */
    void failReception(@NotNull Node scr, int id);

    /**
     * Indicates the type of node according to its source of power
     */
    boolean runsOnBattery();

    boolean isSending();

    boolean isReceiving();
}
