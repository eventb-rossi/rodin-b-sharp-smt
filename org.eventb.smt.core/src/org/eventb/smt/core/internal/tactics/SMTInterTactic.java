/*******************************************************************************
 * Copyright (c) 2021 ISP RAS and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ISP RAS - initial API and implementation
 *******************************************************************************/
package org.eventb.smt.core.internal.tactics;

import static org.eventb.core.seqprover.tactics.BasicTactics.firstSuccessful;

import java.util.Arrays;

import org.eventb.core.seqprover.IProofMonitor;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.ITactic;

/**
 * An automated tactic that runs successively all enabled SMT configurations
 * until one discharges the current sequent.
 * <p>
 * This tactic caches the list of enabled solvers, which is refreshed in a
 * thread-safe manner.
 * </p>
 * 
 * @author Ilya Shchepetkov
 */
public class SMTInterTactic extends SMTAutoTactic {
	@Override
	public Object apply(IProofTreeNode ptNode, IProofMonitor pm) {
		final ITactic[] tactics = TacticsHolder.tactics;
		if (DEBUG) {
			trace("Launching with " + Arrays.toString(tactics));
		}
		return firstSuccessful(tactics).apply(ptNode, pm);
	}

	static void trace(String msg) {
		System.out.println("SMTInterTactic: " + msg);
	}
}
