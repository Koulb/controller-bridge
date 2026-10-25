package app.controllerbridge;
import java.util.*;
public final class ControllerRules {
 public static final class Pad {
  public final String name, descriptor;
  public final int vendor, product, sources;
  public final boolean virtual;
  public Pad(String n,int v,int p,int s,boolean x,String d){name=n;vendor=v;product=p;sources=s;virtual=x;descriptor=d;}
 }
 public static boolean isGamepad(Pad p){return (p.sources&0x401)==0x401||(p.sources&0x1000010)==0x1000010;}
 public static boolean isDualSense(Pad p){return p.vendor==0x054c&&(p.product==0x0ce6||p.product==0x0df2);}
 public static List<Pad> physical(List<Pad> all,String ownDescriptor){
  List<Pad> out=new ArrayList<>();
  for(Pad p:all)if(isGamepad(p)&&!p.virtual&&!p.descriptor.equals(ownDescriptor))out.add(p);
  return out;
 }
}
