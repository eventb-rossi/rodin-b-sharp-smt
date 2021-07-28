/*******************************************************************************
 * Copyright (c) 2011, 2021 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *     ISP RAS - added test for the axiom of empty set
 *******************************************************************************/
package org.eventb.smt.tests.unit;

import static java.util.Collections.singleton;
import static org.eventb.core.seqprover.transformer.SimpleSequents.make;
import static org.eventb.pptrans.Translator.isInGoal;
import static org.eventb.smt.core.SolverKind.VERIT;
import static org.eventb.smt.core.internal.translation.SMTThroughPP.translateTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.ITypeEnvironmentBuilder;
import org.eventb.core.ast.Predicate;
import org.eventb.core.seqprover.transformer.ISimpleSequent;
import org.eventb.core.seqprover.transformer.SimpleSequents;
import org.eventb.smt.core.internal.ast.SMTBenchmark;
import org.eventb.smt.core.internal.ast.SMTFormula;
import org.eventb.smt.core.internal.ast.SMTSignature;
import org.eventb.smt.core.internal.ast.theories.Logic;
import org.eventb.smt.core.internal.translation.SMTThroughPP;
import org.eventb.smt.tests.AbstractTests;
import org.junit.Test;

/**
 * Ensure that translation from ppTrans produced predicates to SMT-LIB 2.0
 * predicates is correct.
 * 
 * @author Yoann Guyot
 * 
 */
public class TranslationTestsWithPPV2_0 extends AbstractTests {
	protected static final ITypeEnvironment defaultTe;
	public static final Logic defaultLogic;
	static {
		defaultTe = mTypeEnvironment("S", "ℙ(S)", "r", "ℙ(R)", "s", "ℙ(R)",
				"a", "ℤ", "b", "ℤ", "c", "ℤ", "u", "BOOL", "v", "BOOL");
		defaultLogic = Logic.AUFLIAv2_0.getInstance();
	}

	private void testTranslateGoalPP(final ITypeEnvironment te,
			final String inputGoal, final String expectedFormula) {

		final Predicate goalPredicate = parse(inputGoal, te.makeBuilder());

		assertTypeChecked(goalPredicate);

		final ISimpleSequent sequent = make((List<Predicate>) null,
				goalPredicate, goalPredicate.getFactory());

		final SMTThroughPP translator = new SMTThroughPP();
		final SMTBenchmark benchmark = translate(translator, "lemma", sequent);

		final SMTFormula formula = benchmark.getFormula();
		assertEquals(expectedFormula, formula.toString());
	}

	private void testContainsAssumptionsPP(final ITypeEnvironment te,
			final String inputGoal, final List<String> expectedAssumptions) {

		final Predicate goal = parse(inputGoal, te.makeBuilder());

		final ISimpleSequent sequent = make((List<Predicate>) null, goal,
				goal.getFactory());

		assertTypeChecked(goal);

		final SMTThroughPP translator = new SMTThroughPP();
		final SMTBenchmark benchmark = translate(translator, "lemma", sequent);

		final List<SMTFormula> assumptions = benchmark.getAssumptions();
		assertEquals(assumptionsString(assumptions),
				expectedAssumptions.size(), assumptions.size());
		for (final SMTFormula assumption : assumptions) {
			assertTrue(
					expectedAssumptionMessage(expectedAssumptions,
							assumption.toString()),
					expectedAssumptions.contains(assumption.toString()));
		}
	}

	private String expectedAssumptionMessage(
			final List<String> expectedAssumptions, final String assumption) {
		return "Expected these assumptions: " + expectedAssumptions.toString()
				+ ". But found this assumption: " + assumption.toString();
	}

	private static void testTranslationV2_0Default(final String ppPredStr,
			final String expectedSMTNode) {
		testTranslationV2_0(defaultTe, ppPredStr, expectedSMTNode);
	}

