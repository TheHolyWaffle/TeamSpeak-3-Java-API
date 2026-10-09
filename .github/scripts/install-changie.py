#!/usr/bin/env python3
"""Install the pinned Changie binary after verifying the archive checksum."""
import hashlib
import io
import pathlib
import platform
import tarfile
import urllib.request

VERSION = "1.26.0"
CHECKSUMS = {
    ("Linux", "x86_64"): ("linux_amd64", "eab168c8287a6e91912e1c02e5260911232d945bfd3c89d8a0e1ace6bb7b6161"),
    ("Linux", "aarch64"): ("linux_arm64", "ec1e542014b5134f1cf86b3a12e86d566ab5ec9bd3901bf66f42a09cd9865b6e"),
    ("Darwin", "arm64"): ("darwin_arm64", "ba1a14e0eb220fb9c7d4dc90af462e7f7178a313cdb1299a17aafbb37838a458"),
    ("Darwin", "x86_64"): ("darwin_amd64", "9a1a4dc8a6901ccd9724b3b9b5ec1354e5038b1b1aa4dceef65976f6462ac7d2"),
}

def main():
    target, checksum = CHECKSUMS[(platform.system(), platform.machine())]
    url = f"https://github.com/miniscruff/changie/releases/download/v{VERSION}/changie_{VERSION}_{target}.tar.gz"
    with urllib.request.urlopen(url, timeout=60) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != checksum:
        raise ValueError("Changie archive checksum mismatch")
    destination = pathlib.Path(".tools/changie")
    destination.parent.mkdir(exist_ok=True)
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as archive:
        destination.write_bytes(archive.extractfile("changie").read())
    destination.chmod(0o755)
    print(destination.resolve())

if __name__ == "__main__":
    main()
