#!/usr/bin/env python3
"""Run host-side controller classification and shell protocol tests."""
import os,pathlib,shutil,subprocess
root=pathlib.Path(__file__).resolve().parent
local=list((root/'tools').glob('jdk-*/Contents/Home'))
java_home=pathlib.Path(os.environ.get('JAVA_HOME',str(local[0]) if local else '/usr/lib/jvm/default-java'))
out=root/'test-classes'
if out.exists():shutil.rmtree(out)
out.mkdir()
src=root/'app/src/main/java/app/controllerbridge'
subprocess.run([str(java_home/'bin/javac'),'-d',str(out),str(src/'ControllerRules.java'),str(src/'ShellV2.java'),str(src/'HelperPolicy.java'),*[str(p) for p in (root/'tests').glob('*.java')]],check=True)
for test in ['ControllerRulesTest','ShellV2Test','HelperPolicyTest']:
    subprocess.run([str(java_home/'bin/java'),'-cp',str(out),'app.controllerbridge.'+test],check=True)
