package app.controllerbridge;
import android.content.Context;
import android.os.Build;
import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class PhoneAdb extends AbsAdbConnectionManager {
 private volatile io.github.muntashirakon.adb.BoundedPairing pendingPairing;
 public void cancelPairing(){io.github.muntashirakon.adb.BoundedPairing p=pendingPairing;if(p!=null)p.cancel();}
 public boolean pairLocal(int port,String code,java.util.function.BooleanSupplier valid)throws Exception{
  try(io.github.muntashirakon.adb.BoundedPairing p=new io.github.muntashirakon.adb.BoundedPairing("127.0.0.1",port,code.getBytes(java.nio.charset.StandardCharsets.UTF_8),key,certificate,"ControllerBridge")){
   pendingPairing=p;if(!valid.getAsBoolean())p.cancel();p.start();return true;
  }finally{pendingPairing=null;}
 }
 private final PrivateKey key;
 private final java.security.cert.Certificate certificate;
 public static PhoneAdb create(Context c)throws Exception{
  return new PhoneAdb(c.getApplicationContext());
 }
 private PhoneAdb(Context c)throws Exception{
  setApi(Build.VERSION.SDK_INT);setHostAddress("127.0.0.1");setTimeout(12,TimeUnit.SECONDS);setThrowOnUnauthorised(true);
  File dir=c.getNoBackupFilesDir(),kf=new File(dir,"adb-key.pk8"),cf=new File(dir,"adb-cert.der");
  if(kf.exists()&&cf.exists()){
   key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(kf.toPath())));
   certificate=CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(Files.readAllBytes(cf.toPath())));
  }else{
   KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");gen.initialize(2048);java.security.KeyPair pair=gen.generateKeyPair();key=pair.getPrivate();
   X500Name name=new X500Name("CN=Controller Bridge");long now=System.currentTimeMillis();
   certificate=new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(name,new BigInteger(128,new SecureRandom()),new Date(now-60000),new Date(now+315360000000L),name,pair.getPublic()).build(new JcaContentSignerBuilder("SHA256withRSA").build(key)));
   Files.write(kf.toPath(),key.getEncoded());Files.write(cf.toPath(),certificate.getEncoded());
  }
 }
 @Override protected PrivateKey getPrivateKey(){return key;}
 @Override protected java.security.cert.Certificate getCertificate(){return certificate;}
 @Override protected String getDeviceName(){return "ControllerBridge";}
}
