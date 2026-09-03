/*******************************************************************************
 * Copyright (c) 2013, 2021 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *     ISP RAS - modification of DefaultAutoWithSmt for interactive case
 *******************************************************************************/
package org.eventb.smt.core.internal.tactics;

import static java.util.Collections.singletonList;
import static org.eventb.core.EventBPlugin.getAutoPostTacticManager;
import static org.eventb.core.seqprover.SequentProver.getAutoTacticRegistry;
import static org.eventb.smt.core.SMTCore.smtInterTactic;
import static org.eventb.smt.core.internal.provers.SMTProversCore.PLUGIN_ID;
import static org.eventb.smt.core.internal.provers.SMTProversCore.logError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.pm.ITacticProfileContribution;
import org.eventb.core.preferences.autotactics.IAutoPostTacticManager;
import org.eventb.core.seqprover.IAutoTacticRegistry;
import org.eventb.core.seqprover.ICombinatorDescriptor;
import org.eventb.core.seqprover.ICombinedTacticDescriptor;
import org.eventb.core.seqprover.ITacticDescriptor;
import org.eventb.core.seqprover.autoTacticPreference.IAutoTacticPreference;

/**
 * Builds a tactic descriptor that duplicates the default auto tactic of the
 * sequent prover and inserts parallel launch of the P0, ML and SMT solvers
 * (with and without lasso) within.
 * The resulting tactic is meant to be installed as a tactic profile in the
 * proving UI.
 * 
 * @author Ilya Shchepetkov
 */
public class DefaultInterWithSMT {
	
	public static class Contribution implements ITacticProfileContribution {

		@Override
		public ITacticDescriptor create() throws CoreException {
			return getTacticDescriptor();
		}
		
	}

	/**
	 * Returns a new tactic descriptor that integrates P0, ML and the enabled
	 * SMT solvers and runs them all in parallel. It can be used in place of
	 * "Default Auto Tactic with SMT" for interactive proofs.
	 * 
	 * @return a new tactic descriptor for interactive proofs
	 */
	public static ITacticDescriptor getTacticDescriptor() {
		final ITacticDescriptor defaultAuto = getDefaultAuto();
		return getTacticDescriptor(defaultAuto);
	}

	/**
	 * Returns a new tactic descriptor based on the given one and integrates
	 * P0, ML and the enabled SMT solvers just after {@link #BEFORE_TACTIC_ID},
	 * and runs them all in parallel. In case of error when building the new
	 * descriptor, the given one is returned.
	 * 
	 * @param base
	 *            a tactic descriptor built from a loop on all pending
	 *            combinator, typically the default Auto tactic preference
	 * @return a new tactic descriptor for interactive proofs
	 */
	public static ITacticDescriptor getTacticDescriptor(ITacticDescriptor base) {
		try {
			return new DefaultInterWithSMT(base).compute();
		} catch (Exception exc) {
			logError("When computing default interactive tactic with SMT", exc);
			return base;
		}
	}

	/*
	 * Id for the tactic descriptor which constitutes the added profile.
	 */
	private static final String TACTIC_ID = PLUGIN_ID + ".interdefault";

	/*
	 * Id of the tactic after which we want to make modifications.
	 */
	private static final String BEFORE_TACTIC_ID//
	= "org.eventb.core.seqprover.dtDestrWDTac";

	/*
	 * Id of the Atelier ML prover tactic descriptor.
	 */
	private static final String ML_TACTIC_ID = "com.clearsy.atelierb.provers.core.ml";

	/*
	 * Id of the Atelier P0 prover tactic descriptor.
	 */
	private static final String P0_TACTIC_ID = "com.clearsy.atelierb.provers.core.p0";

	/*
	 * Id for the lasso tactic descriptor.
	 */
	private static final String LASSO_ID = PLUGIN_ID + ".lasso";

	/*
	 * Id for the first successful tactic descriptor.
	 */
	private static final String FIRST_ID = PLUGIN_ID + ".first";

