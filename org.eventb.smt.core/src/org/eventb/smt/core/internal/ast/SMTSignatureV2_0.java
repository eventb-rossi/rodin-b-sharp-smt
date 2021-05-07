/*******************************************************************************
 * Copyright (c) 2010, 2017 Systerel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Systerel - initial API and implementation
 *     UFRN - additional methods
 *******************************************************************************/
package org.eventb.smt.core.internal.ast;

import static org.eventb.smt.core.internal.ast.attributes.Label.DEFAULT_GOAL_LABEL;
import static org.eventb.smt.core.internal.ast.attributes.Label.DEFAULT_HYPOTHESIS_LABEL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eventb.smt.core.internal.ast.attributes.Label;
import org.eventb.smt.core.internal.ast.commands.DeclareFunCommand;
import org.eventb.smt.core.internal.ast.commands.DeclareSortCommand;
import org.eventb.smt.core.internal.ast.commands.SetLogicCommand;
import org.eventb.smt.core.internal.ast.commands.Command.SMTCommandName;
import org.eventb.smt.core.internal.ast.symbols.SMTQuantifierSymbol;
import org.eventb.smt.core.internal.ast.symbols.SMTSortSymbol;
import org.eventb.smt.core.internal.ast.symbols.SMTSymbol;
import org.eventb.smt.core.internal.ast.theories.Logic;
import org.eventb.smt.core.internal.ast.theories.TheoryV2_0;

public abstract class SMTSignatureV2_0 extends SMTSignature {
	/**
	 * reserved symbols and keywords
	 */
	private final static Set<String> reservedSymbols = getReservedSymbolsAndKeywordsV2_0();

	/**
	 * Constructs a new Signature given the SMT Logic
	 * 
	 * @param logic
	 *            the logic used in the SMTSignature
	 */
	public SMTSignatureV2_0(final Logic logic) {
		super(logic);
	}

	/**
	 * This method returns a set with the reserved symbols and keywords in
	 * SMT-LIB 2.0
	 * 
	 * @return the reserved symbols and keyboards.
	 */
	public static Set<String> getReservedSymbolsAndKeywordsV2_0() {
		/**
		 * "The basic set of reserved words consists of par NUMERAL DECIMAL
		 * STRING _ ! as let forall exists"
		 */
		final List<String> reservedSymbolsAndKeywords = new ArrayList<String>(
				Arrays.asList("par", "NUMERAL", "DECIMAL", "STRING", "_", "!",
						"as", "let"));
		final boolean successfullyAddedReservedSymbolsAndKeywords = reservedSymbolsAndKeywords
				.addAll(SMTQuantifierSymbol.getQuantifierSymbols())
				/**
				 * "Each command name in the scripting language defined in
				 * SMT-LIB documentation is also a reserved word"
				 */
				&& reservedSymbolsAndKeywords.addAll(SMTCommandName
						.getCommandNames())
				&& reservedSymbolsAndKeywords.addAll(SMTConnective
						.getConnectiveSymbols());
		assert successfullyAddedReservedSymbolsAndKeywords;
		return new HashSet<String>(reservedSymbolsAndKeywords);
	}

	/**
	 * Appends the string representation of this signature sorts (when they are
	 * not predefined) in SMT-LIB v2.0 version syntax. It doesn't modify the
	 * buffer if the set contains only predefined symbols.
	 * 
	 * @param builder
	 */
	private void sortDeclarations(final StringBuilder builder) {
		if (!sorts.isEmpty()) {
			final TreeSet<SMTSortSymbol> sortedSorts = new TreeSet<SMTSortSymbol>(
					sorts);
			DeclareSortCommand command;
			for (final SMTSortSymbol sort : sortedSorts) {
				if (!sort.isPredefined()) {
					command = new DeclareSortCommand(sort);
					command.toString(builder);
					builder.append("\n");
				}
			}
		}
	}

	/**
	 * Appends the string representation of this signature funs (when they are
	 * not predefined) in SMT-LIB v2.0 version syntax. It doesn't modify the
	 * buffer if the set contains only predefined symbols.
	 * 
	 * @param builder
	 */
	private static <SMTFunOrPredSymbol extends SMTSymbol> void funDeclarations(
			final StringBuilder builder, final Set<SMTFunOrPredSymbol> elements) {
		if (!elements.isEmpty()) {
			final TreeSet<SMTFunOrPredSymbol> sortedSymbols = new TreeSet<SMTFunOrPredSymbol>(
					elements);
			DeclareFunCommand command;
			for (final SMTFunOrPredSymbol symbol : sortedSymbols) {
				if (!symbol.isPredefined()) {
					command = new DeclareFunCommand(symbol);
					command.toString(builder);
					builder.append("\n");
				}
			}
		}
	}

	/**
	 * Appends to the StringBuilder the string representation of the logic
	 * section
	 * 
	 * @param builder
	 *            the StringBuilder
	 */
	private void logicSection(final StringBuilder builder) {
		final SetLogicCommand setLogicCommand = new SetLogicCommand(
				logic.getName());
		setLogicCommand.toString(builder);
		builder.append("\n");
	}

	@Override
	void loadReservedAndPredefinedSymbols() {
		names.addAll(reservedSymbols);
		/**
		 * Adding hyp and hyp0 to the list, is a trick to make hypotheses
		 * labelling beginning with hyp1
		 */
		names.add("hyp");
		names.add("hyp0");
	}

	@Override
	String freshSymbolName(final Set<String> symbolNames, final String name,
			final String newSymbolName) {
		final String freshName;
		/**
		 * Avoids creating names with apostrophes
		 */
		int apoInd = name.indexOf('\'');
		if (apoInd > -1) {
			final String newName = name.substring(0, apoInd) + '$' +
				name.substring(apoInd + 1);
			return freshSymbolName(symbolNames, newName, newSymbolName);
		}
		/**
		 * Avoids creating names similar to reserved symbols, predefined symbols
		 * or keywords
		 */
		if (reservedSymbols.contains(name)) {
			freshName = freshName(symbolNames, newSymbolName);
		} else {
			freshName = freshName(symbolNames, name);
		}

		names.add(freshName);

		return freshName;
	}

	/**
	 * These labels are used to annotate assertions. That's why their type is
	 * Bool.
	 */
	public Label freshLabel(final boolean goalLabel) {
		final String label;
		if (goalLabel) {
			label = DEFAULT_GOAL_LABEL;
		} else {
			label = DEFAULT_HYPOTHESIS_LABEL;
		}
		final SMTSymbol labelSymbol = freshConstant(label, TheoryV2_0.Core
				.getInstance().getBooleanSort());
		return new Label(labelSymbol);
	}

	/**
	 * Appends to the StringBuilder the string representation of the signature
	 * 
	 * @param builder
	 *            the StringBuilder
	 */
	@Override
	public void toString(final StringBuilder builder) {
		logicSection(builder);
		sortDeclarations(builder);
		funDeclarations(builder, preds);
		funDeclarations(builder, funs);
	}
}