	public static void testTypeEnvironmentFuns(final Logic logic,
			final ITypeEnvironment te, final Set<String> expectedFunctions,
			final String predString) {
		final SMTSignature signature = translateTypeEnvironment(logic, te,
				predString);
		testTypeEnvironmentFuns(signature, expectedFunctions, predString);
	}

	public static void testTypeEnvironmentSorts(final Logic logic,
			final ITypeEnvironment te, final Set<String> expectedFunctions,
			final String predString) {
		final SMTSignature signature = translateTypeEnvironment(logic, te,
				predString);
		testTypeEnvironmentSorts(signature, expectedFunctions, predString);
	}

	protected static SMTSignature translateTypeEnvironment(
			final Logic logic, final ITypeEnvironment iTypeEnv,
			final String ppPredStr) throws AssertionError {
		final Predicate ppPred = parse(ppPredStr, iTypeEnv.makeBuilder());

		final ISimpleSequent sequent = make((List<Predicate>) null, ppPred,
				ppPred.getFactory());

		assertTrue(producePPTargetSubLanguageError(ppPred), isInGoal(sequent));

		return translateTE(logic, sequent);
	}

	private static String producePPTargetSubLanguageError(
			final Predicate predicate) {
		return "\'" + predicate
				+ "\' is not in the target sub-language of the PP translator.";
	}

	/**
	 * Parses a Predicate Calculus formula, (builds hypotheses and goal) and
	 * tests its SMT-LIB translation
	 * 
	 * @param iTypeEnv
	 *            Input type environment
	 * @param ppPredStr
	 *            String representation of the input predicate
	 * @param expectedSMTNode
	 *            String representation of the expected node
	 */
	private static void testTranslationV2_0(final ITypeEnvironment iTypeEnv,
			final String ppPredStr, final String expectedSMTNode) {
		final Predicate ppPred = parse(ppPredStr, iTypeEnv.makeBuilder());

		final ISimpleSequent sequent = make((List<Predicate>) null, ppPred,
				ppPred.getFactory());

		assertTrue(producePPTargetSubLanguageError(ppPred), isInGoal(sequent));

		testTranslationV2_0(ppPred, expectedSMTNode, VERIT.toString());
	}

	/**
	 * Tests the SMT-LIB translation with the given Predicate Calculus formula
	 * 
	 * @param ppPred
	 *            Input Predicate Calculus formula
	 * @param expectedSMTNode
	 *            String representation of the expected node
	 */
	private static void testTranslationV2_0(final Predicate ppPred,
			final String expectedSMTNode, final String solver) {
		final StringBuilder sb = new StringBuilder();
		SMTThroughPP.translate(ppPred).toString(sb, -1, false);
		final String actualSMTNode = sb.toString();
		if (!expectedSMTNode.equals(actualSMTNode)) {
			System.out.println(translationMessage(ppPred, actualSMTNode));
		}
		assertEquals(expectedSMTNode, actualSMTNode);
	}

	private static final String translationMessage(final Predicate ppPred,
			final String smtNode) {
		final StringBuilder sb = new StringBuilder();
		sb.append("\'");
		sb.append(ppPred.toString());
		sb.append("\' was translated in \'");
		sb.append(smtNode);
		sb.append("\'");
		return sb.toString();
	}

	private void doTeTest(final String lemmaName,
			final List<Predicate> parsedHypotheses, final Predicate parsedGoal,
			final Set<String> expectedFuns, final Set<String> expectedPreds,
			final Set<String> expectedSorts) throws IllegalArgumentException {
		// Type check goal and hypotheses
		assertTypeChecked(parsedGoal);
		for (final Predicate parsedHypothesis : parsedHypotheses) {
			assertTypeChecked(parsedHypothesis);
		}

		final ISimpleSequent sequent = SimpleSequents.make(parsedHypotheses,
				parsedGoal, parsedGoal.getFactory());

		final SMTThroughPP translator = new SMTThroughPP();
		final SMTBenchmark benchmark = translate(translator, lemmaName, sequent);

		final SMTSignature signature = benchmark.getSignature();

		testTypeEnvironmentSorts(signature, expectedSorts, "");
		testTypeEnvironmentFuns(signature, expectedFuns, "");
		testTypeEnvironmentPreds(signature, expectedPreds, "");
	}
	
