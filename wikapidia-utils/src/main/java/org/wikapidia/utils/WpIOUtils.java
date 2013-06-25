package org.wikapidia.utils;

import java.io.*;

public class WpIOUtils {
    /**
     * Deserialize an array of bytes into an object.
     * @param input Serialized stream of bytes
     * @return Deserialized object.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Serializable bytesToObject(byte input[]) throws IOException, ClassNotFoundException {
        return (Serializable) new ObjectInputStream(
                new ByteArrayInputStream(input))
                        .readObject();
    }

    /**
     * Serialize an object into bytes.
     * @param o Object to be serialized.
     * @return Serialized stream of bytes.
     * @throws IOException
     */
    public static byte[] objectToBytes(Serializable o) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(out);
        oo.writeObject(o);
        oo.close();
        return out.toByteArray();
    }
}
