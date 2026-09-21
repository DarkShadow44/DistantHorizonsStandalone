import os
import platform
import requests
import tarfile
import zipfile
from pathlib import Path


def get_platform_specific_filename():
    system = platform.system()
    machine = platform.machine()

    if system == "Darwin":
        if machine == "arm64":
            return "apple-codesign-*-aarch64-apple-darwin.tar.gz"
        else:
            return "apple-codesign-*-x86_64-apple-darwin.tar.gz"
    elif system == "Linux":
        if machine == "aarch64":
            return "apple-codesign-*-aarch64-unknown-linux-musl.tar.gz"
        else:
            return "apple-codesign-*-x86_64-unknown-linux-musl.tar.gz"
    elif system == "Windows":
        if machine.endswith("64"):
            return "apple-codesign-*-x86_64-pc-windows-msvc.zip"
        else:
            return "apple-codesign-*-i686-pc-windows-msvc.zip"
    else:
        raise Exception(f"Unsupported platform: {system} {machine}")


def download_and_unpack():
    dest_dir = Path("./apple-codesign")

    repo_url = "https://api.github.com/repos/indygreg/apple-platform-rs/releases/latest"
    dest_dir.mkdir(exist_ok=True)

    # Fetch the latest release info from GitHub
    print("Fetching latest release information...")
    response = requests.get(repo_url)
    response.raise_for_status()
    release_data = response.json()

    # Ensure release data has assets
    if "assets" not in release_data:
        raise Exception("Release data does not contain assets.")

    # Determine the correct asset
    platform_filename = get_platform_specific_filename()
    asset = next((asset for asset in release_data["assets"] if asset["name"].startswith("apple-codesign-") and asset["name"].endswith(platform_filename.split("*")[-1])), None)

    if not asset:
        raise Exception(f"No matching asset found for platform: {platform_filename}")

    # Download the archive
    print(f"Downloading {asset['name']}...")
    download_url = asset["browser_download_url"]
    archive_path = dest_dir / asset["name"]

    with requests.get(download_url, stream=True) as r:
        r.raise_for_status()
        with open(archive_path, "wb") as f:
            for chunk in r.iter_content(chunk_size=8192):
                f.write(chunk)

    print(f"Downloaded to {archive_path}")

    # Extract the archive
    print("Extracting archive...")
    temp_extract_dir = dest_dir / "temp_extract"
    temp_extract_dir.mkdir(parents=True, exist_ok=True)

    if archive_path.suffix == ".zip":
        with zipfile.ZipFile(archive_path, "r") as zip_ref:
            zip_ref.extractall(temp_extract_dir)
    elif archive_path.suffixes[-2:] == [".tar", ".gz"]:
        with tarfile.open(archive_path, "r:gz") as tar_ref:
            tar_ref.extractall(temp_extract_dir)
    else:
        raise Exception(f"Unknown archive format: {archive_path}")

    # Move contents of the root directory inside the archive to dest_dir
    root_dir = next(temp_extract_dir.iterdir())  # Assuming only one root directory
    for item in root_dir.iterdir():
        target_path = dest_dir / item.name
        if target_path.exists():
            if target_path.is_dir():
                os.rmdir(target_path)
            else:
                os.remove(target_path)
        item.rename(target_path)

    # Clean up temporary directories
    for item in temp_extract_dir.iterdir():
        if item.is_dir():
            os.rmdir(item)
    temp_extract_dir.rmdir()

    print(f"Extracted to {dest_dir}")

    # Clean up the archive
    os.remove(archive_path)
    print(f"Removed archive {archive_path}")


if __name__ == "__main__":
    try:
        download_and_unpack()
    except Exception as e:
        print(f"Error: {e}")