	protected void doTTeTest(final String lemmaName,
			final List<String> inputHyps, final String inputGoal,
			final ITypeEnvironment te, final Set<String> expectedFuns,
			final Set<String> expectedPreds, final Set<String> expectedSorts) {
		final List<Predicate> hypotheses = new ArrayList<Predicate>();
		final ITypeEnvironmentBuilder teb = te.makeBuilder();
		for (final String hyp : inputHyps) {
			hypotheses.add(parse(hyp, teb));
		}

		final Predicate goal = parse(inputGoal, teb);

		doTeTest(lemmaName, hypotheses, goal, expectedFuns, expectedPreds,
				expectedSorts);
	}

	/**
	 * "pred-bin" in ppTrans abstract syntax
	 */
	@Test
	public void testPredBinop() {
		/**
		 * limp
		 */
		testTranslationV2_0Default("(a < b ∧ b < c) ⇒ a < c",
				"(=> (and (< a b) (< b c)) (< a c))");
		/**
		 * leqv
		 */
		testTranslationV2_0Default("(a ≤ b ∧ b ≤ a) ⇔ a = b",
				"(= (and (<= a b) (<= b a)) (= a b))");
	}

	/**
	 * "pred-ass"
	 */
	@Test
	public void testPredAssop() {
		/**
		 * land
		 */
		testTranslationV2_0Default("(a = b) ∧ (u = v)", "(and (= a b) (= u v))");
		/**
		 * land (multiple predicates)
		 */
		testTranslationV2_0Default("(a = b) ∧ (u = v) ∧ (r = s)",
				"(and (= a b) (= u v) (forall ((x R)) (= (MS x r) (MS x s))))");
		/**
		 * lor
		 */
		testTranslationV2_0Default("(a = b) ∨ (u = v)", "(or (= a b) (= u v))");
		/**
		 * lor (multiple predicates)
		 */
		testTranslationV2_0Default("(a = b) ∨ (u = v) ∨ (r = s)",
				"(or (= a b) (= u v) (forall ((x R)) (= (MS x r) (MS x s))))");
	}

	/**
	 * "pred-una"
	 */
	@Test
	public void testPredUna() {
		testTranslationV2_0Default("¬ ((a ≤ b ∧ b ≤ c) ⇒ a < c)",
				"(not (=> (and (<= a b) (<= b c)) (< a c)))");
	}

	/**
	 * When a set is used on the left hand side of a membership, the translator
	 * must not use this set as a monadic membership predicate.
	 * 
	 * In this example, we expect the membership of the set PS to be translated
	 * with a monadic predicate, whereas, the membership of the set S must be
	 * translated with the generalised membership predicate 'MS'.
	 */
	@Test
	public void testMonadicMembershipPredicate() {
		final ITypeEnvironment te = mTypeEnvironment("PS", "ℙ(ℙ(ℤ))", "S",
				"ℙ(ℤ)", "x", "ℤ", "PPS", "ℙ(ℙ(ℤ) × ℙ(ℤ))");

		testTranslationV2_0(te, "S ∈ PS ⇒ ¬ x ∈ S",
				"(=> (PS S) (not (MS x S)))");

		testTranslationV2_0(te, "S ↦ S ∈ PPS ⇒ ¬ x ∈ S",
				"(=> (PPS S S) (not (MS x S)))");
	}

	/**
	 * "pred-quant"
	 */
	@Test
	public void testQuantifiers() {
		final ITypeEnvironment te = mTypeEnvironment("RR", "r ↔ s");

		/**
		 * forall
		 */
		testTranslationV2_0Default("∀x·x∈s", "(forall ((x R)) (s x))");
		/**
		 * forall (multiple identifiers)
		 */
		testTranslationV2_0(te, "∀x,y·x↦y∈RR",
				"(forall ((x r) (y s)) (RR x y))");

		/**
		 * bound set
		 */
		testTranslationV2_0Default("∃ x ⦂ ℤ, X ⦂ ℙ(ℤ) · x ∈ X",
				"(exists ((x Int) (X PZ)) (MS x X))");

	}

