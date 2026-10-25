#!/usr/bin/env python3
"""Create a public source bundle from an explicit allowlist; never package keys."""
from pathlib import Path
import hashlib, zipfile, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent
release=root/'release';release.mkdir(exist_ok=True)
files=[root/p for p in ['README.md','AGENTS.md','LICENSE','THIRD_PARTY_NOTICES.txt','build.py','test.py','test_device.py','package_source.py','dependencies.json','.gitignore']]
for directory in ['.github','docs','tests','licenses','third-party-src','app/src/main/java','app/src/main/res']:
    files.extend(p for p in (root/directory).rglob('*') if p.is_file())
files.extend([root/'app/src/main/AndroidManifest.xml',root/'app/src/main/assets/dualsense.jsonl'])
version=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot().get('{http://schemas.android.com/apk/res/android}versionName')
archive=release/f'ControllerBridge-{version}-source.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as out:
    for file in sorted(files):out.write(file,Path('ControllerBridge')/file.relative_to(root))
print(archive)
print('SHA256',hashlib.sha256(archive.read_bytes()).hexdigest())
