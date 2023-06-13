package top.dsbbs2.t544;

import java.util.Arrays;

public class State {
    public Long[]/*uint[]*/ state=new Long[16];
    {
        Arrays.fill(state,0L);
    }
    public Long[]/*uint[]*/ org_state=new Long[16];
    {
        Arrays.fill(org_state,0L);
    }
    public short/*ubyte*/ nr;
    public short/*ubyte*/ p;
}