	/*
	 * Auto-tactic registry
	 */
	private static final IAutoTacticRegistry REGISTRY = getAutoTacticRegistry();

	/*
	 * Tactic combinator for running a tactic within an attempt after lasso.
	 */
	private static final ICombinatorDescriptor ATTEMPT_AFTER_LASSO = REGISTRY
			.getCombinatorDescriptor("org.eventb.core.seqprover.attemptAfterLasso");

	/*
	 * Tactic combinator for running a tactic in parallel.
	 */
	private static final ICombinatorDescriptor FIRST_SUCCESFUL = REGISTRY
			.getCombinatorDescriptor("org.eventb.core.seqprover.firstSuccessful");

	/**
	 * Returns the tactic descriptor for the "Default Auto Tactic" from the
	 * Event-B core plug-in.
	 * 
	 * @return the "Default Auto Tactic" descriptor
	 */
	public static ITacticDescriptor getDefaultAuto() {
		final IAutoPostTacticManager manager = getAutoPostTacticManager();
		final IAutoTacticPreference pref = manager.getAutoTacticPreference();
		return pref.getDefaultDescriptor();
	}

	// The tactic in which we're making modifications.
	private ICombinedTacticDescriptor base;

	private List<ITacticDescriptor> baseTactics;

	private List<ITacticDescriptor> newTactics;

	private DefaultInterWithSMT(ITacticDescriptor base) {
		this.base = (ICombinedTacticDescriptor) base;
	}

	/*
	 * Creates a new tactic descriptor from the given one, by inserting the SMT
	 * auto tactic just before the partition rewrite tactic.
	 */
	private ITacticDescriptor compute() {
		baseTactics = base.getCombinedTactics();
		newTactics = new ArrayList<ITacticDescriptor>(baseTactics);
		removeFromList();
		insertIntoList();
		return baseCombinator().combine(newTactics, TACTIC_ID);
	}
	
	/*
	 * Removes the ML and P0 tactics from the list: insertIntoList() puts them
	 * back inside the parallel combinator, so leaving them here would run them
	 * a second time, sequentially, defeating the point of the profile.
	 *
	 * Matching on the tactic id rather than on a position keeps this correct if
	 * the sequent prover ever reorders its default auto tactic. It also works
	 * when the Atelier B provers are not installed: the registry then yields a
	 * placeholder descriptor that still carries the requested id.
	 */
	private void removeFromList() {
		newTactics.removeIf(desc -> {
			final String id = desc.getTacticID();
			return ML_TACTIC_ID.equals(id) || P0_TACTIC_ID.equals(id);
		});
	}

	/*
	 * Inserts parallel launch of provers and SMT solvers in the list of tactics
	 * just before BEFORE_TACTIC_ID.
	 */
	private void insertIntoList() {
		final ITacticDescriptor beforeDesc;
		beforeDesc = REGISTRY.getTacticDescriptor(BEFORE_TACTIC_ID);
		final int index = newTactics.indexOf(beforeDesc);
		newTactics.add(index, firstSuccessful());
	}

	/*
	 * Returns a tactic descriptor for running provers and SMT solvers in parallel.
	 */
	private ITacticDescriptor firstSuccessful() {
		final List<ITacticDescriptor> list = Arrays.asList(
			REGISTRY.getTacticDescriptor(ML_TACTIC_ID),
			REGISTRY.getTacticDescriptor(P0_TACTIC_ID),
			smtInterTactic,
			smtInterInLasso()
		);
		return FIRST_SUCCESFUL.combine(list, FIRST_ID);
	}

	/*
	 * Returns a tactic descriptor for running the SMT interactive-tactic embedded
	 * within an attempt after lasso.
	 */
	private ITacticDescriptor smtInterInLasso() {
		final List<ITacticDescriptor> list = singletonList(smtInterTactic);
		return ATTEMPT_AFTER_LASSO.combine(list, LASSO_ID);
	}

	private ICombinatorDescriptor baseCombinator() {
		final String combinatorId = base.getCombinatorId();
		return REGISTRY.getCombinatorDescriptor(combinatorId);
	}

}
