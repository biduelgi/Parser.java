import java.util.HashMap;

public class TypeMap extends HashMap<Variable, Type> {
    public void display() {
        System.out.println(this.entrySet());
    }
}
