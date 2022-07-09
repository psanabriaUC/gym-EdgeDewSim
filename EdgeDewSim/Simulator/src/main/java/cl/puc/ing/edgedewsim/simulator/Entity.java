package cl.puc.ing.edgedewsim.simulator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract representation of an entity. We define an entity as an actor capable of reacting to events in a simulation
 * (e.g. nodes, smartphones, proxies).
 */
public abstract class Entity {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    protected final Simulation simulation;
    /**
     * A string identifier specified by the user. All entities' names should be unique.
     */
    private final String name;
    /**
     * A system defined numeric ID for internal management purposes. Numerical IDs are defined when loading the entity,
     * there is no guarantee that the same entity will have the same ID in two different simulation runs.
     */
    private final int id;
    /**
     * Flag indicating if this node may process events. If set to false, received events will be ignored.
     */
    private boolean active = true;

    public Entity(String name, Simulation simulation) {
        super();
        this.name = name;
        this.id = NEXT_ID.incrementAndGet();
        this.simulation = simulation;
    }

    /**
     * Resets the static parameters.
     */
    static void reset() {
       // NEXT_ID.set(0);
    }

    final void receiveEvent(Event e) {
        if (isActive())
            this.processEvent(e);
    }

    /**
     * This method should be extended for defining a new entity type.
     *
     * @param event The event that will be processed.
     */
    public abstract void processEvent(Event event);

    /**
     * Gets the entity's numeric ID.
     *
     * @return The entity's numeric ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the entity's string identifier.
     *
     * @return The entity's string identifier.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets whether this entity is active or not.
     *
     * @return Whether this entity is active or not.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Enables or disables this entity.
     *
     * @param active {@code true} to set this entity as active; {@code false} otherwise.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    @Override
    public String toString() {
        return this.getName();
    }

}
