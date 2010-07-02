
package org.eclipse.e4.ui.model.workbench;

/**
 * Initialization support for running Xtext languages 
 * without equinox extension registry
 */
public class TextualWorkbenchStandaloneSetup extends TextualWorkbenchStandaloneSetupGenerated{

	public static void doSetup() {
		new TextualWorkbenchStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
}

