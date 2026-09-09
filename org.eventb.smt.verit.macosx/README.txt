os/macosx/x86_64/veriT
----------------------

Built from veriT stable2016 with veriT-stable2016.patch, by the recipe
below.  Links libSystem alone.  Minimum macOS 10.11.

os/macosx/aarch64/veriT
-----------------------

Built by build.sh on macOS 26.6 / Apple M4 with Apple clang 21.  Same
source and the same patch, plus veriT-stable2016-implicit-decls.patch:
clang 16 and later reject the implicit function declarations the 2016
code relies on.  The bundled GMP pin is moved from 6.0.0a to 6.3.0,
because 6.0.0a has no aarch64-darwin support; it is still linked
statically, so this binary also depends on libSystem alone.  It carries
the linker's ad-hoc signature, which arm64 macOS requires.

Checked against the Intel binary under Rosetta 2 on ground,
uninterpreted-function and propositional benchmarks with named
hypotheses: the proof text -- and so the unsat core the plug-in greps
out of it -- came back byte-identical on every one.

Which one is used
-----------------

Eclipse resolves the plug-in's "$os$/veriT" through os/macosx/<arch>,
so Apple Silicon gets the native arm64 binary and Intel gets the x86_64
one.

--------------------------------------------------------------------
The original Intel recipe, kept for provenance:

Compiling veriT on Mac OS X
---------------------------

Fetch the following tarballs:

  http://www.verit-solver.org/distrib/veriT-stable2016.tar.gz

Make sure that the GMP library is not installed locally.

Uncompress the veriT tarball and enter the commands:

  cd veriT-stable2016
  patch -p1 < ../veriT-stable2016.patch
  autoconf
  ./configure

Ensure that you see the line

  config.status: executing extern-gmp commands

If you do not see it, it means that you have a libgmp installed on your
computer.  Deactivate it (e.g., brew unlink gmp).

  make

The binary file `veriT` is the compiled solver.  Check its dependencies with

  otool -L veriT

This shall answer something like

  veriT:
	/usr/lib/libSystem.B.dylib (...)

with no dependency beyond the system library.
