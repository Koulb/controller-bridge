#!/usr/bin/env python3
"""Independent Android input-registry check; optional computer-side verification."""
import argparse, os, re, subprocess, sys

def devices(dump, descriptor=None):
    hub = dump.split('Event Hub State:', 1)[1].split('Input Reader State:', 1)[0]
    result = []
    for block in re.split(r'(?m)(?=^    -?\d+: )', hub):
        name = re.search(r'^    -?\d+: (.+)', block)
        identity = re.search(r'vendor=0x([\da-f]+), product=0x([\da-f]+)', block)
        if not name or not identity or not re.search(r'Classes:.*(?:GAMEPAD|JOYSTICK)', block):
            continue
        location = re.search(r'(?m)^      Location: (.*)$', block)
        registered_descriptor = re.search(r'(?m)^      Descriptor: (.*)$', block)
        result.append(dict(name=name[1], vendor=int(identity[1], 16), product=int(identity[2], 16),
                           own=bool((registered_descriptor and registered_descriptor[1]==descriptor) if descriptor else (location and location[1]=='controller-bridge-v1')),
                           enabled='Enabled: true' in block,
                           sony_layout='Vendor_054c_Product_0ce6.kl' in block))
    return result

def verify(pads, expect, controller=None, no_physical_sony=False):
    own = [p for p in pads if p['own']]
    physical = [p for p in pads if not p['own']]
    if expect == 'on':
        assert len(own)==1, f'Expected exactly one app virtual pad; got {len(own)}'
        p=own[0]
        assert (p['vendor'],p['product'])==(0x054c,0x0ce6), 'Wrong virtual Sony identity'
        assert p['enabled'] and p['sony_layout'], 'Virtual pad not enabled with Sony key layout'
        assert physical, 'No physical gamepad detected'
    else:
        assert not own, 'Virtual controller still registered after off'
    if controller:
        assert any(controller.casefold() in p['name'].casefold() for p in physical), 'Expected physical controller absent'
    if no_physical_sony:
        assert not any(p['vendor']==0x054c and p['product'] in (0x0ce6,0x0df2) for p in physical), 'Another DualSense is present; test inconclusive'
    return physical

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('expect', choices=['on','off'])
    parser.add_argument('--adb', default=os.getenv('ADB','adb'))
    parser.add_argument('--controller')
    parser.add_argument('--no-physical-sony', action='store_true')
    parser.add_argument('--descriptor', help='Exact registered descriptor for API-backed controller; required for 0.3')
    args=parser.parse_args()
    try:
        dump=subprocess.check_output([args.adb,'shell','dumpsys','input'],text=True,timeout=15)
        physical=verify(devices(dump,args.descriptor),args.expect,args.controller,args.no_physical_sony)
        print('PASS: virtual DualSense '+args.expect+'; physical: '+(', '.join(p['name'] for p in physical) or 'none'))
    except (AssertionError,ValueError,IndexError,subprocess.SubprocessError) as e:
        print('FAIL:',e,file=sys.stderr);sys.exit(1)
