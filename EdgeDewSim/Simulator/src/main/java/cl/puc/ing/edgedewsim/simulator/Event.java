package cl.puc.ing.edgedewsim.simulator;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A raw event to be dispatched and processed by an {@link Entity}. It defines an originator, a target, a timestamp,
 * an identifier to distinguish different event types, and a payload. An event usually implies a change in state on
 * the receiving entities.
 */
public class Event implements Comparable<Event> {

    /**
     * Flag to specify the event should be dispatched to all available nodes.
     */
    public static final int BROADCAST = -1;
    /**
     * Flag to specify an event has no originator.
     */
    public static final int NO_SOURCE = -1;
    private static final AtomicLong NEXT_ID = new AtomicLong(0);
    private final long eventId;
    private final int srcId;
    private final int trgId;
    private final int eventType;
    private final Object data;
    private long time;

    private Event(long eventId, long time, int srcId, int trgId, int eventType, Object data) {
        super();
        this.eventId = eventId;
        this.time = time;
        this.srcId = srcId;
        this.trgId = trgId;
        this.eventType = eventType;
        this.data = data;
    }

    /**
     * Factory method. Creates an event setting all its parameters.
     *
     * @param sourceId  source, use Event.NO_SOURCE to indicate that the event has no source
     * @param targetId  target, use Event.BROADCAST to indicate that is a broadcast event
     * @param eventType indicates the event type
     * @param data      extra information, might be null
     * @return a new event
     */
    public static Event createEvent(int sourceId, long time, int targetId, int eventType, Object data) {
        long eventId = NEXT_ID.getAndIncrement();
        return new Event(eventId, time, sourceId, targetId, eventType, data);
    }

    /**
     * Resets the static atomic counter to assigning new IDs to new events.
     */
    static void reset() {
        // NEXT_ID.set(0);
    }

    @Override
    public int compareTo(Event o) {
        long result = this.time - o.time;
        if (result < 0)
            return -1;
        if (result > 0)
            return 1;
        long id = this.eventId - o.eventId;
        if (id < 0)
            return -1;
        if (id > 0)
            return 1;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof Event) && (((Event) obj).eventId == this.eventId));
    }

    /**
     * Gets the system defined ID associated to this event.
     *
     * @return The ID of this event.
     */
    public long getEventId() {
        return eventId;
    }

    /**
     * Gets the time at which this event should be dispatched.
     *
     * @return The timestamp of this event.
     */
    public long getTime() {
        return time;
    }

    /**
     * Gets the ID of the {@link Entity} that originates this event, or {@link Event#NO_SOURCE} if this event has
     * no specific source.
     *
     * @return The ID of the {@link Entity} that dispatches this event, or {@link Event#NO_SOURCE}.
     */
    public int getSourceId() {
        return srcId;
    }

    /**
     * Gets the ID of the {@link Entity} that should receive and process this event, or {@link Event#BROADCAST} if this
     * event should be sent to everyone.
     *
     * @return The ID of the {@link Entity} that will handle this event, or {@link Event#BROADCAST}.
     */
    public int getTargetId() {
        return trgId;
    }

    /**
     * Gets a numeric identifier that represents a certain type of event. The meaning of different values of this
     * attribute depends on the simulation being run.
     *
     * @return The event type.
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * Gets the payload associated with this event. May be null.
     *
     * @return The event's payload.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    /**
     * Updates the timestamp for this event.
     *
     * @param newTime The new timestamp for this event.
     */
    void modifyTime(long newTime) {
        time = newTime;
    }

    /**
     * Data for the NetworkActivity event.
     */
    public static class NetworkActivityEventData {
        private final int messageSize;
        private final boolean incoming;

        public NetworkActivityEventData(int messageSize, boolean incoming) {
            this.messageSize = messageSize;
            this.incoming = incoming;
        }

        public int getMessageSize() {
            return messageSize;
        }

        public boolean isMessageIncoming() {
            return incoming;
        }
    }

}
