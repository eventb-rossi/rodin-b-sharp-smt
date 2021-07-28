/*******************************************************************************
 * Copyright (c) 2011, 2021 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *******************************************************************************/
package org.eventb.smt.tests.acceptance;

import java.util.Arrays;
import java.util.List;

import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.smt.tests.ConfigProvider;
import org.junit.Test;

/**
 * This class contains acceptance tests dedicated to solver runs, which are,
 * tests to check that a solver is ran correctly on the entire chain call.
 * 
 * One shall put in this class such tests, whatever is the solver, but with
 * SMT-LIB 2.0 benchmarks translated with PP.
 * 
 * @author Yoann Guyot
 * 
 */
public abstract class SolverRunWithPPV2_0Tests extends SolverRunTests {

	public SolverRunWithPPV2_0Tests(ConfigProvider provider) {
		super(provider);
	}

	@Test
	public void unsatCall() {
		final List<String> hyps = Arrays.asList( //
				"x < y", //
				"y < z");

		doTest("unsat", hyps, "x < z", arith_te, VALID);
	}

	@Test
	public void truePred() {
		final List<String> hyps = Arrays.asList( //
				"b = TRUE", //
				"c ≠ FALSE");

		doTest("true_pred", hyps, "b = c", arith_te, VALID);
	}

	@Test
	public void boolSet2() {
		final List<String> hyps = Arrays.asList( //
				"b↦c ∈ BOOL×BOOL", //
				"b↦c = TRUE↦FALSE");

		doTest("bool_set2", hyps, "b = TRUE", arith_te, VALID);
	}

	@Test
	public void setsEquality() {
		final ITypeEnvironment te = mTypeEnvironment("p", "ℙ(ℤ)", "q", "ℙ(ℤ)");

		final List<String> hyps = Arrays.asList( //
				"p ∈ ℙ({1})", //
				"p ≠ ∅", //
				"q ∈ ℙ({1})", //
				"q ≠ ∅");

		doTest("sets_equality", hyps, "p = q", te, VALID);
	}

	/**
	 * Regression test for bug #798. We expect all SMT solvers to prove this very
	 * simple example.
	 */
	@Test
	public void modusTollens() {
		final ITypeEnvironment te = mTypeEnvironment("a", "ℤ");

		final List<String> hyps = Arrays.asList( //
				"a = 0 ∨ a = 1", //
				"a ≠ 0");

		doTest("modus_tollens", hyps, "a = 1", te, VALID);
	}
}
