#!/usr/bin/env sh
set -e

WRAPPER_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
GRADLE_VERSION="8.7"
DIST_NAME="gradle-${GRADLE_VERSION}-bin"
DIST_URL="https://services.gradle.org/distributions/${DIST_NAME}.zip"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$WRAPPER_DIR/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/$DIST_NAME"
GRADLE_HOME="$DIST_DIR/gradle-${GRADLE_VERSION}"

if [ ! -d "$GRADLE_HOME" ]; then
  mkdir -p "$DIST_DIR"
  echo "Bootstrapping Gradle ${GRADLE_VERSION}..." >&2
  PYTHON=""
  if command -v python3 >/dev/null 2>&1; then
    PYTHON="python3"
  elif command -v python >/dev/null 2>&1; then
    PYTHON="python"
  fi
  if [ -z "$PYTHON" ]; then
    echo "Python 3 is required to bootstrap Gradle." >&2
    exit 1
  fi
  DIST_URL="$DIST_URL" DIST_DIR="$DIST_DIR" "$PYTHON" - <<'PY'
import os
import pathlib
import urllib.request
import zipfile

url = os.environ["DIST_URL"]
target_dir = pathlib.Path(os.environ["DIST_DIR"])
zip_path = target_dir / "gradle.zip"
os.makedirs(target_dir, exist_ok=True)
print(f"Downloading {url} -> {zip_path}")
with urllib.request.urlopen(url) as resp, open(zip_path, "wb") as fh:
    while True:
        chunk = resp.read(1024 * 256)
        if not chunk:
            break
        fh.write(chunk)
with zipfile.ZipFile(zip_path) as zf:
    zf.extractall(target_dir)
os.remove(zip_path)
PY
fi

exec "$GRADLE_HOME/bin/gradle" -p "$WRAPPER_DIR" "$@"
