/*******************************************************************************
 * Copyright (c) 2014 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *******************************************************************************/
package org.eventb.smt.core;

import static org.eventb.core.seqprover.SequentProver.getAutoTacticRegistry;
import static org.eventb.smt.core.internal.provers.SMTProversCore.PLUGIN_ID;

import org.eclipse.core.runtime.IPath;
import org.eventb.core.seqprover.ITacticDescriptor;
import org.eventb.smt.core.internal.prefs.ConfigDescriptor;
import org.eventb.smt.core.internal.prefs.ConfigPreferences;
import org.eventb.smt.core.internal.prefs.SolverDescriptor;
import org.eventb.smt.core.internal.prefs.SolverPreferences;
import org.eventb.smt.core.internal.tactics.DefaultAutoWithSMT;
import org.eventb.smt.core.internal.tactics.SMTTacticDescriptors;

/**
 * Facade class providing the interface to the SMT core plug-in functionality.
 * <p>
 * This plug-in maintains two list of descriptors: one for all known solver
 * descriptors and one for all known configuration descriptors.
 * </p>
 * 
 * @author Yoann Guyot
 */
public class SMTCore {

	/**
	 * Descriptor of the auto-tactic that runs each enabled solver on the
	 * current sequent until it is discharged.
	 */
	public static final ITacticDescriptor smtAutoTactic = getAutoTacticRegistry()
			.getTacticDescriptor(PLUGIN_ID + ".autoTactic");

	/**
	 * Descriptor of the interactive-tactic that runs each enabled solver on the
	 * current sequent in parallel until first one discharges it.
	 */
	public static final ITacticDescriptor smtInterTactic = getAutoTacticRegistry()
			.getTacticDescriptor(PLUGIN_ID + ".interTactic");

	/**
	 * Returns a tactic descriptor for running the given SMT solver
	 * configuration. This method does not verify that the configuration has
	 * been registered. The tactic is configured with the default values for the
	 * time out and restricted parameters.
	 * 
	 * @param configName
	 *            the name of an SMT configuration
	 * @return a tactic descriptor for running the given configuration
	 */
	public static ITacticDescriptor getTacticDescriptor(String configName) {
		return SMTTacticDescriptors.getTacticDescriptor(configName);
	}

	/**
	 * Returns a tactic descriptor for running the "Default Auto Tactic" with
	 * the SMT solvers inserted. The SMT solvers are run within a lasso.
	 * <p>
	 * This tactic descriptor is meant to be installed by the UI plug-in as a
	 * tactic profile in the proving UI, as tactic profile cannot yet be
	 * contributed through extension points.
	 * </p>
	 * 
	 * @return a tactic descriptor for "Default Auto Tactic with SMT"
	 */
	public static ITacticDescriptor getDefaultAutoWithSMT() {
		return DefaultAutoWithSMT.getTacticDescriptor();
	}

	/**
	 * Returns an array of all known SMT configuration descriptors.
	 *
	 * @return an array of all known SMT configurations
	 */
	public static IConfigDescriptor[] getConfigurations() {
		return ConfigPreferences.getConfigs();
	}

	/**
	 * Creates a new configuration descriptor from the given parameters.
	 * 
	 * @param name
	 *            name of the configuration
	 * @param solverName
	 *            name of the solver
	 * @param args
	 *            arguments to pass to the solver
	 * @param approach
	 *            translation approach
	 * @param version
	 *            SMT-LIB version
	 * @param enabled
	 *            whether this configuration shall be part of the default SMT
	 *            auto-tactic
	 * @return a configuration descriptor
	 * @deprecated call
	 *             {@link #newConfigDescriptor(String, String, String, boolean)}
	 *             instead
	 */
	@Deprecated
	public static IConfigDescriptor newConfigDescriptor(String name, String solverName, String args,
			TranslationApproach approach, SMTLIBVersion version, boolean enabled) {
		return newConfigDescriptor(name, solverName, args, enabled);
	}

	/**
	 * Creates a new configuration descriptor from the given parameters.
	 * 
	 * @param name
	 *            name of the configuration
	 * @param solverName
	 *            name of the solver
	 * @param args
	 *            arguments to pass to the solver
	 * @param enabled
	 *            whether this configuration shall be part of the default SMT
	 *            auto-tactic
	 * @return a configuration descriptor
	 */
	public static IConfigDescriptor newConfigDescriptor(String name, String solverName, String args, boolean enabled) {
		return new ConfigDescriptor(name, false, solverName, args, enabled);
	}

	/**
	 * Sets the list of known SMT configurations. This replaces the list of SMT
	 * configurations with the given ones. However, bundled configurations
	 * cannot be removed nor changed by this method (except for enablement). If
	 * a bundled configuration is not present in the list, it will nevertheless
	 * automatically be added to the list of configurations.
	 */
	public static void setConfigurations(IConfigDescriptor[] configs) {
		ConfigPreferences.setConfigs(configs);
	}

	/**
	 * Returns an array of all known SMT solver descriptors.
	 *
	 * @return an array of all known SMT solvers
	 */
	public static ISolverDescriptor[] getSolvers() {
		return SolverPreferences.getSolvers();
	}

	/**
	 * Creates a new solver descriptor from the given parameters.
	 *
	 * @param name
	 *            name of the solver
	 * @param kind
	 *            kind of the solver
	 * @param path
	 *            path to the solver binary
	 * @return a solver descriptor
	 */
	public static ISolverDescriptor newSolverDescriptor(String name,
			SolverKind kind, IPath path) {
		return new SolverDescriptor(name, false, kind, path);
	}

	/**
	 * Sets the list of known SMT solvers. This replaces the list of known SMT
	 * solver descriptors with the given ones. However, bundled solvers cannot
	 * be removed nor changed by this method. If a bundled solver is not present
	 * in the list, it will nevertheless automatically be added to the list of
	 * known SMT solver descriptors.
	 */
	public static void setSolvers(ISolverDescriptor[] newSolvers) {
		SolverPreferences.setSolvers(newSolvers);
	}

}
