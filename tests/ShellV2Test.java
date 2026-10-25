package app.controllerbridge;
import java.io.*;
import java.util.Arrays;
public class ShellV2Test {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ShellV2.writeStdin(out, new byte[]{65,66,67});
        if (!Arrays.equals(out.toByteArray(), new byte[]{0,3,0,0,0,65,66,67})) throw new AssertionError("stdin framing");
        byte[] packets = {1,2,0,0,0,65,66,2,1,0,0,0,67,3,1,0,0,0,0};
        InputStream fragmented = new FilterInputStream(new ByteArrayInputStream(packets)) {
            @Override public int read(byte[] b,int offset,int length)throws IOException {return super.read(b,offset,Math.min(length,1));}
        };
        if (!"ABC".equals(ShellV2.drain(fragmented))) throw new AssertionError("fragmented output / exit");
        try {ShellV2.drain(new ByteArrayInputStream(new byte[]{1,4,0,0,0,65}));throw new AssertionError("truncation accepted");}
        catch (EOFException expected) {}
        try {ShellV2.drain(new ByteArrayInputStream(new byte[]{1,-1,-1,-1,127}));throw new AssertionError("oversized packet accepted");}
        catch (IOException expected) {}
        System.out.println("PASS: 4 shell protocol checks");
    }
}
