package example;

class NullPath {
    public static int foo(String str) {
		if (str.equals("Butus"))
			str = null;
        String s = str.intern(); //str could be NULL, so NP_NULL_ON_SOME_PATH should occur
        for (int i=0; i < str.length(); i++) {
            System.out.println(s);
        }

        return str.length();
    }

    public static void main(String args[]) {
		String str = null;
		if (args.length == 1) 
			str = "Okoska";
        foo(str);
    }
}
