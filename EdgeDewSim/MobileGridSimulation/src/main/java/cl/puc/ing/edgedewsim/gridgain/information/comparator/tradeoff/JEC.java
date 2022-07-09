package cl.puc.ing.edgedewsim.gridgain.information.comparator.tradeoff;

/**
 * The Job Energy-aware Criterion ranks nodes taking into account the relationship between the
 * energy used and quantity of finished jobs by a node. Energy used refers to the battery capacity
 * in milliamperes as reported by the device manufacturer while finished jobs refers to the quantity
 * of benchmarking tests from that a device is able to finish before the depletion of its battery
 * occurs
 */
public class JEC extends StaticComparatorBase {

    public JEC() {
        super("AjConsumptionRate.properties");
    }
}
