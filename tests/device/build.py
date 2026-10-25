"""Build a separate local device-test APK with the application's signing key."""
from pathlib import Path
import os,subprocess,zipfile,shutil
r=Path(__file__).resolve().parent
main=r.parents[1]
j=next((main/'tools').glob('jdk-*/Contents/Home'))
t=main/'tools/build-tools/android-16'
a=main/'tools/platform/android-36/android.jar'
b=main/'build/device-test';b.mkdir(parents=True,exist_ok=True)
(b/'classes').mkdir(exist_ok=True);(b/'dex').mkdir(exist_ok=True)
env={**os.environ,'JAVA_HOME':str(j),'PATH':str(j/'bin')+os.pathsep+os.environ['PATH']}
def run(*args):subprocess.run(list(map(str,args)),check=True,env=env)
run(t/'aapt2','link','-o',b/'resources.apk','-I',a,'--manifest',r/'AndroidManifest.xml')
run(j/'bin/javac','-source','8','-target','8','-Xlint:-options','-cp',a,'-d',b/'classes',*r.glob('*.java'))
run(j/'bin/jar','cf',b/'code.jar','-C',b/'classes','.')
run(t/'d8','--min-api','30','--lib',a,'--output',b/'dex',b/'code.jar')
shutil.copyfile(b/'resources.apk',b/'unsigned.apk')
with zipfile.ZipFile(b/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write(b/'dex/classes.dex','classes.dex')
run(t/'zipalign','-f','4',b/'unsigned.apk',b/'aligned.apk')
run(t/'apksigner','sign','--ks',main/'signing.p12','--ks-pass','pass:changeit','--out',b/'Tests.apk',b/'aligned.apk')
run(t/'apksigner','verify',b/'Tests.apk')
