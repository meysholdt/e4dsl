package org.eclipse.e4.ui.model.workbench.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.common.types.xtext.AbstractTypeScopeProvider;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.DefaultGlobalScopeProvider;

import com.google.inject.Inject;

public class TextualWorkbenchGlobalScopeProvider extends
		DefaultGlobalScopeProvider {

	@Inject
	private AbstractTypeScopeProvider javaTypeScopeProvider;
	
	@Override
	public IScope getScope(EObject context, EReference reference) {
		if(reference.getEReferenceType().getEPackage() == TypesPackage.eINSTANCE) {
			return javaTypeScopeProvider.getScope(context, reference);
		}
		return super.getScope(context, reference);
	}
}
