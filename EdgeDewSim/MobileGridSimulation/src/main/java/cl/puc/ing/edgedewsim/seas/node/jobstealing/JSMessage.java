package cl.puc.ing.edgedewsim.seas.node.jobstealing;

public class JSMessage {

    public static final int STEAL_REQUEST_TYPE = 0x10000000;

    private final int requestType;

    public JSMessage(int requestType) {
        this.requestType = requestType;
    }

    public int getRequestType() {
        return requestType;
    }
}