	@Test
	public void testExists() {
		/**
		 * exists
		 */
		testTranslationV2_0Default("∃x·x∈s", "(exists ((x R)) (s x))");
		/**
		 * exists (multiple identifiers)
		 */
		testTranslationV2_0Default("∃x,y·x∈s∧y∈s",
				"(exists ((x R) (y R)) (and (s x) (s y)))");
	}

	/**
	 * "pred-lit"
	 */
	@Test
	public void testPredLit() {
		/**
		 * btrue
		 */
		testTranslationV2_0Default("⊤", "true");
		/**
		 * bfalse
		 */
		testTranslationV2_0Default("⊥", "false");
	}

	/**
	 * "pred-rel"
	 */
	@Test
	public void testPredRelop() {
		/**
		 * equal (identifiers of type ℤ)
		 */
		testTranslationV2_0Default("a = b", "(= a b)");
		/**
		 * equal (integer numbers)
		 */
		testTranslationV2_0Default("42 − 1 + 1 = 42", "(= (+ (- 42 1) 1) 42)");
		/**
		 * lt
		 */
		testTranslationV2_0Default("a < b", "(< a b)");
		/**
		 * le
		 */
		testTranslationV2_0Default("a ≤ b", "(<= a b)");
		/**
		 * gt
		 */
		testTranslationV2_0Default("a > b", "(> a b)");
		/**
		 * ge
		 */
		testTranslationV2_0Default("a ≥ b", "(>= a b)");
	}

	/**
	 * Arithmetic expressions binary operations: cf. "a-expr-bin"
	 */
	@Test
	public void testArithExprBinop() {
		/**
		 * minus
		 */
		testTranslationV2_0Default("a − b = c", "(= (- a b) c)");
		/**
		 * equal (a-expr-bin)
		 */
		testTranslationV2_0Default("a − b = a − c", "(= (- a b) (- a c))");
	}

	@Test
	public void testArithExprBinopUnsupported() {
		/**
		 * expn
		 */
		testTranslationV2_0Default("a ^ b = c", "(= (expn a b) c)");
		/**
		 * div
		 */
		testTranslationV2_0Default("a ÷ b = c", "(= (divi a b) c)");
		/**
		 * mod
		 */
		testTranslationV2_0Default("a mod b = c", "(= (mod a b) c)");
	}

	/**
	 * Arithmetic expressions associative operations: cf. "a-expr-ass"
	 */
	@Test
	public void testArithExprAssnop() {
		/**
		 * plus
		 */
		testTranslationV2_0Default("a + c + b = a + b + c",
				"(= (+ a c b) (+ a b c))");
		/**
		 * mul
		 */
		testTranslationV2_0Default("a ∗ b ∗ c = a ∗ c ∗ b",
				"(= (* a b c) (* a c b))");
	}

	/**
	 * Arithmetic expressions unary operations: cf. "a-expr-una"
	 */
	@Test
	public void testArithExprUnop() {
		/**
		 * uminus (right child)
		 */
		testTranslationV2_0Default("a = −b", "(= a (- b))");
		/**
		 * uminus (left child)
		 */
		testTranslationV2_0Default("−a = b", "(= (- a) b)");
	}

	/**
	 * "pred-in" This test should not happen with ppTrans; The
	 */

	@Test
	public void testPredIn() {
		final ITypeEnvironment te = mTypeEnvironment(defaultTe, //
				"A", "ℙ(ℤ)", "AB", "ℤ ↔ ℤ");

		testTranslationV2_0Default("a ∈ A", "(A a)");
		testTranslationV2_0(te, "a↦b ∈ AB", "(AB a b)");
		testTranslationV2_0(te, "a↦BOOL↦BOOL ∈ X", "(X a BOOLS BOOLS)");
	}

