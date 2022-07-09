package cl.puc.ing.edgedewsim.seas.proxy.jobstealing.condition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class CombinedCondition implements StealingCondition {

    protected List<StealingCondition> conditions = new ArrayList<StealingCondition>();

    @SuppressWarnings("unchecked")
    public void setCondition(String cond) throws Exception {
        String clazzName = cond.split(":")[0];
        Class<StealingCondition> clazz = (Class<StealingCondition>) Class.forName(clazzName);
        StealingCondition pol = clazz.newInstance();
        this.setProperties(pol, clazz, cond.split(":"), 1);
        this.conditions.add(pol);
    }

    private void setProperties(Object ss,
                               Class<?> clazz, String[] split, int i) throws Exception {
        for (int j = i; j < split.length; j++) {
            String prop = split[j].trim();
            String[] kv = prop.split("=");
            String name = "set" + kv[0];
            Method m = clazz.getMethod(name, String.class);
            m.invoke(ss, kv[1]);
        }
    }

}
