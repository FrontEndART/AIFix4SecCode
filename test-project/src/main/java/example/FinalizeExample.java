package example;
public class FinalizeExample {  
    public static void main(String[] args)   
    {   
        FinalizeExample obj = new FinalizeExample();   
        System.out.println(obj.hashCode());   
        obj = null;   
        // calling garbage collector
        System.gc();   
        System.out.println("end of garbage collection");   
  
    }   
 
    public void finalize()
    {   
        System.out.println("finalize method called");   
    }   
} 
