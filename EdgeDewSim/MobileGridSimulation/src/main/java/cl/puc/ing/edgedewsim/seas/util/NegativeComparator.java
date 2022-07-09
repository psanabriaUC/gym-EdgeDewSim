package cl.puc.ing.edgedewsim.seas.util;

import java.util.Comparator;

public class NegativeComparator<T extends Comparable<T>> implements Comparator<T> {

    @Override
    public int compare(T arg0, T arg1) {
        return arg1.compareTo(arg0);
    }

}
