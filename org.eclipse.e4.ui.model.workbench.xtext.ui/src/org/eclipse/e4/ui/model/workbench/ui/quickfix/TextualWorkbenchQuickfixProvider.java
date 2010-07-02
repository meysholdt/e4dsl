package org.eclipse.e4.ui.model.workbench.ui.quickfix;

import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.Application;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.Command;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.E4WorkbenchXtextFactory;
import org.eclipse.e4.ui.model.workbench.ui.linking.TextualWorkbenchDiagnosticMessageProvider;
import org.eclipse.e4.ui.model.workbench.validation.TextualWorkbenchJavaValidator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.IModification;
import org.eclipse.xtext.ui.editor.model.edit.IModificationContext;
import org.eclipse.xtext.ui.editor.model.edit.ISemanticModification;
import org.eclipse.xtext.ui.editor.quickfix.DefaultQuickfixProvider;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.Issue;

public class TextualWorkbenchQuickfixProvider extends DefaultQuickfixProvider {

	@Fix(TextualWorkbenchJavaValidator.SHOULD_START_WITH_CAPITAL)
	public void capitalizeName(final Issue issue,
			IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Capitalize name", "Capitalize name of type",
				"upcase.png", new IModification() {
					public void apply(IModificationContext context)
							throws BadLocationException {
						IXtextDocument xtextDocument = context
								.getXtextDocument();
						String firstLetter = xtextDocument.get(issue
								.getOffset(), 1);
						xtextDocument.replace(issue.getOffset(), 1, Strings
								.toFirstUpper(firstLetter));
					}
				});
	}

	@Fix(TextualWorkbenchDiagnosticMessageProvider.MISSING_COMMAND)
	public void createSupertype(final Issue issue,
			IssueResolutionAcceptor acceptor) {
		final String linkText = issue.getData()[0];
		final String reference = issue.getData()[1];
		acceptor.accept(issue, "Create command '" + linkText + "'",
				"Create command '" + linkText + "'", null,
				new ISemanticModification() {
					public void apply(final EObject element,
							IModificationContext context) {
						Application app = EcoreUtil2.getContainerOfType(
								element, Application.class);
						EReference ref = (EReference) element.eClass()
								.getEStructuralFeature(reference);
						Command cmd = E4WorkbenchXtextFactory.eINSTANCE
								.createCommand();
						cmd.setCommandName(linkText);
						cmd.setId(linkText);
						app.getCommands().add(cmd);
						element.eSet(ref, cmd);
					}
				});
	}

}
