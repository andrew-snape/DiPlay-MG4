#!/usr/bin/env python3
"""Fail CI if credential containers, private-key blocks or iCloud duplicate files enter the source tree.

iCloud duplicates are tracked files whose stem ends in a space and a number, such as "name 2.md".
"""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
names = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode().split('\0')
blocked_suffixes = {'.pk8', '.p7b', '.pem', '.key', '.p12', '.pfx', '.jks', '.keystore', '.apk', '.aab'}
private_block = re.compile(rb'-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----\s+[A-Za-z0-9+/=\r\n]{40,}')
failures = []
duplicates = []
for name in filter(None, names):
    path = root / name
    if re.search(r' \d+$', Path(name).stem):
        duplicates.append(name)
    if not path.is_file():
        continue
    if '.private' in path.relative_to(root).parts or path.suffix.lower() in blocked_suffixes:
        failures.append(name)
    elif private_block.search(path.read_bytes()):
        failures.append(name)
errors = []
if failures:
    errors.append('Credential or distribution files are forbidden in the public tree:\n' + '\n'.join(failures))
if duplicates:
    errors.append('iCloud duplicate files are forbidden in the tree:\n' + '\n'.join(duplicates))
if errors:
    raise SystemExit('\n'.join(errors))
print('Public tree check passed: no credential containers, private-key blocks or iCloud duplicate files.')
