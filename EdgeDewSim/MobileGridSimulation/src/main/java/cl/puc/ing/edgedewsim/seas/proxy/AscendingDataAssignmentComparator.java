package cl.puc.ing.edgedewsim.seas.proxy;

import java.util.Comparator;

public class AscendingDataAssignmentComparator implements
        Comparator<DataAssignment> {

    private final DataAssignmentEvaluatorIF evaluator;

    public AscendingDataAssignmentComparator(DataAssignmentEvaluatorIF evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public int compare(DataAssignment da1, DataAssignment da2) {
        double da1Value = da1.eval();
        double da2Value = evaluator.eval(da2);

        return Double.compare(da1Value, da2Value);
    }

}
