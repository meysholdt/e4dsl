/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.e4.ui.model.workbench.ui.linking;

import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.E4WorkbenchXtextPackage;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.diagnostics.DiagnosticSeverity;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;

public class TextualWorkbenchDiagnosticMessageProvider extends
		LinkingDiagnosticMessageProvider {

	public static final String PREFIX = "org.eclipse.e4.ui.model.workbench.ui.linking.TextualWorkbenchDiagnosticMessageProvider.";
	public static final String MISSING_COMMAND = PREFIX + "missing_command";

	@Override
	public DiagnosticMessage getUnresolvedProxyMessage(
			final ILinkingDiagnosticContext context) {
		if (context.getReference().getEReferenceType() == E4WorkbenchXtextPackage.eINSTANCE
				.getCommand())
			return new DiagnosticMessage("Missing supertype "
					+ context.getLinkText(), DiagnosticSeverity.ERROR,
					MISSING_COMMAND, context.getLinkText(), context
							.getReference().getName());
		return super.getUnresolvedProxyMessage(context);
	}
}
