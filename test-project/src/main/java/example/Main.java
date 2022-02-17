package example;
public class Main {
    public static String MY_CONSTANT = "This variable should be final!";  //example for MS_SHOULD_BE_FINAL

    public static void main(String args[]) {
        System.out.println("Demo program");
        System.out.println(MY_CONSTANT);

        MyDate date = new MyDate();
        System.out.println(date.getDate());

        ArrayDemo ad = new ArrayDemo ("OSA Tool");
        ad.withActions(args);
    }
}