	@Test
	public void testPredIn2() {
		testTranslationV2_0Default("a↦BOOL↦a ∈ Y", "(Y a BOOLS a)");
	}

	@Test
	public void testPredInInt() {
		final ITypeEnvironment te = mTypeEnvironment(defaultTe, //
				"int", "S", "SPZ", "S ↔ ℙ(ℤ)", "AZ", "ℤ ↔ ℙ(ℤ)");

		/**
		 * Through these unit tests, the integer axiom is not generated. That's
		 * why the membership predicate symbol 'MS' is not already in use, and
		 * can be expected here.
		 */
		testTranslationV2_0(te, "INTS↦ℤ ∈ SPZ", "(SPZ INTS0 INTS)");
		testTranslationV2_0(te, "a↦ℤ ∈ AZ", "(AZ a INTS)");
	}

	/**
	 * "pred-setequ"
	 */
	@Test
	public void testPredSetEqu() {
		testTranslationV2_0Default("r = s",
				"(forall ((x R)) (= (MS x r) (MS x s)))");
	}

	/**
	 * "pred-boolequ"
	 */
	@Test
	public void testPredBoolEqu() {
		testTranslationV2_0Default("u = v", "(= u v)");
		testTranslationV2_0Default("u = TRUE", "u");
		testTranslationV2_0Default("TRUE = u", "u");
	}

	/**
	 * "pred-identequ"
	 */
	@Test
	public void testPredIdentEqu() {
		final ITypeEnvironment te = mTypeEnvironment("p", "S", "q", "S");

		testTranslationV2_0(te, "p = q", "(= p q)");
	}

	@Test
	public void testTRUELit() {
		final ITypeEnvironment te = mTypeEnvironment(//
				"f", "ℙ(BOOL)", "x", "BOOL");

		testTranslationV2_0(te, "x ∈ f", "(f x)");
	}

	@Test
	public void testTRUEPred() {
		final ITypeEnvironment te = mTypeEnvironment(//
				"B", "ℙ(BOOL)", "b", "BOOL", "c", "BOOL");

		/**
		 * Formulas containing boolean equalities and memberships involving
		 * boolean values.
		 */
		testTranslationV2_0(te, "b = TRUE ∧ b ∈ B", "(and b (B b))");
		testTranslationV2_0(te, "TRUE = b ∧ b ∈ B", "(and b (B b))");
		testTranslationV2_0(te, "b = c ∧ b ∈ B", "(and (= b c) (B b))");

		/**
		 * Formulas containing boolean equalities and quantified boolean
		 * variables.
		 */
		testTranslationV2_0(te, "b = TRUE ∧ (∀d·d = b)",
				"(and b (forall ((d Bool)) (= d b)))");
		testTranslationV2_0(te, "TRUE = b ∧ (∀d·d = b)",
				"(and b (forall ((d Bool)) (= d b)))");
		testTranslationV2_0(te, "b = c ∧ (∀d·d = b)",
				"(and (= b c) (forall ((d Bool)) (= d b)))");

		/**
		 * Boolean equalities without any membership involving boolean values,
		 * neither quantified boolean variables.
		 */
		testTranslationV2_0(te, "b = TRUE", "b");
		testTranslationV2_0(te, "TRUE = b", "b");
		testTranslationV2_0(te, "b = c", "(= b c)");
	}

	@Test
	public void testReservedWords() {
		final ITypeEnvironment te = mTypeEnvironment(//
				"DECIMAL", "par", "NUMERAL", "par");

		testTranslationV2_0(te, "DECIMAL = NUMERAL", "(= nf1 nf0)");
	}

	@Test
	public void testReservedWordsSorts() {
		final ITypeEnvironment te = mTypeEnvironment(//
				"ite", "ℙ(par)", "let", "par", "as", "par");

		final Set<String> expectedSorts = new HashSet<String>(Arrays.asList( //
				"PP", "Int", "PZ", "Bool", "PB", "NS"));

		testTypeEnvironmentSorts(defaultLogic, te, expectedSorts, "let = as");

	}

