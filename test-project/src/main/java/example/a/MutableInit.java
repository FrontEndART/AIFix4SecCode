package example.a;
import java.util.Set;
import java.util.List;
import java.util.TreeSet;
import java.util.Arrays;

public class MutableInit {
    public static final Set<String> mySet2 = new TreeSet<>(Arrays.asList("ez", "az"));
    List<String> list2 = List.of("foo", "bar", "baz");
    
    public static void main(String args[]) {
        
    }
    
}
