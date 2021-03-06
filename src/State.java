import java.util.HashMap;

public class State extends HashMap<Variable, Value> {

    public State(){}

    public State(Variable key, Value val){put(key, val);}

    public State onion(Variable key, Value val){
        put(key, val);
        return this;
    }

    public State onion(State t){
        for (Variable key : t.keySet( ))
            put(key, t.get(key));
        return this;
    }
}