	@Test
	public void testReservedWordsFuns() {
		final ITypeEnvironment te = mTypeEnvironment( //
				"DECIMAL", "ℙ(as)", "NUMERAL", "as", "STRING", "as");

		final Set<String> expectedFuns = new HashSet<String>(Arrays.asList( //
				"BOOLS () PB", //
				"nf0 () NS", //
				"INTS () PZ", //
				"nf1 () PA", //
				"nf () NS"));

		testTypeEnvironmentFuns(defaultLogic, te, expectedFuns,
				"NUMERAL = STRING");
	}

	@Test
	public void testNumeral() {
		final ITypeEnvironment te = ExtendedFactory.eff.makeTypeEnvironment();
		testTranslateGoalPP(te, "n ≥ 1", "(not (<= 1 n))");
	}

	@Test
	public void testNegative() {
		final ITypeEnvironment te = ExtendedFactory.eff.makeTypeEnvironment();
		testTranslateGoalPP(te, "n ≥ −1", "(not (<= (- 1) n))");
	}

	@Test
	public void testQuantifier() {
		final ITypeEnvironment te = ExtendedFactory.eff.makeTypeEnvironment();
		testTranslateGoalPP(te, "∀ x · x + 1 ∈ S",
				"(not (forall ((x Int)) (exists ((x0 Int)) (and (= x0 (+ x 1)) (S x0)))))");
	}

	@Test
	public void testIntAxiom() {
		final ITypeEnvironment te = defaultTe;
		final List<String> expectedAssumptions = Arrays
				.asList("(forall ((x Int)) (MS x INTS))", //
						"(forall ((x1 Int)) (exists ((X PZ)) (and (MS x1 X) (forall ((y Int)) (=> (MS y X) (= y x1))))))", //
						"(exists ((X0 PZ)) (forall ((x2 Int)) (not (MS x2 X0))))");

		testContainsAssumptionsPP(te, "a + b ↦ ℤ ∈ AZ", expectedAssumptions);
	}

	@Test
	public void testEmptySetAxiom() {
		final ITypeEnvironment te = defaultTe;
		final List<String> expectedAssumptions = Arrays
				.asList("(exists ((X1 PZ)) (forall ((x1 Int)) (not (MS x1 X1))))", //
						"(forall ((x0 Int)) (exists ((X0 PZ)) (and (MS x0 X0) (forall ((y Int)) (=> (MS y X0) (= y x0))))))");
		testContainsAssumptionsPP(te, "∃ x, X ⦂ ℙ(ℤ) · x ∈ X", expectedAssumptions);
	}

	@Test
	public void testTrueAxiom() {
		final ITypeEnvironment te = mTypeEnvironment("Y", "ℙ(BOOL×BOOL)");
		final List<String> expectedAssumptions = Arrays.asList();

		testContainsAssumptionsPP(te, "FALSE↦TRUE ∈ Y", expectedAssumptions);
	}

	@Test
	public void testBoolAxiom() {
		final ITypeEnvironment te = defaultTe;
		final List<String> expectedAssumptions = Arrays.asList();
		testContainsAssumptionsPP(te, "a↦BOOL↦a ∈ Y", expectedAssumptions);
	}

	@Test
	public void testBoundBaseType() {
		final ITypeEnvironment te = mTypeEnvironment();
		testTranslateGoalPP(te, "∀z⦂ℙ(A×B),c⦂ℙ(A×B)·z=c",
				"(not (forall ((z PAB) (c PAB) (x A) (x0 B)) (= (MS x x0 z) (MS x x0 c))))");
	}

	@Test
	public void testBoundBaseType2() {
		final ITypeEnvironment te = mTypeEnvironment();
		testTranslateGoalPP(
				te,
				"∀z⦂A×B,c⦂A×B·z=c",
				"(not (and (forall ((z_1 A) (z_10 B) (c_1 A) (c_10 B)) (= z_1 c_1)) (forall ((z_11 A) (z_12 B) (c_11 A) (c_12 B)) (= z_12 c_12))))");
	}

