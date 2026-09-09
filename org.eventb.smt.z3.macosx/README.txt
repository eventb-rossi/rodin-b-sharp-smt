os/macosx/x86_64/z3
-------------------

Downloaded from the official Z3 4.5.0 release:

  https://github.com/Z3Prover/z3/releases/download/z3-4.5.0/z3-4.5.0-x64-osx-10.11.6.zip

and taken from bin/z3 in that archive.  It is byte-identical to the published
binary (sha256 658b81d828380d9ea538401b079e0abb0eac42f01a55a6c13104a16aed6788b4).
Links only libSystem and libc++; minimum macOS 10.11.

(An earlier README said this came from the 4.4.1 release.  It did not -- the
binary's own banner, its Bundle-Version and the checksum above all say 4.5.0.)

os/macosx/aarch64/z3
--------------------

Built from the 4.5.0 sources by build.sh, on macOS 26.6 / Apple M4 with Apple
clang 21.  Z3 4.5.0 predates Apple Silicon, so there is no official build and
the version is pinned; see z3-4.5.0-fallthrough.patch for the one source change
a current clang requires.

Links only libSystem and libc++, the same set as the Intel binary, and carries
the linker's ad-hoc signature (arm64 macOS will not run unsigned code).

Checked against the Intel binary under Rosetta 2 on ground, uninterpreted-
function, array and quantified benchmarks with :produce-unsat-cores: verdicts
and unsat cores came back byte-identical on every one.

Which one is used
-----------------

Eclipse resolves the plug-in's "$os$/z3" through os/macosx/<arch>, so Apple
Silicon gets the native arm64 binary and Intel gets the x86_64 one.  If the
arm64 binary is ever missing, BundledFileExtractor retries the lookup with
$arch$ overridden to x86_64 and falls back to Rosetta 2 -- which Apple removes
in macOS 28.
