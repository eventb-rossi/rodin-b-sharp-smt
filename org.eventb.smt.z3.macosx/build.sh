#!/bin/bash
#
#  Build of Z3 4.5.0 for macOS, for the architecture of the machine it runs on.
#  The arm64 binary in os/macosx/aarch64 was produced by this script on
#  macOS 26.6 / Apple M4 with Apple clang 21.  There is no official arm64 build
#  of 4.5.0 -- the release predates Apple Silicon -- and the version is pinned,
#  so it has to be built from source.
#
#  Needs nothing beyond the Command Line Tools and python3.  Z3 4.5.0 does carry
#  a CMake build, but under contrib/cmake and needing a bootstrap step; its own
#  scripts/mk_make.py generator runs fine under python3 and is simpler.
#
#  One patch is applied:
#
#    z3-4.5.0-fallthrough.patch   removes a fallthrough annotation that falls
#                                 through to nothing; clang rejects it outright
#
#  Nothing else needs adjusting for arm64.  The generated makefile carries no SSE
#  flags, src/util/hwf.cpp #undefs USE_INTRINSICS under clang so the SSE2
#  intrinsics are never reached, and the -D_AMD64_ that mk_make.py always defines
#  only selects 64-bit pointer sizes and alignments, not x86 instructions.
#
#  Usage: ./build.sh
#

set -eu

test "$(uname -s)" = Darwin || { echo "must run on macOS" >&2; exit 1; }

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

curl -sSL -o "$WORK/z3.tar.gz" \
    https://github.com/Z3Prover/z3/archive/refs/tags/z3-4.5.0.tar.gz
tar xzf "$WORK/z3.tar.gz" -C "$WORK"
cd "$WORK/z3-z3-4.5.0"

patch -p1 < "$HERE/z3-4.5.0-fallthrough.patch"

python3 scripts/mk_make.py
cd build
make -j"$(sysctl -n hw.ncpu)"

./z3 -version

cd "$HERE"
mkdir -p "os/macosx/$ARCH"
cp "$WORK/z3-z3-4.5.0/build/z3" "os/macosx/$ARCH/z3"
chmod 755 "os/macosx/$ARCH/z3"

out="os/macosx/$ARCH/z3"

file "$out"
case "$ARCH $(file -b "$out")" in
    "aarch64 Mach-O 64-bit executable arm64"|"x86_64 Mach-O 64-bit executable x86_64") ;;
    *) echo "$out is not a $ARCH binary" >&2; exit 1 ;;
esac

# libz3 is built alongside, but the z3 executable is self-contained: it must
# link libSystem and libc++ and nothing else.
otool -L "$out"
if otool -L "$out" | tail -n +2 | grep -qvE '/usr/lib/(libSystem\.B|libc\+\+\.1)\.dylib'; then
    echo "$out has dynamic dependencies beyond libSystem and libc++" >&2
    exit 1
fi

# arm64 macOS refuses unsigned code.  The linker signs ad-hoc, so this only has
# to confirm it; x86_64 carries no such requirement and often has no signature.
if [ "$ARCH" = aarch64 ]; then
    codesign --verify --strict "$out"
fi