	@Test
	public void testBoundBaseType3() {
		final ITypeEnvironment te = mTypeEnvironment();
		testTranslateGoalPP(te, "∀z⦂A,c⦂A·z↦c=c↦z",
				"(not (and (forall ((z A) (c A)) (= z c)) (forall ((z0 A) (c0 A)) (= c0 z0))))");
	}

	@Test
	public void testBoundBaseType4() {
		final ITypeEnvironment te = mTypeEnvironment();
		testTranslateGoalPP(te, "∃ x ⦂ ℤ×ℤ×ℤ, X ⦂ ℙ(ℤ×ℤ×ℤ) · x ∈ X",
				"(not (exists ((x_1 Int) (x_10 Int) (x_11 Int) (X PZZZ)) (MS x_1 x_10 x_11 X)))");
	}

	@Test
	public void testBoundBaseType5() {
		final ITypeEnvironment te = mTypeEnvironment();
		testTranslateGoalPP(te,
				"∃ x ⦂ ℙ(ℙ(ℤ)×ℙ(ℤ)), X ⦂ ℙ(ℙ(ℙ(ℤ)×ℙ(ℤ))) · x ∈ X",
				"(not (exists ((x PZZ) (X PZZ0)) (MS x X)))");
	}

	@Test
	public void testBoundBaseType6() {
		final ITypeEnvironment te = mTypeEnvironment();
		testTranslateGoalPP(te,
				"∃ x ⦂ ℙ(ℙ(ℤ)×ℙ(ℤ)), X ⦂ ℙ(ℙ(ℙ(ℤ)×ℙ(ℤ))) · x ∈ X",
				"(not (exists ((x PZZ) (X PZZ0)) (MS x X)))");
	}

	@Test
	public void testBoundRightHandSide() {
		final ITypeEnvironment te = mTypeEnvironment("a", "ℙ(A)");
		testTranslateGoalPP(
				te,
				"∀z⦂ℙ(A),c⦂A·(c ∈ a)∧(c ∈ z)",
				"(not (and (forall ((z PA) (c A)) (MS c a)) (forall ((z0 PA) (c0 A)) (MS c0 z0))))");
	}

	/**
	 * Ensures that a set which occurs in a simple manner is simplified into a
	 * predicate, rather than using the general encoding of membership.
	 */
	@Test
	public void testSetAsPredicate() {
		final ITypeEnvironment te = mTypeEnvironment("a", "S");
		testTranslateGoalPP(te, "A ∪ {a} = A",
				"(not (forall ((x S)) (= (or (A x) (= x a)) (A x))))");
	}

	@Test
	public void testExtensions() throws Exception {
		final ITypeEnvironment te = org.eventb.core.ast.tests.AbstractTests.LIST_FAC.makeTypeEnvironment();
		testTranslateGoalPP(te, "head(cons(1, nil)) = 2",
				"(not (exists ((x57 List_Type) (x58 Int)) (and (exists ((x59 Int)) (and (= x59 1) (cons x59 nil x57))) (= x58 2) (head x57 x58))))");
	}

	@Test
	public void testCallBelong1XtraSortXtraFun() {
		final ITypeEnvironment te = mTypeEnvironment(//
				"e", "ℙ(S)", "f", "ℙ(S)", "g", "S", "a", "A", "c", "BOOL");

		final List<String> hyps = new ArrayList<String>();
		hyps.add("g ∈ e");

		final Set<String> expectedSorts = singleton("S");

		final Set<String> expectedFuns = singleton("g () S");

		final Set<String> expectedPreds = new HashSet<String>(Arrays.asList( //
				"e (S) Bool", //
				"f (S) Bool"));

		doTTeTest("belong_1_type_environment", hyps, "g ∈ f", te, expectedFuns,
				expectedPreds, expectedSorts);
	}

}
