/*******************************************************************************
 * Copyright (c) 2010, 2012 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *     UFRN - minor modifications
 *******************************************************************************/
package org.eventb.smt.core.internal.provers;

import static org.eclipse.core.runtime.IStatus.ERROR;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eventb.smt.core.internal.prefs.AbstractPreferences;
import org.eventb.smt.core.internal.tactics.SMTAutoTactic;
import org.eventb.smt.core.internal.tactics.SMTInterTactic;
import org.eventb.smt.core.internal.translation.Translator;
import org.osgi.framework.BundleContext;

/**
 * This is the main class of the SMT solvers plugin.
 */
public class SMTProversCore extends Plugin {
	
	/**
	 * This plug-in identifier.
	 */
	public static final String PLUGIN_ID = "org.eventb.smt.core"; //$NON-NLS-1$

	/**
	 * The shared instance.
	 */
	private static SMTProversCore plugin;

	/**
	 * Debug variables
	 */
	private static final String DEBUG = PLUGIN_ID + "/debug/";
	private static final String DEBUG_PREFS = DEBUG + "prefs";
	private static final String DEBUG_TACTICS = DEBUG + "tactics";
	private static final String DEBUG_TRANSLATOR = DEBUG + "translator";
	private static final String DEBUG_TRANSLATOR_DETAILS = DEBUG_TRANSLATOR
			+ "_details";

	/**
	 * Returns the single instance of the Smt Provers for Rodin core plug-in.
	 * 
	 * @return the single instance of the Smt Provers for Rodin core plug-in
	 */
	public static SMTProversCore getDefault() {
		return plugin;
	}

	private static boolean parseOption(final String key) {
		final String option = Platform.getDebugOption(key);
		return "true".equalsIgnoreCase(option);
	}

	/**
	 * Process debugging/tracing options coming from Eclipse.
	 */
	private void configureDebugOptions() {
		AbstractPreferences.DEBUG = parseOption(DEBUG_PREFS);
		SMTAutoTactic.DEBUG = parseOption(DEBUG_TACTICS);
		SMTInterTactic.DEBUG = parseOption(DEBUG_TACTICS);
		Translator.DEBUG = parseOption(DEBUG_TRANSLATOR);
		Translator.DEBUG_DETAILS = parseOption(DEBUG_TRANSLATOR_DETAILS);
	}

	/**
	 * Adds an error in the plug-in log.
	 *
	 * @param message
	 *            some message describing the error
	 * @param exception
	 *            the exception that caused the error or <code>null</code>
	 */
	public static void logError(final String message, final Throwable exception) {
		plugin.getLog().log(new Status(ERROR, PLUGIN_ID, message, exception));
	}

	/**
	 * Enables Java assertion checks for this plug-in.
	 */
	private void enableAssertions() {
		getClass().getClassLoader().setDefaultAssertionStatus(true);
	}

	/**
	 * Starts up this plug-in.
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		enableAssertions();
		if (isDebugging()) {
			configureDebugOptions();
		}
	}

	/**
	 * Stops this plug-in.
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
}
