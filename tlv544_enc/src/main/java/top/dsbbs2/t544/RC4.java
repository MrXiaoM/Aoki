package top.dsbbs2.t544;

public class RC4 {
    private final byte[] S = new byte[256];
    private int x;
    private int y;

    public RC4(byte[] key) {
        if (key.length < 1 || key.length > 256) {
            throw new IllegalArgumentException("Invalid key length (1-256 bytes allowed)");
        }

        for (int i = 0; i < 256; i++) {
            S[i] = (byte) i;
        }

        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + S[i] + key[i % key.length]) & 0xFF;
            swap(S, i, j);
        }

        this.x = 0;
        this.y = 0;
    }

    public byte[] encrypt(byte[] plaintext) {
        byte[] ciphertext = new byte[plaintext.length];
        for (int i = 0; i < plaintext.length; i++) {
            x = (x + 1) & 0xFF;
            y = (y + S[x]) & 0xFF;
            swap(S, x, y);
            int t = (S[x] + S[y]) & 0xFF;
            ciphertext[i] = (byte) (plaintext[i] ^ S[t]);
        }
        return ciphertext;
    }

    public byte[] decrypt(byte[] ciphertext) {
        return encrypt(ciphertext); // Encryption and decryption are the same in RC4
    }

    private void swap(byte[] array, int i, int j) {
        byte temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
