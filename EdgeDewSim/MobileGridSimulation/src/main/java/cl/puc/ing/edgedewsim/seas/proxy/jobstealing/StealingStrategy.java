package cl.puc.ing.edgedewsim.seas.proxy.jobstealing;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

/**
 * Stealing Strategy
 * Todos los set deben tener una version que reciban un string como parametro
 *
 * @author cuchillo
 */
public interface StealingStrategy {

    Device getVictim(StealerProxy sp, Device stealer);
}
