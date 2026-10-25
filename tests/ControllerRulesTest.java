package app.controllerbridge;
import java.util.*;
public final class ControllerRulesTest {
 static ControllerRules.Pad p(String n,int v,int p,int s,boolean virtual,String d){return new ControllerRules.Pad(n,v,p,s,virtual,d);}
 static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args){
  int gamepad=0x401, joystick=0x1000010;
  ControllerRules.Pad eight=p("8BitDo",0x2dc8,0x200b,gamepad,false,"eight");
  ControllerRules.Pad xbox=p("Xbox",0x45e,0xb13,joystick,false,"xbox");
  ControllerRules.Pad sony=p("DualSense",0x054c,0x0ce6,gamepad,false,"sony");
  ControllerRules.Pad fake=p("DualSense",0x054c,0x0ce6,gamepad,false,"fake");
  ok(ControllerRules.physical(Collections.emptyList(),"fake").isEmpty(),"no controller");
  ok(ControllerRules.physical(Arrays.asList(eight,xbox),"fake").size()==2,"any brand / multiple");
  ok(ControllerRules.isDualSense(sony),"real DualSense");
  ok(ControllerRules.isDualSense(p("Edge",0x054c,0x0df2,gamepad,false,"edge")),"DualSense Edge");
  ok(!ControllerRules.isDualSense(p("DualSense",0x1234,1,gamepad,false,"other")),"name alone insufficient");
  ok(ControllerRules.physical(Arrays.asList(fake,eight),"fake").size()==1,"fake excluded");
  ok(ControllerRules.physical(Arrays.asList(fake,sony),"fake").get(0)==sony,"real Sony preserved");
  ok(ControllerRules.physical(Arrays.asList(p("Keyboard",0,0,0x101,false,"keyboard"),p("Sensor",0x054c,0x0ce6,0x4000000,false,"sensor")),"fake").isEmpty(),"keyboard/sensor excluded");
  ok(ControllerRules.physical(Arrays.asList(p("Android virtual",0,0,gamepad,true,"virtual")),"fake").isEmpty(),"OS virtual excluded");
  System.out.println("PASS: 9 controller classification checks");
 }
}
