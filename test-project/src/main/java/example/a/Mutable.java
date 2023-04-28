package example.a;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;

public class Mutable {
    public static final Set<String> mySet = new TreeSet<>();
    public static final Set<String> mySet2 = new TreeSet<>();
    public static final String array[] = {"Alma", "Korte"};
    public static final String array2[] = {"Alma", "Korte"};
    
    public static Set<String> get() {
       Set<String> mySet2 = mySet;    
       return mySet2;
    } 
    
    public static Set<String> get2() {
       return mySet2;    
    } 
    
    public String getArrayElement2() {
        return array2[0];
    }
    
    public String[] getArray2() {
        return array2;
    }
    
    public static void main(String args[]) {
        
    }
    
}
