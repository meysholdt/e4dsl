package org.eclipse.e4.ui.model.workbench.names;

import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.ApplicationElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;

public class TextualWorkbenchNameProvider extends
		DefaultDeclarativeQualifiedNameProvider {

	public String qualifiedName(ApplicationElement element) {
		return qualifiedNameRecursive(element);
	}

	private String qualifiedNameRecursive(EObject element) {
		if (element == null)
			return null;
		String parent = qualifiedNameRecursive(element.eContainer());
		if (element instanceof ApplicationElement) {
			ApplicationElement ae = (ApplicationElement) element;
			if (ae.getId() != null && ae.getId().length() > 0)
				return parent != null ? parent + "." + ae.getId() : ae.getId();
		}
		return parent;
	}
}
