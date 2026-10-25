#!/usr/bin/env python3
"""Build the standalone APK using JDK 17+ and Android SDK build-tools 36.0.0."""
import hashlib,json,os,pathlib,shutil,subprocess,urllib.request,zipfile
R=pathlib.Path(__file__).resolve().parent
B=R/'build';V=R/'vendor';B.mkdir(exist_ok=True);V.mkdir(exist_ok=True)
local_jdk=list((R/'tools').glob('jdk-*/Contents/Home'))
J=pathlib.Path(os.environ.get('JAVA_HOME',str(local_jdk[0]) if local_jdk else '/usr/lib/jvm/default-java'))
sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/nonexistent')))
T=sdk/'build-tools/36.0.0';A=sdk/'platforms/android-36/android.jar'
if not T.exists():T=R/'tools/build-tools/android-16';A=R/'tools/platform/android-36/android.jar'
env={**os.environ,'JAVA_HOME':str(J),'PATH':str(J/'bin')+os.pathsep+os.environ['PATH']}
def run(*args):subprocess.run([str(x) for x in args],check=True,env=env)
lock=json.loads((R/'dependencies.json').read_text())
jars=[];native={}
for name,entry in lock.items():
 p=V/name
 if not p.exists():urllib.request.urlretrieve(entry['url'],p)
 if hashlib.sha256(p.read_bytes()).hexdigest()!=entry['sha256']:raise RuntimeError('Dependency checksum mismatch: '+name)
 if p.suffix=='.aar':
  with zipfile.ZipFile(p) as z:
   jar=B/(p.stem+'.jar');jar.write_bytes(z.read('classes.jar'));jars.append(jar)
   for f in z.namelist():
    if f.startswith('jni/') and f.endswith('.so'):native['lib/'+f[4:]]=z.read(f)
 else:jars.append(p)
classes=B/'classes';generated=B/'generated'
for directory in [classes,generated,B/'dex']:
 if directory.exists():shutil.rmtree(directory)
 directory.mkdir()
assets=R/'app/src/main/assets'
shutil.copyfile(R/'THIRD_PARTY_NOTICES.txt',assets/'THIRD_PARTY_NOTICES.txt')
shutil.copytree(R/'licenses',assets/'licenses',dirs_exist_ok=True)
run(T/'aapt2','compile','--dir',R/'app/src/main/res','-o',B/'resources.zip')
run(T/'aapt2','link','-o',B/'resources.apk','-I',A,'--manifest',R/'app/src/main/AndroidManifest.xml','--java',generated,'--auto-add-overlay','-A',R/'app/src/main/assets',B/'resources.zip')
src=list((R/'app/src/main/java').rglob('*.java'))+list(generated.rglob('*.java'))
run(J/'bin/javac','-source','8','-target','8','-Xlint:-options','-cp',os.pathsep.join(map(str,[A]+jars)),'-d',classes,*src)
run(J/'bin/jar','cf',B/'app.jar','-C',classes,'.')
dex=B/'dex';dex.mkdir(exist_ok=True)
run(T/'d8','--release','--min-api','30','--lib',A,'--output',dex,B/'app.jar',*jars)
unsigned=B/'unsigned.apk';shutil.copyfile(B/'resources.apk',unsigned)
with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as z:
 for p in dex.glob('classes*.dex'):z.write(p,p.name)
 for name,data in native.items():z.writestr(name,data)
run(T/'zipalign','-f','4',unsigned,B/'aligned.apk')
key=R/'signing.p12'
if not key.exists():run(J/'bin/keytool','-genkeypair','-keystore',key,'-storepass','changeit','-keypass','changeit','-alias','controllerbridge','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Controller Bridge Local Build')
run(T/'apksigner','sign','--ks',key,'--ks-pass','pass:changeit','--out',B/'ControllerBridge.apk',B/'aligned.apk')
run(T/'apksigner','verify','--verbose',B/'ControllerBridge.apk')
print('APK:',B/'ControllerBridge.apk')
