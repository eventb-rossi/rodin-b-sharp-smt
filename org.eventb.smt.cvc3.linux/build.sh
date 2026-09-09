#!/bin/bash
#
#  Build of cvc3 for linux/x86_64.  Writes os/linux/cvc3.
#
#  Three things below look wrong and are not.  The container is Debian 8 because
#  the compiler has to predate GCC 6; zchaff is deleted before configuring
#  because its licence forbids distribution; and the binary is checked, not just
#  compiled, because a bad build of CVC3 still links, runs and reports the right
#  version.  README tells the whole story -- read it before changing any of this.
#
#  Usage: ./build.sh [cvc3-2.4.1.tar.gz]
#

set -eu

CVC3_ARCHIVE="${1:-cvc3-2.4.1.tar.gz}"
CVC3_URL="https://cs.nyu.edu/acsys/cvc3/releases/2.4.1/cvc3-2.4.1.tar.gz"
IMAGE="docker.io/library/debian:8"
RUNTIME="$(command -v podman || command -v docker)"

# Rootless podman maps the container's root onto the invoking user, so the file
# it writes into the bind mount already belongs to us.  Rootful docker leaves it
# owned by root, and the chmod at the end would then fail under set -e, so hand
# the ids in and let the container hand ownership back.
case "$RUNTIME" in
    *docker) HOST_IDS="$(id -u):$(id -g)" ;;
    *)       HOST_IDS= ;;
esac

test -f "$CVC3_ARCHIVE" || curl -fSL -o "$CVC3_ARCHIVE" "$CVC3_URL"

"$RUNTIME" run --rm -e HOST_IDS="$HOST_IDS" -v "$PWD":/w:z -w /w "$IMAGE" \
    bash -s "$CVC3_ARCHIVE" <<'INNER'
set -eu
CVC3_ARCHIVE=$1

# jessie is archived: no valid signatures, no valid Release dates
cat > /etc/apt/sources.list <<L
deb [trusted=yes] http://archive.debian.org/debian jessie main
deb [trusted=yes] http://archive.debian.org/debian-security jessie/updates main
L
echo 'Acquire::Check-Valid-Until "false";' > /etc/apt/apt.conf.d/99no-check-valid

apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get install -y --allow-unauthenticated \
    g++ make libgmp-dev flex bison perl

cd /tmp
tar zxf "/w/$CVC3_ARCHIVE"
cd cvc3-2.4.1
rm -v src/sat/xchaff*
./configure --disable-zchaff
make -j"$(nproc)"

cp bin/x86_64-linux-gnu/static/cvc3 /w/os/linux/cvc3
if [ -n "${HOST_IDS:-}" ]; then chown "$HOST_IDS" /w/os/linux/cvc3; fi
INNER

chmod 755 os/linux/cvc3
file os/linux/cvc3
test "$(strings -a os/linux/cvc3 | grep -c CSolver)" -eq 0 \
    || { echo "FAILED: zchaff code is present in the binary" >&2; exit 1; }
echo 'zchaff check: clean'
