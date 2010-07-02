package org.eclipse.e4.ui.model.workbench;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.e4.ui.model.workbench.parseTreeConstruction.TextualWorkbenchParsetreeConstructor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parsetree.AbstractNode;
import org.eclipse.xtext.parsetree.CompositeNode;
import org.eclipse.xtext.parsetree.LeafNode;
import org.eclipse.xtext.parsetree.reconstr.ITokenStream;
import org.eclipse.xtext.parsetree.reconstr.impl.TreeConstructionReportImpl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class FixedParseTreeConstructor extends
		TextualWorkbenchParsetreeConstructor {
	protected class FixedWsMergerStream extends WsMergerStream {

		public FixedWsMergerStream(ITokenStream out) {
			super(out);
		}

		@Override
		protected void writeWhitespacesSince(AbstractNode node)
				throws IOException {
			if (node == null) {
				lastCont = null;
				return;
			}
			CompositeNode c = lastCont;
			int i = lastIndex;
			CompositeNode backupLast = lastCont;
			int backupI = lastIndex;
			lastCont = node.getParent();
			lastIndex = lastCont.getChildren().indexOf(node);
			List<LeafNode> ws = Lists.newArrayList();
			while (true) {
				i++;
				while (c != null && i >= c.getChildren().size()) {
					i = c.getParent() != null ? c.getParent().getChildren()
							.indexOf(c) + 1 : -1;
					c = c.getParent();
				}
				while (c != null && c.getChildren().size() > 0
						&& c.getChildren().get(i) != node
						&& c.getChildren().get(i) instanceof CompositeNode) {
					c = (CompositeNode) c.getChildren().get(i);
					i = 0;
				}
				if (c == null || i < 0 || i >= c.getChildren().size())
					return;
				AbstractNode n = c.getChildren().get(i);
				if (n == node) {
					if (n instanceof CompositeNode)
						for (LeafNode l : n.getLeafNodes())
							if (tokenUtil.isWhitespaceNode(l))
								ws.add(l);
							else
								break;
					if (ws.isEmpty()) {
						out.writeHidden(hiddenTokenHelper
								.getWhitespaceRuleFor(""), "");
						// System.out.println("WS: -nothing-");
					}
					for (LeafNode l : ws) {
						// System.out.println("WS: '" + l.getText() + "'");
						out.writeHidden(l.getGrammarElement(), l.getText());
					}
					return;
				} else if (tokenUtil.isWhitespaceNode(n))
					ws.add((LeafNode) n);
				else
					return;
			}
		}

	}

	public TreeConstructionReport serializeRecursive(EObject object,
			ITokenStream out) throws IOException {
		TreeConstructionReportImpl rep = createReport(object);
		AbstractToken root = serialize(object, rep);
		Set<CompositeNode> roots = Sets.newHashSet();
		Map<EObject, AbstractToken> obj2token = Maps.newHashMap();
		collectRootsAndEObjects(root, obj2token, roots);
		// dump("", root);
		Map<LeafNode, EObject> comments = commentAssociater
				.associateCommentsWithSemanticEObjects(object, roots);
		for (CompositeNode r : roots)
			assignNodesByMatching(obj2token, r, comments);
		WsMergerStream wsout = new FixedWsMergerStream(out);
		// dump("", root);
		write(root, wsout);
		wsout.close();
		return rep;
	}
}
