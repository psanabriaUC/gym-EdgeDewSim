package cl.puc.ing.edgedewsim.mobilegrid.network;

import cl.puc.ing.edgedewsim.mobilegrid.node.Node;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a message that needs to be transferred between devices.
 */
public class Message<T> {
    public static final int STEAL_MSG_SIZE = 2346 + 20; //http://stackoverflow.com/questions/5543326/what-is-the-total-length-of-pure-tcp-ack-sent-over-ethernet
    // 20 bytes are integrated by 16 bytes corresponding to a IPv6 address of the stealer node + 4 bytes corresponding to a integer that indicates the quantity of jobs to be stolen

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    /**
     * The total size of this message. Used for estimating transfer times.
     */
    private final long messageSize;
    /**
     * Indicates the offset of this data package with respect to the full message.
     */
    private final int offset;
    /**
     * Flag to indicate if this is the last package of a message or not. If false, receivers should expect to get
     * in the future another message with a higher offset than this one.
     */
    private final boolean lastMessage;
    /**
     * The sender of this message.
     */
    private Node source;

    // Offset and HasNextMessage are used to simulate message fragmentation.
    /**
     * The receiver of this message.
     */
    private Node destination;
    /**
     * The payload of this message.
     */
    private T data;

    /**
     * The total size of this message. Used for estimating transfer times.
     */
    private long totalDataSize;
    /*
    public Message(int id, Node source, Node dst, T data, long messageSize) {
        this(id, source, dst, data, messageSize, 0, true);
    }
    */

    /**
     * Creates a new message to be transferred across the network.
     *  @param id           The ID of the message.
     * @param source        The sender of the message.
     * @param dst           The message's recipient.
     * @param data          The message's payload.
     * @param totalDataSize The size of the whole data chunk which the message belongs to
     * @param messageSize   The size of the message in bytes.
     * @param offset        The offset of this message's payload. Only relevant when a large payload has to be fragmented across
 *                          multiple messages.
     * @param lastMessage   A flag indicating whether this is the last fragment for a given message or not.
     */
    public Message(int id, Node source, Node dst, T data, long totalDataSize, long messageSize, int offset, boolean lastMessage) {
        super();
        this.id = id; //NEXT_ID.incrementAndGet();
        this.source = source;
        this.destination = dst;
        this.data = data;
        this.totalDataSize = totalDataSize;
        this.messageSize = messageSize;
        this.offset = offset;
        this.lastMessage = lastMessage;
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public Node getSource() {
        return source;
    }

    public void setSource(Node source) {
        this.source = source;
    }

    public Node getDestination() {
        return destination;
    }

    public void setDestination(Node dst) {
        this.destination = dst;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getMessageSize() {
        return messageSize;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isLastMessage() {
        return lastMessage;
    }

    public long getTotalDataSize() {
        return totalDataSize;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "\n" +
                "destinationNode=" + this.getDestination().toString() + "\n" +
                "sourceNode=" + this.getSource().toString() + "\n" +
                "isLastMessage=" + this.isLastMessage() + "\n" +
                "offset=" + this.getOffset() + "\n" +
                "dataType=" + this.getData().toString();
    }
}
