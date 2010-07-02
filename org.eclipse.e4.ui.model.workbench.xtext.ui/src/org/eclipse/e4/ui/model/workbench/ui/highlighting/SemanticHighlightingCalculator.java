package org.eclipse.e4.ui.model.workbench.ui.highlighting;

import org.eclipse.e4.ui.model.workbench.services.TextualWorkbenchGrammarAccess;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.parsetree.AbstractNode;
import org.eclipse.xtext.parsetree.LeafNode;
import org.eclipse.xtext.parsetree.NodeUtil;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator;

import com.google.inject.Inject;

public class SemanticHighlightingCalculator implements
		ISemanticHighlightingCalculator {

	private TextualWorkbenchGrammarAccess grammar;

	@Inject
	@SuppressWarnings("unused")
	private void setGrammar(IGrammarAccess g) {
		grammar = (TextualWorkbenchGrammarAccess) g;
	}

	public void provideHighlightingFor(final XtextResource resource,
			IHighlightedPositionAcceptor acceptor) {
		if (resource == null)
			return;

		Iterable<AbstractNode> allNodes = NodeUtil.getAllContents(resource
				.getParseResult().getRootNode());
		for (AbstractNode abstractNode : allNodes) {
			if (abstractNode.getGrammarElement() instanceof CrossReference) {
				CrossReference ref = (CrossReference) abstractNode
						.getGrammarElement();
				if (ref.getType().getClassifier().getEPackage() == TypesPackage.eINSTANCE)
					highlightNode(abstractNode,
							SemanticHighlightingConfiguration.JAVA_REF,
							acceptor);
				else if (ref.getType().getClassifier().getEPackage() == EcorePackage.eINSTANCE
						&& ref.getType().getClassifier() != EcorePackage.eINSTANCE
								.getEObject())
					highlightNode(abstractNode,
							SemanticHighlightingConfiguration.META_REF,
							acceptor);
				else
					highlightNode(abstractNode,
							SemanticHighlightingConfiguration.CROSS_REF,
							acceptor);
			} else if (abstractNode.getGrammarElement() instanceof RuleCall) {
				RuleCall rc = (RuleCall) abstractNode.getGrammarElement();
				Assignment ass = GrammarUtil.containingAssignment(rc);
				if (rc.getRule() == grammar.getFQNRule() && ass != null
						&& ass.getFeature().equals("id"))
					highlightNode(abstractNode,
							SemanticHighlightingConfiguration.FQN, acceptor);

			}
		}
	}

	private void highlightNode(AbstractNode node, String id,
			IHighlightedPositionAcceptor acceptor) {
		if (node == null)
			return;
		if (node instanceof LeafNode) {
			acceptor.addPosition(node.getOffset(), node.getLength(), id);
		} else {
			for (LeafNode leaf : node.getLeafNodes()) {
				if (!leaf.isHidden()) {
					acceptor.addPosition(leaf.getOffset(), leaf.getLength(), id);
				}
			}
		}
	}

}