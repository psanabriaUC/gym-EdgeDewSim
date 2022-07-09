package cl.puc.ing.edgedewsim.seas.proxy.jobstealing;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

/**
 * Stealing policy
 * Todos los set deben tener una version que reciban un string como parametro
 *
 * @author cuchillo
 */
public interface StealingPolicy {

    /**
     * Returns how many jobs should be stolen
     *
     * @param stealer The node stealing jobs.
     * @param victim  The node to steal jobs from.
     * @return Number of stolen jobs.
     */
    int jobsToSteal(Device stealer, Device victim);
}
