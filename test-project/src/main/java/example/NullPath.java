package example;

class ullPath {
    public static int foo(String str) {
        String s = str.intern(); //str could be NULL, so NP_NULL_ON_SOME_PATH should occur
        for (int i=0; i < str.length(); i++) {
            System.out.println(s);
        }

        return str.length();
    }

    public static void main(String args[]) {
        if (args.length == 1)
            foo(null);
        else foo("Not null");
    }
}