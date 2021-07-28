/*******************************************************************************
 * Copyright (c) 2013, 2021 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *     ISP RAS - inserted smtAutoTactic without lasso
 *******************************************************************************/
package org.eventb.smt.core.internal.tactics;

import static java.util.Collections.singletonList;
import static org.eventb.core.EventBPlugin.getAutoPostTacticManager;
import static org.eventb.core.seqprover.SequentProver.getAutoTacticRegistry;
import static org.eventb.smt.core.SMTCore.smtAutoTactic;
import static org.eventb.smt.core.internal.provers.SMTProversCore.PLUGIN_ID;
import static org.eventb.smt.core.internal.provers.SMTProversCore.logError;

import java.util.ArrayList;
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
 * sequent prover and inserts the SMT solvers within. The resulting tactic is
 * meant to be installed as a tactic profile in the proving UI.
 * 
 * @author Laurent Voisin
 */
public class DefaultAutoWithSMT {
	
	public static class Contribution implements ITacticProfileContribution {

		@Override
		public ITacticDescriptor create() throws CoreException {
			return getTacticDescriptor();
		}
		
	}

	/**
	 * Returns a new tactic descriptor that integrates enabled SMT solvers and
	 * can be used in place of "Default Auto Tactic".
	 * 
	 * @return a new tactic descriptor integrating the enabled SMT solvers
	 */
	public static ITacticDescriptor getTacticDescriptor() {
		final ITacticDescriptor defaultAuto = getDefaultAuto();
		return getTacticDescriptor(defaultAuto);
	}

	/**
	 * Returns a new tactic descriptor based on the given one and integrating
	 * the enabled SMT solvers just after {@link #BEFORE_TACTIC_ID}. In case of
	 * error when building the new descriptor, the given one is returned.
	 * 
	 * @param base
	 *            a tactic descriptor built from a loop on all pending
	 *            combinator, typically the default Auto tactic preference
	 * @return a new tactic descriptor integrating the enabled SMT solvers
	 */
	public static ITacticDescriptor getTacticDescriptor(ITacticDescriptor base) {
		try {
			return new DefaultAutoWithSMT(base).compute();
		} catch (Exception exc) {
			logError("When computing default auto tactic with SMT", exc);
			return base;
		}
	}

	/*
	 * Id for the tactic descriptor which constitutes the added profile.
	 */
	private static final String TACTIC_ID = PLUGIN_ID + ".default";

	/*
	 * Id of the tactic after which we want to insert the SMT solvers.
	 */
	private static final String BEFORE_TACTIC_ID//
	= "org.eventb.core.seqprover.dtDestrWDTac";

	/*
	 * Id for the tactic descriptor which contains the SMT auto-tactic embedded
	 * in an attempt after lasso.
	 */
	private static final String LASSO_ID = PLUGIN_ID + ".lasso";

	/*
	 * Auto-tactic registry
	 */
	private static final IAutoTacticRegistry REGISTRY = getAutoTacticRegistry();

	/*
	 * Tactic combinator for running a tactic within an attempt after lasso.
	 */
	private static final ICombinatorDescriptor ATTEMPT_AFTER_LASSO = REGISTRY
			.getCombinatorDescriptor("org.eventb.core.seqprover.attemptAfterLasso");

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

	// The tactic in which we're inserting the SMT solvers
	private ICombinedTacticDescriptor base;

	private List<ITacticDescriptor> baseTactics;

	private List<ITacticDescriptor> newTactics;

	private DefaultAutoWithSMT(ITacticDescriptor base) {
		this.base = (ICombinedTacticDescriptor) base;
	}

	/*
	 * Creates a new tactic descriptor from the given one, by inserting the SMT
	 * auto tactic just before the partition rewrite tactic.
	 */
	private ITacticDescriptor compute() {
		baseTactics = base.getCombinedTactics();
		insertIntoList();
		return baseCombinator().combine(newTactics, TACTIC_ID);
	}

	/*
	 * Inserts the SMT solvers in the list of tactics just before
	 * BEFORE_TACTIC_ID.
	 */
	private void insertIntoList() {
		newTactics = new ArrayList<ITacticDescriptor>(baseTactics);
		final ITacticDescriptor beforeDesc;
		beforeDesc = REGISTRY.getTacticDescriptor(BEFORE_TACTIC_ID);
		final int index = newTactics.indexOf(beforeDesc);
		newTactics.add(index, smtAutoInLasso());
		newTactics.add(index, smtAutoTactic);
	}

	/*
	 * Returns a tactic descriptor for running the SMT auto-tactic embedded
	 * within an attempt after lasso.
	 */
	private ITacticDescriptor smtAutoInLasso() {
		final List<ITacticDescriptor> list = singletonList(smtAutoTactic);
		return ATTEMPT_AFTER_LASSO.combine(list, LASSO_ID);
	}

	private ICombinatorDescriptor baseCombinator() {
		final String combinatorId = base.getCombinatorId();
		return REGISTRY.getCombinatorDescriptor(combinatorId);
	}

}
