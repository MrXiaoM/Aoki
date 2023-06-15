package top.dsbbs2.t544;

import java.security.MessageDigest;
import java.util.ArrayList;
import static top.dsbbs2.t544.Data.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface Tlv544Sign {
    public static byte[] signBytes(byte[] payload){
        long curr=System.currentTimeMillis()*1000;
        return sign(curr,payload);
    }
    public static byte[] sign(long time,byte[] payload)
    {
        if(time > 1000000) {
            time %= 1000000;
        }
        byte[] curr_bytes=longToLittleEndianBytes(time);
        ArrayList<Byte> bytes=new ArrayList<>(payload.length+curr_bytes.length);
        for(byte i : payload) bytes.add(i);
        for(byte i : curr_bytes) bytes.add(i);
        Random rng=new Random(time);
        Function<Random,Integer> gen_idx=r->r.nextInt(50);
        Short[]/*ubyte[]*/ key_table=new Short[40];
        Arrays.fill(key_table, (short) 0);
        for(int i=0;i<2;i++) key_table[i]= (short) (KEY_TABLE[gen_idx.apply(rng)]+50);
        for(int i=1;i<3;i++) key_table[i + 1] = (short) (key_table[i] + 20);
        Short[]/*ubyte[]*/ ks=Arrays.copyOfRange(key_table,0,4);
        List<Short>/*ListView<ubyte>*/ k3=Arrays.asList(key_table).subList(4,14);
        IntStream.range(0,4).forEach(i->k3.set(2+i,KEY1[i]));
        for(int i=0;i<4;i++) k3.set(i+2+4, (short) (KEY2[i]^ks[i]));
        for(int i=0;i<2;i++){
            k3.set(i,k3.get(i+8));
            k3.set(i+8,(short)0);
        }
        byte[]/*ubyte[]*/ key=new byte[8];
        for(int i=0;i<8;i++) key[i]=(byte)(short)k3.get(i);
        byte[] rc_key=key;
        RC4 cipher=new RC4(rc_key);
        key=cipher.encrypt(key);
        byte[] crc_dat=new byte[21];
        for(int i=4;i<12;i++) crc_dat[i]=(byte)CRC_PART[i-4];
        byte[] part_k=new byte[32];
        for(int i=4;i<4+32;i++)part_k[i-4]=(byte)(short)key_table[i];
        byte[] part_crc=new byte[8];
        for(int i=4;i<4+8;i++)part_k[i-4]=(byte)(short)crc_dat[i];
        tencentEncA(bytes,part_k,part_crc);
        try {
            MessageDigest md5_core = MessageDigest.getInstance("MD5");
            md5_core.update(unwrapBytes(bytes.parallelStream().toArray(Byte[]::new)));
            byte[] md5_result=md5_core.digest();
            crc_dat[2]=1;
            crc_dat[3]=curr_bytes[3];
            crc_dat[4]=1;
            for(int i=5;i<5+4;i++) crc_dat[i]=(byte)(short)key_table[i-5];
            byte[] t=u32ToBigEndianBytes(time);
            for(int i=9;i<9+4;i++) crc_dat[i]=t[i-9];
            for(int i=13;i<13+8;i++) crc_dat[i]=md5_result[i-13];

            long/*uint*/ crc32=tencentCrc32(CRC_TABLE,unwrapBytes(subList(Arrays.asList(wrapBytes(crc_dat)),2).parallelStream().toArray(Byte[]::new)));
            byte[] crc32_bytes=u32ToLittleEndianBytes(crc32);
            for(int i=36;i<36+4;i++) key_table[i]=(short)crc32_bytes[i-36];
            crc_dat[0]=(byte)(short)key_table[36];
            crc_dat[1]=(byte)(short)key_table[39];
            byte[] tmp=new byte[4];
            rng.nextBytes(tmp);
            long t1=bigEndianBytesToU32(tmp);
            rng.nextBytes(tmp);
            long t2=bigEndianBytesToU32(tmp);
            rng.nextBytes(tmp);
            long t3=bigEndianBytesToU32(tmp);
            byte[] nbytes=u32ToBigEndianBytes(t1^t2^t3);
            for(int i=0;i<4;i++) key_table[i]=(short)nbytes[i];
            for(int i=4;i<9;i++) {
                List<Short> k=Arrays.asList(key_table).subList(0,i<<1);
                Pair<List<Short>,List<Short>> aaa=shortListSplitAt(k,i);
                List<Short> l=aaa.a,r=aaa.b;
                for(int i2=0;i2<r.size();i2++) r.set(i2,l.get(i2));
            }
            byte[] out=new byte[39];
            for(int i=0;i<39;i++) out[i]=(byte)(short)key_table[i];
            transformEncode(crc_dat);
            byte[] enc_dat=new byte[21];
            tencentEncB(Arrays.asList(key_table).subList(0,16),crc_dat,enc_dat);
            transformDecode(enc_dat);
            out[0] = 12;
            out[1] = 5;
            for(int i=2;i<2+4;i++) out[i]=nbytes[i-2];
            for(int i=6;i<6+21;i++) out[i]=enc_dat[i-6];
            Arrays.fill(out,27,27+4, (byte) 0);
            out[31] = (byte) TABLE2[gen_idx.apply(rng)];
            out[32] = (byte) TABLE2[gen_idx.apply(rng)];
            byte[] tmp_byte=new byte[1];
            rng.nextBytes(tmp_byte);
            short tmp_byte_fin=tmp_byte[0];
            if(tmp_byte_fin<0) tmp_byte_fin= (short) ((128-tmp_byte_fin-1)%256);
            tmp_byte_fin%=256;
            byte add= (byte) (tmp_byte_fin%8);
            add |= 0b1;
            out[33] = (byte) (out[31] + add);
            out[34] = (byte) (out[32] + 9 - add + 1);
            Arrays.fill(out,35,35+4,(byte)0);

            return out;
        }catch(Throwable t){return sneakyThrow(t);}
    }

    public static void tencentEncB(List<Short> ktb, byte[] crc, byte[] output) {
        byte[] buf=new byte[16];
        for(int i=0;i<output.length;i++)
        {
            if((i & 15) == 0) {
                for(int i2=0;i2<buf.length;i2++) buf[i2]=(byte)(short)ktb.get(i2);
                _tencent_enc_b(buf, ENC_TRB);
                for(int j=ktb.size()-1;j>-1;j--){
                    ktb.set(j,ubyteWrappingAdd(ktb.get(j),(short)1));
                    if(ktb.get(j) != 0) {
                        break;
                    }
                }
            }
            output[i] = sub_aa(i, ENC_TEA, buf, crc);
        }
    }
    public static void _tencent_enc_b(byte[] p1,long[] p2)
    {
        for(int i=0;i<9;i++)
        {
            permute(IP_TABLE,p1);
            int i4=i<<2;
            sub_b(p1,Arrays.asList(wrapLongArray(p2)).subList(i4,i4+4));
            sub_c(ENC_TEB,p1);
            sub_e(ENC_TEC,p1);
        }
        permute(IP_TABLE,p1);
        int f4=10<<2;
        sub_b(p1,Arrays.asList(wrapLongArray(p2)).subList(f4-4,f4));
        sub_c(ENC_TEB,p1);
        sub_a(p1,Arrays.asList(wrapLongArray(p2)).subList(f4,f4+4));
    }
    public static void sub_a(byte[] data,List<Long> t)
    {
        for(int i=0;i<t.size();i++)
        {
            long num=t.get(i);
            byte[] num_bytes=u32ToLittleEndianBytes(num);
            byte a=num_bytes[0],b=num_bytes[1],c=num_bytes[2],d=num_bytes[3];
            if(subList(Arrays.asList(wrapBytes(data)),i<<2).size()>=4)
            {
                data[i<<2]^=d;
                data[(i<<2)+1]^=c;
                data[(i<<2)+2]^=b;
                data[(i<<2)+3]^=a;
            }else throw new RuntimeException();
        }
    }
    public static void sub_b(byte[] data,List<Long> t)
    {
        byte[] tb=new byte[16];
        for(int i=0;i<t.size();i++)
        {
            long val=t.get(i);
            int i4=i<<2;
            byte[] val_bytes=u32ToLittleEndianBytes(val);
            for(int j=i4;j<i4+4;j++) tb[j]=val_bytes[j-i4];
        }
        for(int i=0;i<4;i++)
        {
            int i4=i<<2;
            if(subList(Arrays.asList(wrapBytes(data)),i<<2).size()>=4)
            {
                int v=(i4 + 3) & 15,w=(i4 + 6) & 15,x=(i4 + 9) & 15,y=(i4 + 12) & 15;
                v=tb[v];
                w=tb[w];
                x=tb[x];
                y=tb[y];
                data[i<<2]^=v;
                data[(i<<2)+1]^=w;
                data[(i<<2)+2]^=x;
                data[(i<<2)+3]^=y;
            }else throw new RuntimeException();
        }
    }
    public static void sub_c(short[][] t,byte[] data)
    {
        for(int i=0;i<16;i++)
        {
            int datum=convertToShort(data[i]);
            short[] tab=t[datum>>4];
            data[i]= (byte) tab[datum & 15];
        }
    }
    public static void permute(short[] t,byte[] p)
    {
        byte[] tmp=p;
        for(int i=0;i<16;i++) {
            int idx=t[i];
            p[i]=tmp[idx];
        }
    }
    public static void sub_e(short[][] t,byte[] data)
    {
        for(int i=0;i<4;i++)
        {
            int i4=i<<2;
            if(Arrays.asList(wrapBytes(data)).subList(i4,i4+4).size()>=4)
            {
                short a=convertToShort(data[i4]),b=convertToShort(data[i4+1]),c=convertToShort(data[i4+2]),d=convertToShort(data[i4+3]);
                short[] ta=t[a],tb=t[b],tc=t[c],td=t[d];
                data[i4] = (byte) ((c ^ d) ^ (ta[0] ^ tb[1]));
                data[i4 + 1] = (byte) ((a ^ d) ^ (tb[0] ^ tc[1]));
                data[i4 + 2] = (byte) ((a ^ b) ^ (tc[0] ^ td[1]));
                data[i4 + 3] = (byte) ((b ^ c) ^ (td[0] ^ ta[1]));
            }else throw new RuntimeException();
        }
    }
    public static byte sub_aa(int i,short[][][][] table,byte[] buf,byte[] data)
    {
        int datum=convertToShort(data[i]);
        int idx = i & 15;
        int bufdx = convertToShort(buf[idx]);
        short[][][] tb=table[idx];
        short a = (short) (tb[0][datum >> 4][bufdx >> 4] << 4);
        short b = tb[1][datum & 15][bufdx & 15];
        return (byte) (a ^ b);
    }
    public static Long[] wrapLongArray(long[] arr)
    {
        Long[] ret=new Long[arr.length];
        for(int i=0;i<arr.length;i++) ret[i]=arr[i];
        return ret;
    }
    public static void transformEncode(byte[] x)
    {
        transformer(x,ENC_TR);
    }
    public static void transformDecode(byte[] x)
    {
        transformer(x,DEC_TR);
    }
    public static void transformer(byte[] x,short[][] tab)
    {
        for(int i2=0;i2<x.length;i2++)
        {
            short val=convertToShort(x[i2]);
            int i=i2<<1;
            int e=val;
            short a = (short) (tab[i & 31][e >> 4] << 4);
            short b = tab[(i + 1) & 31][e & 15];
            x[i2] =(byte)(a^b);
        }
    }
    public static byte[] shortsToBytes(Short[] shorts)
    {
        byte[] ret=new byte[shorts.length];
        for(int i=0;i<shorts.length;i++) ret[i]=(byte)(short)shorts[i];
        return ret;
    }
    public static byte[] shortsToBytes(short[] shorts)
    {
        byte[] ret=new byte[shorts.length];
        for(int i=0;i<shorts.length;i++) ret[i]=(byte)shorts[i];
        return ret;
    }
    public static long bigEndianBytesToU32(byte[] bytes) {
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Invalid byte array length (4 bytes required)");
        }

        long result = 0;
        result |= (bytes[0] & 0xFFL) << 24;
        result |= (bytes[1] & 0xFFL) << 16;
        result |= (bytes[2] & 0xFFL) << 8;
        result |= bytes[3] & 0xFFL;

        return result;
    }

    public static byte[] u32ToLittleEndianBytes(long value) {
        byte[] result = new byte[4];

        result[0] = (byte) (value & 0xFF);
        result[1] = (byte) ((value >> 8) & 0xFF);
        result[2] = (byte) ((value >> 16) & 0xFF);
        result[3] = (byte) ((value >> 24) & 0xFF);

        return result;
    }

    public static long u32RotateRight(long value, int shift) {
        shift = shift & 0x1F; // Ensure shift is within 0-31 range
        value = value & 0xFFFFFFFFL; // Ensure value is within the range of uint32

        return ((value >>> shift) | (value << (32 - shift))) & 0xFFFFFFFFL;
    }
    public static long u32RotateLeft(long value, int shift) {
        shift = shift & 0x1F; // Ensure shift is within 0-31 range
        value = value & 0xFFFFFFFFL; // Ensure value is within the range of uint32

        return ((value << shift) | (value >>> (32 - shift))) & 0xFFFFFFFFL;
    }
    public static <T> List<T> subList(List<T> l,int start)
    {
        return l.subList(start,l.size());
    }
    public static long/*uint*/ tencentCrc32(long[]/*uint[]*/ table,byte[] bytes)
    {
        if(bytes.length==0) return 0;
        long crc=(long)(Math.pow(2,32)-1);
        for(byte val : bytes)
        {
            short val2=convertToShort(val);
            val2^=convertToShort((byte)crc);
            val2=convertToShort((byte)val2);
            crc = (crc >> 8) ^ table[val2];
        }
        return ~crc;
    }

    public static short convertToShort(byte unsignedByte) {
        return (short) (unsignedByte & 0xFF);
    }
    public static byte[] u32ToBigEndianBytes(long value) {
        byte[] result = new byte[4];

        result[0] = (byte) ((value >> 24) & 0xFF);
        result[1] = (byte) ((value >> 16) & 0xFF);
        result[2] = (byte) ((value >> 8) & 0xFF);
        result[3] = (byte) (value & 0xFF);

        return result;
    }

    public static byte[] unwrapBytes(Byte[] bytes)
    {
        byte[] ret=new byte[bytes.length];
        for(int i=0;i<bytes.length;i++)
            ret[i]=bytes[i];
        return ret;
    }
    public static Byte[] wrapBytes(byte[] bytes)
    {
        Byte[] ret=new Byte[bytes.length];
        for(int i=0;i<bytes.length;i++)
            ret[i]=bytes[i];
        return ret;
    }
    @SuppressWarnings({"unchecked"})
    public static <T extends Throwable,R> R sneakyThrow(Throwable t) throws T{
        throw (T)t;
    }
    public static void tencentEncA(ArrayList<Byte> input,byte[] key,byte[] data){
        State state=new State();
        stateInit(state,key,data,0L,(short)20);
        encrypt(state,input);
    }
    public static void encrypt(State state,ArrayList<Byte> data)
    {
        int cnt=0;
        int len=data.size();
        while(len>0){
            if(state.p==0){
                for(int t=0;t<state.nr;t+=2) subAd(state.state);
                for(int i=0;i<16;i++) state.state[i]= u32WrappingAdd(state.state[i],state.org_state[i]);
            }
            byte[] sb=new byte[16<<2];
            for(int i=0;i<state.state.length;i++){
                long val=state.state[i];
                byte[] vb=u32ToLittleEndianBytes(val);
                for(int j=(i<<2);j<((i+1)<<2);j++) sb[j]=vb[j-(i<<2)];
            }
            while(state.p<=64&&len!=0){
                data.set(cnt, (byte) (data.get(cnt)^sb[state.p]));
                state.p+=1;
                cnt+=1;
                len-=1;
            }
            if(state.p>=64)
            {
                state.p=0;
                state.org_state[12] += 1;
                state.state = state.org_state;
            }
        }
    }
    public static void subAd(Long[]/*uint[]*/ st)
    {
        long r12=st[3];
        long dx=st[4];
        long bp=st[11];
        long r15= u32WrappingAdd(st[0],dx);
        long r9=u32RotateLeft(st[12]^r15,16);
        long si=st[5];
        long r11= u32WrappingAdd(st[8],r9);
        long r14= u32WrappingAdd(st[1],si);
        long r8=u32RotateLeft(st[13]^r14,16);
        long cx=st[6];
        long r13= u32WrappingAdd(st[2],cx);
        long bx= u32WrappingAdd(st[9],r8);
        long di=u32RotateLeft(st[14]^r13,16);
        long r10= u32WrappingAdd(st[10],di);
        dx=u32RotateLeft(dx^r11,12);
        r15= u32WrappingAdd(r15,dx);
        r9=u32RotateLeft(r9^r15,8);
        si=u32RotateLeft(si^bx,12);
        r14= u32WrappingAdd(r14,si);
        cx=u32RotateLeft(cx^r10,12);
        r11= u32WrappingAdd(r11,r9);
        r8^=r14;
        r13= u32WrappingAdd(r13,cx);
        r8=u32RotateLeft(r8,8);
        bx= u32WrappingAdd(bx,r8);
        di=u32RotateLeft(di^r13,8);
        long tmp0=u32RotateLeft(dx^r11,7);
        dx=st[7];
        si^=bx;
        long tmp1=bx;
        bx=r10;
        si=u32RotateLeft(si,7);
        bx= u32WrappingAdd(bx,di);
        r12= u32WrappingAdd(r12,dx);
        r15= u32WrappingAdd(r15,si);
        r10=u32RotateLeft(st[15]^r12,16);
        cx=u32RotateLeft(cx^bx,7);
        bp= u32WrappingAdd(bp,r10);
        r14= u32WrappingAdd(r14,cx);
        dx=u32RotateLeft(dx^bp,12);
        r9=u32RotateLeft(r9^r14,16);
        r12= u32WrappingAdd(r12,dx);
        r10=u32RotateLeft(r10^r12,8);
        bp= u32WrappingAdd(bp,r10);
        r10=u32RotateLeft(r10^r15,16);
        bx= u32WrappingAdd(bx,r10);
        si=u32RotateLeft(si^bx,12);
        r15= u32WrappingAdd(r15,si);
        st[0]=r15;
        r10=u32RotateLeft(r10^r15,8);
        bx= u32WrappingAdd(bx,r10);
        st[15]=r10;
        st[10]=bx;
        dx=u32RotateLeft(dx^bp,7);
        bp= u32WrappingAdd(bp,r9);
        cx=u32RotateLeft(cx^bp,12);
        r13= u32WrappingAdd(r13,dx);
        r14= u32WrappingAdd(r14,cx);
        st[5]=u32RotateLeft(si^bx,7);
        r8=u32RotateLeft(r8^r13,16);
        st[1]=r14;
        r11= u32WrappingAdd(r11,r8);
        r9=u32RotateLeft(r9^r14,8);
        bp= u32WrappingAdd(bp,r9);
        st[12]=r9;
        dx=u32RotateLeft(dx^r11,12);
        st[11]=bp;
        r13= u32WrappingAdd(r13,dx);
        st[6]=u32RotateLeft(cx^bp,7);
        r8=u32RotateLeft(r8^r13,8);
        st[2]=r13;
        r11= u32WrappingAdd(r11,r8);
        st[8]=r11;
        st[7]=u32RotateLeft(dx^r11,7);
        st[13]=r8;
        r12= u32WrappingAdd(r12,tmp0);
        di^=r12;
        di=u32RotateLeft(di,16);
        cx= u32WrappingAdd(tmp1,di);
        dx=u32RotateLeft(tmp0^cx,12);
        r12= u32WrappingAdd(r12,dx);
        di^=r12;
        st[3]=r12;
        long rd=u32RotateLeft(di,8);
        st[14]=rd;
        cx= u32WrappingAdd(cx,rd);
        st[4]=u32RotateLeft(dx^cx,7);
        st[9]=cx;
    }
    public static long longRotateLeft(long value, int distance) {
        distance &= 63;  // Ensure distance is within the range [0, 63]
        return (value << distance) | (value >>> (64 - distance));
    }

    public static long longRotateRight(long value, int distance) {
        distance &= 63;  // Ensure distance is within the range [0, 63]
        return (value >>> distance) | (value << (64 - distance));
    }
    public static long ulongWrappingAdd(long a, long b)
    {
        return (long)((a+b)%Math.pow(2,64));
    }
    public static long u32WrappingAdd(long a,long b)
    {
        return (long)((a+b)%Math.pow(2,32));
    }
    public static short ubyteWrappingAdd(short a,short b)
    {
        return (short)((a+b)%Math.pow(2,8));
    }
    public static void stateInit(State state,byte[] key,byte[] data,long counter,short nr)
    {
        state.nr=nr;
        state.p=0;
        initStateImpl(state,key,data,counter);
    }
    public static void initStateImpl(State state,byte[] key,byte[] data,long counter)
    {
        Long[] stat=state.state;
        System.arraycopy(STAT_CHK, 0, stat, 0, 4);
        for(int i=0;i<32;i+=4)
        {
            int i4=i+4;
            byte[] kb=Arrays.copyOfRange(key,i,i4);
            long k=littleEndianBytesToUnsignedInt(kb);
            stat[(i+16)>>2]=k;
        }
        BiFunction<List<Long>,byte[],Void> put_16b=(dst,src)->{
            Pair<Byte[],Byte[]> tmp=bytesSplitAt(src,4);
            Byte[] a=tmp.a,b=tmp.b;
            long u1=littleEndianBytesToUnsignedInt(a),u2=littleEndianBytesToUnsignedInt(b);
            dst.set(0,u1);
            dst.set(1,u2);
            return null;
        };
        put_16b.apply(Arrays.asList(stat).subList(12,14),longToLittleEndianBytes(counter));
        put_16b.apply(Arrays.asList(stat).subList(14,16),data);
        Long[] org_state=state.org_state;
        for(int i=0;i<12;i++) org_state[i]=stat[i];
        for(int i=12;i<16;i++) org_state[i]= ThreadLocalRandom.current().nextLong((long)Math.pow(2,32));
    }
    public static long littleEndianBytesToUnsignedInt(byte[] bytes) {
        if (bytes.length > 4) {
            throw new IllegalArgumentException("Invalid byte array length (up to 4 bytes allowed)");
        }

        long result = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            result <<= 8;
            result |= bytes[i] & 0xFF;
        }

        return result;
    }
    public static long littleEndianBytesToUnsignedInt(Byte[] bytes) {
        if (bytes.length > 4) {
            throw new IllegalArgumentException("Invalid byte array length (up to 4 bytes allowed)");
        }

        long result = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            result <<= 8;
            result |= bytes[i] & 0xFF;
        }

        return result;
    }
    public static byte[] longToLittleEndianBytes(long value) {
        byte[] result = new byte[8];

        for (int i = 0; i < 8; i++) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        return result;
    }
    public static Pair<Byte[],Byte[]> bytesSplitAt(byte[] arr,int mid)
    {
        Pair<Byte[],Byte[]> res=new Pair<>(new Byte[mid],new Byte[arr.length-mid]);
        for(int i=0;i<mid;i++)  res.a[i]=arr[i];
        for(int i=mid;i<arr.length;i++)
            res.b[i-mid]=arr[i];
        return res;
    }
    public static Pair<List<Short>,List<Short>> shortListSplitAt(List<Short> arr,int mid)
    {
        Pair<List<Short>,List<Short>> res=new Pair<>(null,null);
        res.a=arr.subList(0,mid);
        res.b=arr.subList(mid,arr.size());
        return res;
    }
}
