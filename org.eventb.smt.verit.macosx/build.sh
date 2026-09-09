#!/bin/bash
#
#  Build of veriT stable2016 for macOS, for the architecture of the machine it
#  runs on.  The arm64 binary in os/macosx/aarch64 was produced by this script
#  on macOS 26.6 / Apple M4 with Apple clang 21; the x86_64 binary predates it
#  and came from the recipe kept in README.txt.
#
#  Requires autoconf, which stock macOS does not ship (brew install autoconf, or
#  use a GitHub macos-14+ runner, which has it).  Everything else -- clang, make,
#  flex, bison -- comes with the Command Line Tools.
#
#  Two patches are applied:
#
#    veriT-stable2016.patch                 the negative-memset guard, from the
#                                           original macOS port
#    veriT-stable2016-implicit-decls.patch  four missing prototypes; clang 16+
#                                           rejects implicit declarations, so
#                                           the source does not build without it
#
#  veriT links GMP dynamically if configure finds one on the system, and
#  otherwise builds extern/gmp-local statically and links that.  The second path
#  is the one we want -- it is what makes the binary depend on libSystem alone --
#  so any system GMP has to be out of the way.  extern/gmp-local pins GMP 6.0.0a
#  (2014), which has no aarch64-darwin support, so the pin is bumped to 6.3.0.
#
#  Usage: ./build.sh
#

set -eu

test "$(uname -s)" = Darwin || { echo "must run on macOS" >&2; exit 1; }
command -v autoconf >/dev/null || { echo "autoconf is required (brew install autoconf)" >&2; exit 1; }

# Eclipse names the ARM architecture "aarch64" (Platform.ARCH_AARCH64), and
# that is the os/macosx/<arch> directory the fragment is resolved through.
# uname(1) says "arm64", so the two have to be mapped.
case "$(uname -m)" in
    arm64)  ARCH=aarch64 ;;
    x86_64) ARCH=x86_64  ;;
    *) echo "unsupported architecture $(uname -m)" >&2; exit 1 ;;
esac
HERE="$PWD"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

curl -sSL -o "$WORK/veriT.tar.gz" http://www.verit-solver.org/distrib/veriT-stable2016.tar.gz
tar xzf "$WORK/veriT.tar.gz" -C "$WORK"
cd "$WORK/veriT-stable2016"

patch -p1 < "$HERE/veriT-stable2016.patch"
patch -p1 < "$HERE/veriT-stable2016-implicit-decls.patch"

sed -i '' -e 's/^DIR=gmp-.*/DIR=gmp-6.3.0/' \
          -e 's/^SOURCES:=gmp-.*/SOURCES:=gmp-6.3.0.tar.bz2/' extern/gmp-local/Makefile
curl -sSL -o extern/gmp-local/gmp-6.3.0.tar.bz2 https://ftp.gnu.org/gnu/gmp/gmp-6.3.0.tar.bz2

brew unlink gmp >/dev/null 2>&1 || true

autoconf
./configure > configure.log 2>&1
grep -q 'executing extern-gmp commands' configure.log \
    || { echo "configure found a system libgmp; the binary would not be self-contained" >&2; exit 1; }

# GMP first and on its own: the veriT Makefile does not order itself against it,
# so a parallel build links veriT-SAT before libgmp.a exists.
make extern/gmp
make -j"$(sysctl -n hw.ncpu)"

cd "$HERE"
mkdir -p "os/macosx/$ARCH"
cp "$WORK/veriT-stable2016/veriT" "os/macosx/$ARCH/veriT"
chmod 755 "os/macosx/$ARCH/veriT"

out="os/macosx/$ARCH/veriT"

file "$out"
case "$ARCH $(file -b "$out")" in
    "aarch64 Mach-O 64-bit executable arm64"|"x86_64 Mach-O 64-bit executable x86_64") ;;
    *) echo "$out is not a $ARCH binary" >&2; exit 1 ;;
esac

# Linking GMP statically is the whole point of the extern/gmp path above, so
# the binary must come out depending on libSystem and nothing else.
otool -L "$out"
if otool -L "$out" | tail -n +2 | grep -qvE '/usr/lib/libSystem\.B\.dylib'; then
    echo "$out has dynamic dependencies beyond libSystem" >&2
    exit 1
fi

# arm64 macOS refuses unsigned code.  The linker signs ad-hoc, so this only has
# to confirm it; x86_64 carries no such requirement and often has no signature.
if [ "$ARCH" = aarch64 ]; then
    codesign --verify --strict "$out"
fi
