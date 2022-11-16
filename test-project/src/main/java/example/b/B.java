package example.b;
import java.util.Set;
import example.a.Mutable;
import example.a.MutableInit;

class B {
	public static void main(String args[])
	{
		Set<String> mySet4 = Mutable.mySet;
		mySet4.add("Hihi");
		String array4[] = Mutable.array;
		array4[0]="Hiszti";
		
		System.out.println(MutableInit.mySet2.size());
	}
} 