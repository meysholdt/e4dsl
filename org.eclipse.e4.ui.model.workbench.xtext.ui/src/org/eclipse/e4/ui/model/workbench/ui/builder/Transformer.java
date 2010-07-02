package org.eclipse.e4.ui.model.workbench.ui.builder;

import java.util.List;
import java.util.Map;

import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.Application;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.ApplicationElement;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.Command;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.E4WorkbenchXtextPackage;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.GenericValue;
import org.eclipse.e4.ui.model.workbench.names.TextualWorkbenchNameProvider;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmIdentifyableElement;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.util.EmfFormatter;

import com.google.common.collect.Maps;

public class Transformer {

	public EObject transform(EObject source, EPackage targetPackage) {
		Map<EObject, EObject> source2target = Maps.newHashMap();
		fixCommands(source);
		EObject result = transformContainment(source, targetPackage,
				source2target);
		transformReferences(source, source2target);
		return result;
	}

	private void transformReferences(EObject source,
			Map<EObject, EObject> source2target) {
		EObject target = source2target.get(source);
		if (target == null) {
			System.out.println("The target for " + EmfFormatter.objPath(source)
					+ " is not available");
			return;
		}
		for (EReference sourceReference : source.eClass().getEAllReferences()) {
			if (sourceReference.getEType().getEPackage() == TypesPackage.eINSTANCE)
				continue;
			EReference targetReference = getTargetFeature(sourceReference,
					target.eClass());
			if (targetReference != null && source.eIsSet(sourceReference)) {
				if (sourceReference.isContainment()) {
					if (sourceReference.isMany())
						for (Object o : (List<?>) source.eGet(sourceReference))
							transformReferences((EObject) o, source2target);
					else
						transformReferences((EObject) source
								.eGet(sourceReference), source2target);
				} else {
					if (sourceReference.isMany()) {
						@SuppressWarnings("unchecked")
						EList<Object> s = (EList<Object>) source
								.eGet(sourceReference);
						@SuppressWarnings("unchecked")
						EList<Object> t = (EList<Object>) target
								.eGet(targetReference);
						for (Object o : s)
							t.add(source2target.get(o));
					} else {
						EObject x = source2target.get(source
								.eGet(sourceReference));
						target.eSet(targetReference, x);
					}
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	private <T extends EStructuralFeature> T getTargetFeature(
			EStructuralFeature sourceFeature, EClass targetType) {
		EStructuralFeature targetFeature = targetType
				.getEStructuralFeature(sourceFeature.getName());
		if (targetFeature == null) {
			System.out.println("EClass '" + targetType.getName()
					+ "' in EPackage '" + targetType.getEPackage().getName()
					+ "' contains no feature named '" + sourceFeature.getName()
					+ "'");
			return null;
		}
		if (sourceFeature.eClass() != targetFeature.eClass()
				|| sourceFeature.isMany() != targetFeature.isMany()) {
			System.out.println("Features '"
					+ sourceFeature.getEContainingClass().getName() + "."
					+ sourceFeature.getName()
					+ "' of source and target are not compatible.");
			return null;
		}
		return (T) targetFeature;
	}

	@SuppressWarnings("unchecked")
	private <T extends EClassifier> T getTargetClassifier(T sourceType,
			EPackage targetPackage) {
		EClassifier targetType = targetPackage.getEClassifier(sourceType
				.getName());
		if (targetType == null) {
			System.out.println("EClass '" + sourceType.getName()
					+ "' not found in package '" + targetPackage.getName()
					+ "'");
			return null;
		}
		if (sourceType.eClass() != targetType.eClass()) {
			System.out.println("Types '" + sourceType.getName()
					+ "' are incompatible.");
			return null;
		}
		return (T) targetType;
	}

	private void fixCommands(EObject root) {
		Application app = (Application) root;
		for (Command command : app.getCommands()) {
			if (command.getBinding() != null) {
				command.getBinding().setCommand(command);
				app.getBindings().add(command.getBinding());
			}
			if (command.getHandler() != null) {
				command.getHandler().setCommand(command);
				app.getHandlers().add(command.getHandler());
			}
		}
	}

	private EObject transformContainment(EObject source,
			EPackage targetPackage, Map<EObject, EObject> source2target) {
		EClass targetType = null;
		if (source instanceof ApplicationElement
				&& ((ApplicationElement) source).getGenericType() != null)
			targetType = ((ApplicationElement) source).getGenericType();
		else
			targetType = getTargetClassifier(source.eClass(), targetPackage);
		if (targetType == null)
			return null;
		EObject target = targetType.getEPackage().getEFactoryInstance().create(
				targetType);
		source2target.put(source, target);
		for (EStructuralFeature sourceFeature : source.eClass()
				.getEAllStructuralFeatures()) {
			if (!source.eIsSet(sourceFeature))
				continue;
			if (sourceFeature == E4WorkbenchXtextPackage.eINSTANCE
					.getApplicationElement_GenericType()
					|| sourceFeature == E4WorkbenchXtextPackage.eINSTANCE
							.getCommand_Binding()
					|| sourceFeature == E4WorkbenchXtextPackage.eINSTANCE
							.getCommand_Handler())
				continue;
			if (sourceFeature.getEType().getEPackage() == TypesPackage.eINSTANCE) {
				transformJavaType(target, (EReference) sourceFeature, source);
				continue;
			}
			if (sourceFeature == E4WorkbenchXtextPackage.eINSTANCE
					.getApplicationElement_GenericValues()) {
				transformGenericValue(target, (ApplicationElement) source);
				continue;
			}
			EStructuralFeature targetFeature = getTargetFeature(sourceFeature,
					targetType);
			if (sourceFeature == E4WorkbenchXtextPackage.eINSTANCE
					.getApplicationElement_Id()) {
				transformID(target, (EAttribute) targetFeature,
						(EAttribute) sourceFeature, source);
				continue;
			}
			if (sourceFeature.getName().equals("iconURI")) {
				transformIconURI(target, (EAttribute) targetFeature,
						(EAttribute) sourceFeature, source);
				continue;
			}
			if (targetFeature != null
					&& !sourceFeature.isTransient()
					&& targetFeature.isChangeable()
					&& (!(sourceFeature instanceof EReference && !((EReference) sourceFeature)
							.isContainment()))) {
				if (sourceFeature.isMany()) {
					@SuppressWarnings("unchecked")
					EList<Object> s = (EList<Object>) source
							.eGet(sourceFeature);
					@SuppressWarnings("unchecked")
					EList<Object> t = (EList<Object>) target
							.eGet(targetFeature);
					for (Object o : s) {
						Object x = transformValue(sourceFeature, o,
								targetPackage, source2target);
						if (x != null)
							t.add(x);
					}
				} else {
					Object x = transformValue(sourceFeature, source
							.eGet(sourceFeature), targetPackage, source2target);
					if (x != null)
						target.eSet(targetFeature, x);
				}
			}
		}
		return target;
	}

	@SuppressWarnings("unchecked")
	private void transformGenericValue(EObject target, ApplicationElement source) {
		for (GenericValue value : source.getGenericValues()) {
			if (value.getFeature() instanceof EAttribute) {
				if (value.getFeature().isMany()) {
					List<Object> l = (List<Object>) target.eGet(value
							.getFeature());
					for (String v : value.getValues()) {
						Object o = value.getFeature().getEType().getEPackage()
								.getEFactoryInstance().createFromString(
										(EDataType) value.getFeature()
												.getEType(), v);
						l.add(o);
					}
				} else {
					Object o = value.getFeature().getEType().getEPackage()
							.getEFactoryInstance().createFromString(
									(EDataType) value.getFeature().getEType(),
									value.getValues().get(0));
					target.eSet(value.getFeature(), o);
				}
			}
		}

	}

	private void transformIconURI(EObject target, EAttribute targetFeature,
			EAttribute sourceFeature, EObject source) {
		Application app = EcoreUtil2.getContainerOfType(source,
				Application.class);
		if (app != null) {
			String val = (String) source.eGet(sourceFeature);
			if (!val.startsWith("_"))
				target.eSet(targetFeature, app.getIconPrefix() + "/" + val);
		}
	}

	private IQualifiedNameProvider nameProvider = new TextualWorkbenchNameProvider();

	private void transformID(EObject target, EAttribute targetFeature,
			EAttribute sourceFeature, EObject source) {
		if (source.eIsSet(sourceFeature)) {
			String val = (String) nameProvider.getQualifiedName(source);
			if (val != null && !val.startsWith("_")) {
				target.eSet(targetFeature, val);
			}
		}
	}

	private void transformJavaType(EObject target, EReference sourceFeature,
			EObject source) {
		String targetName = sourceFeature.getName().substring(0,
				sourceFeature.getName().length() - 3);
		EAttribute targetAttribute = (EAttribute) target.eClass()
				.getEStructuralFeature(targetName);
		String project = source.eResource().getURI().segment(1);
		JvmIdentifyableElement jvm = (JvmIdentifyableElement) source
				.eGet(sourceFeature);
		String fqn = jvm.getCanonicalName();
		URI uri = URI.createPlatformPluginURI("/" + project + "/" + fqn, true);
		target.eSet(targetAttribute, uri.toString());
	}

	private Object transformValue(EStructuralFeature sourceFeature,
			Object source, EPackage targetPackage,
			Map<EObject, EObject> source2target) {
		if (source instanceof EObject)
			return transformContainment((EObject) source, targetPackage,
					source2target);
		else if (sourceFeature.getEType() instanceof EEnum)
			return transformEnum(sourceFeature, source, targetPackage);
		return source;
	}

	private Object transformEnum(EStructuralFeature sourceFeature,
			Object source, EPackage targetPackage) {
		EEnum targetType = getTargetClassifier(
				(EEnum) sourceFeature.getEType(), targetPackage);
		if (targetType == null)
			return null;
		EEnumLiteral target = targetType.getEEnumLiteral(((Enumerator) source)
				.getName());
		if (target == null) {
			System.out.println("Enum-Literal '"
					+ sourceFeature.getEType().getEPackage().getName() + "."
					+ sourceFeature.getEType().getName() + "."
					+ ((Enumerator) source).getName()
					+ "' not found in package '" + targetPackage + "."
					+ targetType.getName() + "'");
			return null;
		}
		return target.getInstance();
	}

}
