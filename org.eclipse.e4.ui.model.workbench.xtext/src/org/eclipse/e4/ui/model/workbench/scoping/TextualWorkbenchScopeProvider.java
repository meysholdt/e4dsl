/*
 * generated by Xtext
 */
package org.eclipse.e4.ui.model.workbench.scoping;

import java.util.Set;

import org.eclipse.e4.ui.model.application.MApplicationPackage;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.ApplicationElement;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.E4WorkbenchXtextPackage;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.GenericValue;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.parsetree.CompositeNode;
import org.eclipse.xtext.parsetree.NodeUtil;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * This class contains custom scoping description.
 * 
 * see : http://www.eclipse.org/Xtext/documentation/latest/xtext.html#scoping on
 * how and when to use it
 * 
 */
public class TextualWorkbenchScopeProvider extends
		AbstractDeclarativeScopeProvider {

	private EClass getTargetEClass(ApplicationElement context) {
		if (context.getGenericType() != null)
			return context.getGenericType();
		EClassifier cls = MApplicationPackage.eINSTANCE.getEClassifier(context
				.eClass().getName());
		if (cls instanceof EClass)
			return (EClass) cls;
		return null;
	}

	private ParserRule findCurrentParserRule(EObject ctx) {
		CompositeNode node = NodeUtil.getNode(ctx);
		while (node != null) {
			if (node.getGrammarElement() instanceof ParserRule)
				return (ParserRule) node.getGrammarElement();
			if (node.getGrammarElement() instanceof RuleCall) {
				RuleCall rc = (RuleCall) node.getGrammarElement();
				if (rc.getRule().getType().getClassifier() instanceof EClass)
					return (ParserRule) rc.getRule();
			}
			if (node.getGrammarElement() != null)
				return GrammarUtil.containingParserRule(node
						.getGrammarElement());
			node = node.getParent();
		}
		return null;
	}

	private EClass findCurrentExpectedType(EObject ctx) {
		CompositeNode node = NodeUtil.getNode(ctx);
		while (node != null) {
			if (node.getGrammarElement() instanceof ParserRule)
				return (EClass) ((ParserRule) node.getGrammarElement())
						.getType().getClassifier();
			if (node.getGrammarElement() instanceof Action)
				return (EClass) ((Action) node.getGrammarElement()).getType()
						.getClassifier();
			if (node.getGrammarElement() instanceof RuleCall) {
				RuleCall rc = (RuleCall) node.getGrammarElement();
				if (rc.getRule().getType().getClassifier() instanceof EClass)
					return (EClass) ((ParserRule) rc.getRule()).getType()
							.getClassifier();
			}
			node = node.getParent();
		}
		return null;
	}

	public IScope scope_ApplicationElement_genericType(final EObject ctx,
			EReference ref) {
		final IScope scope = getDelegate().getScope(ctx, ref);
		final EClass baseClass = findCurrentExpectedType(ctx);
		if (baseClass == null)
			return scope;
		return new IScope() {

			public IScope getOuterScope() {
				return IScope.NULLSCOPE;
			}

			public Iterable<IEObjectDescription> getContents() {
				return Iterables.filter(scope.getAllContents(),
						new Predicate<IEObjectDescription>() {
							public boolean apply(IEObjectDescription input) {
								EObject o = input.getEObjectOrProxy();
								if (o.eIsProxy())
									o = EcoreUtil.resolve(o, ctx);
								if (o instanceof EClass)
									for (EClass s : ((EClass) o)
											.getEAllSuperTypes())
										if (s.getName().equals(
												baseClass.getName())
												&& s.getEPackage()
														.getNsURI()
														.equals(
																s.getEPackage()
																		.getNsURI()))
											return true;
								return false;
							}
						});
			}

			public Iterable<IEObjectDescription> getAllContents() {
				return getContents();
			}

			public IEObjectDescription getContentByName(String name) {
				return scope.getContentByName(name);
			}

			public IEObjectDescription getContentByEObject(EObject object) {
				return scope.getContentByEObject(object);
			}
		};
	}

	public IScope scope_GenericValue_references(EObject context,
			EReference reference) {
		if (!(context instanceof GenericValue))
			return IScope.NULLSCOPE;
		GenericValue gv = (GenericValue) context;
		if (!(gv.getFeature() instanceof EReference))
			return IScope.NULLSCOPE;
		EReference ref = (EReference) gv.getFeature();
		if (ref.isContainment())
			return IScope.NULLSCOPE;
		if (ref.getEType().getEPackage().getNsURI().equals(
				MApplicationPackage.eNS_URI)) {
			EReference ref2 = EcoreFactory.eINSTANCE.createEReference();
			ref2.setContainment(false);
			ref2.setEType(E4WorkbenchXtextPackage.eINSTANCE.getEClassifier(ref
					.getEType().getName()));
			ref2.setName(ref.getName());
			return getDelegate().getScope(context, ref2);
		}
		return getDelegate().getScope(context, ref);
	}

	public IScope scope_GenericValue_feature(ApplicationElement context,
			EReference reference) {
		EClass cls = getTargetEClass(context);
		if (cls == null)
			return IScope.NULLSCOPE;
		ParserRule pr = findCurrentParserRule(context);
		if (pr == null)
			return Scopes.scopeFor(cls.getEAllStructuralFeatures());
		final Set<String> features = Sets.newHashSet();
		for (Assignment a : GrammarUtil.containedAssignments(pr))
			features.add(a.getFeature());
		return Scopes.scopeFor(Iterables.filter(
				cls.getEAllStructuralFeatures(),
				new Predicate<EStructuralFeature>() {
					public boolean apply(EStructuralFeature input) {
						return !features.contains(input.getName());
					}
				}));
	}
}
