import sys
import lief
import subprocess
import download_codesign
from pathlib import Path

# Parse the input binary & xit if binary is invalid
output_path = sys.argv[1]
binary = lief.parse(sys.stdin.buffer.read())
if binary is None:
    exit(1)

# Remove signature from Mac binaries
if isinstance(binary, lief.MachO.Binary):
    binary.remove_signature()

# Write the modified binary to the output path
binary.write(output_path)

# Sign Mac binaries (required to make them usable because apple)
if isinstance(binary, lief.MachO.Binary):
    print(f"Signing {output_path}...")

    # Check if the Apple code-signing files are available, if not, download them
    if not Path("./apple-codesign/COPYING").exists():
        download_codesign.download_and_unpack()

    # Run the code-signing process
    sign_process = subprocess.Popen(["./apple-codesign/rcodesign", "sign", output_path], shell=False,
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    sign_process.wait()
