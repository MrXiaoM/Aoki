package top.dsbbs2.t544;

import java.util.Objects;

public class Pair<A,B>{
    public A a;
    public B b;
    public Pair(A a,B b)
    {
        this.a=a;
        this.b=b;
    }
    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof Pair)) return false;
        try {
            return Objects.equals(((Pair<A, B>) obj).a, this.a) && Objects.equals(((Pair<A, B>) obj).b, this.b);
        }catch(ClassCastException e){return false;}
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}
