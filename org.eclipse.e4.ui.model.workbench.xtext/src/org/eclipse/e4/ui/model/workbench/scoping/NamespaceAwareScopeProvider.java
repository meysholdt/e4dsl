/*******************************************************************************
 * Copyright (c) 2008,2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.e4.ui.model.workbench.scoping;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.common.types.access.ITypeProvider;
import org.eclipse.xtext.common.types.xtext.AbstractTypeScopeProvider;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ImportedNamespaceAwareLocalScopeProvider;

import com.google.inject.Inject;

public class NamespaceAwareScopeProvider extends
		ImportedNamespaceAwareLocalScopeProvider {

	@Inject
	private AbstractTypeScopeProvider typeScopeProvider;

	@Override
	protected IScope getGlobalScope(EObject context, EReference reference) {
		EClass referenceType = reference.getEReferenceType();
		if (EcoreUtil2.isAssignableFrom(TypesPackage.Literals.JVM_TYPE,
				referenceType)) {
			ResourceSet resourceSet = context.eResource().getResourceSet();
			ITypeProvider typeProvider = typeScopeProvider
					.getTypeProviderFactory().findTypeProvider(resourceSet);
			if (typeProvider == null)
				typeProvider = typeScopeProvider.getTypeProviderFactory()
						.createTypeProvider(resourceSet);
			return typeScopeProvider.createTypeScope(typeProvider);
		} else {
			return super.getGlobalScope(context, reference);
		}
	}

	// @Override
	// public Set<ImportNormalizer> getImportNormalizer(EObject context) {
	// if (context instanceof Application) {
	// Module module = (Application) context;
	// Set<ImportNormalizer> result = super.getImportNormalizer(context);
	// result.add(createImportNormalizer("java.lang.*"));
	// String name = module.getCanonicalName();
	// int dot = name.lastIndexOf('.');
	// if (dot >= 0) {
	// name = name.substring(0, dot) + ".*";
	// result.add(createImportNormalizer(name));
	// }
	// return result;
	// }
	// return Collections.emptySet();
	// }

}
