package eu.assuremoss.utils;

import lombok.Data;

@Data
public class Pair<A, B> {
    private A a;
    private B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return "Pair(A = " + a + ", B = " + b + ")";
    }
}
