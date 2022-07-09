package cl.puc.ing.edgedewsim.mobilegrid.network;

import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.mobilegrid.node.TransferInfo;

/**
 * Base utility class for handling messages at different stages of their life cycle.
 *
 * @param <T> The type of the message's payload to process.
 */
public class MessageHandler<T> {
    /**
     * Called when a message is received. No guarantee is made that the payload is complete.
     *
     * @param message The message received.
     */
    public void onMessageReceived(Message<T> message) {
    }

    /**
     * Called after {@link MessageHandler#onMessageReceived(Message)} only if the entire payload of the message
     * has been confirmed to have been received.
     *
     * @param message The message received.
     */
    public void onMessageFullyReceived(Message<T> message) {
    }

    /**
     * Called right before sending a message.
     *
     * @param transferInfo The information of the message's transfer state.
     */
    public void onWillSendMessage(TransferInfo<T> transferInfo) {
    }

    /**
     * Called when receiving confirmation that a delivered message has been successfully received by the intended
     * receiver.
     *
     * @param message The message that was sent.
     */
    public void onMessageSentAck(Message<T> message) {
    }

    /**
     * Called after {@link MessageHandler#onMessageSentAck(Message)} only if the entire payload of the message
     * has been confirmed to have been received.
     *
     * @param message The message that was sent.
     */
    public void onMessageFullySent(Message<T> message) {
    }

    /**
     * Called when a message sent by someone else failed to arrive to this {@link Node}.
     *
     * @param message The message sent.
     */
    public void onCouldNotReceiveMessage(Message<T> message) {
    }

    /**
     * Called when this {@link Node} does not have enough power to send a message.
     *
     * @param transferInfo The information of the message's transfer state.
     */
    public void onCouldNotSendMessage(TransferInfo<T> transferInfo) {
    }

    /**
     * Called when a message sent by this {@link Node} could not be received by its intended recipient.
     *
     * @param message The message sent.
     */
    public void onMessageSentFailedToArrive(Message<T> message) {
    }

}
