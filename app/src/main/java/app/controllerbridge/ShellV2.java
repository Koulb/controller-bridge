package app.controllerbridge;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Android shell v2 framing. Raw mode gives uinput a pipe with reliable EOF. */
public final class ShellV2 {
    private ShellV2() {}
    public static void writeStdin(OutputStream out, byte[] data) throws IOException {
        byte[] packet = new byte[data.length + 5];
        packet[0] = 0;
        for (int i = 0; i < 4; i++) packet[i + 1] = (byte)(data.length >>> (8 * i));
        System.arraycopy(data, 0, packet, 5, data.length);
        out.write(packet);
        out.flush();
    }
    public static String drain(InputStream input) throws IOException {
        DataInputStream in = new DataInputStream(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (true) {
            int id = in.readUnsignedByte();
            int length = Integer.reverseBytes(in.readInt());
            if (length < 0 || length > 1024 * 1024) throw new IOException("Invalid shell packet");
            byte[] data = new byte[length];
            in.readFully(data);
            if (id == 1 || id == 2) output.write(data, 0, Math.min(length, 4096 - output.size()));
            if (id == 3) return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
