/*******************************************************************************
 * Copyright (c) 2013, 2017 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *******************************************************************************/
package org.eventb.smt.core.internal.tactics;

import static org.eventb.core.seqprover.tactics.BasicTactics.composeUntilSuccess;
import static org.eventb.smt.core.SMTPreferences.AUTO_TIMEOUT;
import static org.eventb.smt.core.internal.prefs.SimplePreferences.getAutoTimeout;
import static org.eventb.smt.core.internal.tactics.SMTTacticDescriptors.getTacticDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eventb.core.seqprover.IProofMonitor;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.ITactic;
import org.eventb.core.seqprover.ITacticDescriptor;
import org.eventb.smt.core.IConfigDescriptor;
import org.eventb.smt.core.SMTCore;
import org.eventb.smt.core.internal.prefs.ConfigPreferences;
import org.eventb.smt.core.internal.prefs.IPreferencesChangeListener;
import org.eventb.smt.core.internal.prefs.SimplePreferences;

/**
 * An automated tactic that runs successively all enabled SMT configurations
 * until one discharges the current sequent.
 * <p>
 * This tactic caches the list of enabled solvers, which is refreshed in a
 * thread-safe manner.
 * </p>
 * 
 * @author Laurent Voisin
 */
public class SMTAutoTactic implements ITactic {

	public static boolean DEBUG = false;

	/*
	 * Sub-class for initializing the list of tactics in a lazy manner. This
	 * class is also responsible for maintaining it up to date.
	 */
	protected static class TacticsHolder implements IPreferencesChangeListener,
			IPreferenceChangeListener {

		static {
			if (DEBUG) {
				trace("Loading holder class");
			}
			final TacticsHolder instance = new TacticsHolder();
			SimplePreferences.addChangeListener(instance);
			ConfigPreferences.addChangeLister(instance);
			instance.preferencesChange();
		}

		static volatile ITactic[] tactics;

		@Override
		public void preferencesChange() {
			final IConfigDescriptor[] configs = SMTCore.getConfigurations();
			final long timeout = getAutoTimeout();
			if (DEBUG) {
				trace("Updating tactic list with " + Arrays.toString(configs));
				trace("and timeout " + timeout);
			}
			final List<ITactic> newTactics = new ArrayList<ITactic>(
					configs.length);
			for (final IConfigDescriptor config : configs) {
				if (config.isEnabled()) {
					final ITactic tactic = makeConfigTactic(config, timeout);
					newTactics.add(tactic);
				}
			}
			tactics = newTactics.toArray(new ITactic[newTactics.size()]);
		}

		/*
		 * Returns a tactic for running the given SMT solver with default
		 * parameters (restricted and timeout).
		 */
		private ITactic makeConfigTactic(IConfigDescriptor config, long timeout) {
			final String configName = config.getName();
			final ITacticDescriptor tacticDesc = getTacticDescriptor(
					configName, timeout);
			return tacticDesc.getTacticInstance();
		}

		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			if (event.getKey().equals(AUTO_TIMEOUT)) {
				preferencesChange();
			}
		}

	}

	@Override
	public Object apply(IProofTreeNode ptNode, IProofMonitor pm) {
		final ITactic[] tactics = TacticsHolder.tactics;
		if (DEBUG) {
			trace("Launching with " + Arrays.toString(tactics));
		}
		return composeUntilSuccess(tactics).apply(ptNode, pm);
	}

	static void trace(String msg) {
		System.out.println("SMTAutoTactic: " + msg);
	}
}
