package cl.puc.ing.edgedewsim.gridgain.information.comparator.tradeoff;

/**
 * The Time Energy-aware Criterion ranks nodes taking into account the relationship between the
 * energy used and time a node was executing jobs. Energy used refers to the battery capacity
 * in milliamperes as reported by the device manufacturer while time refers to the time a device
 * was performing benchmarking tests before the depletion of its battery occurs
 */
public class TEC extends StaticComparatorBase {


    public TEC() {
        super("AhConsumptionRate.properties");
    }
}
