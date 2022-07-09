package cl.puc.ing.edgedewsim.seas.proxy;

import java.util.Comparator;

public class DescendingDataAssignmentComparator implements
        Comparator<DataAssignment> {

    private final DataAssignmentEvaluatorIF evaluator;

    public DescendingDataAssignmentComparator(DataAssignmentEvaluatorIF eval) {
        evaluator = eval;
    }


    @Override
    public int compare(DataAssignment da1, DataAssignment da2) {

        double da1Value = evaluator.eval(da1);
        double da2Value = evaluator.eval(da2);

        return Double.compare(da2Value, da1Value);
    }

}
