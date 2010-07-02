package org.eclipse.e4.ui.model.workbench.validation;

import java.util.Collections;
import java.util.List;

import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.Application;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.ApplicationElement;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.E4WorkbenchXtextPackage;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.KeyBinding;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.Check;

public class TextualWorkbenchJavaValidator extends
		AbstractTextualWorkbenchJavaValidator {

	private final static String PREFIX = "org.eclipse.e4.ui.model.workbench.validation.TextualWorkbenchJavaValidator.";
	public final static String INVALID_KEY_STROKES = PREFIX
			+ "invalid_key_strokes";
	public final static String INVALID_ICON_URI = PREFIX + "invalid_icon_uri";
	public final static String SHOULD_START_WITH_CAPITAL = PREFIX
			+ "invalid_icon_uri";

	@Override
	protected List<EPackage> getEPackages() {
		return Collections
				.<EPackage> singletonList(E4WorkbenchXtextPackage.eINSTANCE);
	}

	@Check
	public void validKeyStroke(KeyBinding keyBinding) {
		try {
			if (keyBinding != null)
				KeySequence.getInstance(keyBinding.getKeySequence());
		} catch (ParseException p) {
			error("Invalid key sequence: " + p.getMessage(), keyBinding,
					E4WorkbenchXtextPackage.KEY_BINDING__KEY_SEQUENCE,
					INVALID_KEY_STROKES);
		}
	}

	@Check
	public void checkIconURI(EObject obj) {
		EStructuralFeature iconUriFeat = obj.eClass().getEStructuralFeature(
				"iconURI");
		if (iconUriFeat == null || !obj.eIsSet(iconUriFeat))
			return;
		String iconURI = obj.eGet(iconUriFeat).toString();
		Application app = EcoreUtil2.getContainerOfType(obj, Application.class);
		URI icon = null;
		try {
			icon = URI.createURI(app.getIconPrefix() + "/" + iconURI);
		} catch (Throwable e) {
			error("No valid URI: " + e.getMessage(), obj.eClass().getFeatureID(
					iconUriFeat));
		}
		if (icon == null)
			return;
		if (obj.eResource().getResourceSet().getURIConverter().exists(icon,
				null))
			return;
		if (icon.isPlatformPlugin()) {
			icon = URI.createPlatformResourceURI(icon.toPlatformString(false),
					false);
			if (obj.eResource().getResourceSet().getURIConverter().exists(icon,
					null))
				return;
		}
		error("The file " + icon + " does not exist.", obj.eClass()
				.getFeatureID(iconUriFeat), INVALID_ICON_URI);
	}

	@Check
	public void checkIdCapital(ApplicationElement ele) {
		if (ele.getId() == null || ele.getId().length() < 1
				|| ele.getId().contains("."))
			return;
		int feature = ele.eClass().getFeatureID(
				E4WorkbenchXtextPackage.eINSTANCE.getApplicationElement_Id());
		if (!Character.isUpperCase(ele.getId().charAt(0)))
			warning("IDs should start with a capital character.", feature,
					SHOULD_START_WITH_CAPITAL);
	}
}
