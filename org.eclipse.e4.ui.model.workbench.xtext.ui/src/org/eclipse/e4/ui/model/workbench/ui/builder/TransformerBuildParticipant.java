package org.eclipse.e4.ui.model.workbench.ui.builder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.MApplicationPackage;
import org.eclipse.e4.ui.model.workbench.e4WorkbenchXtext.Application;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.xtext.builder.IXtextBuilderParticipant;
import org.eclipse.xtext.resource.IResourceDescription.Delta;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class TransformerBuildParticipant implements IXtextBuilderParticipant {

	private List<String> fileExtensions;

	@Inject
	public TransformerBuildParticipant(
			@Named("file.extensions") String fileExtensions) {
		this.fileExtensions = Arrays.asList(fileExtensions.split(","));
	}

	protected boolean isE4XtextResourceURI(URI resourceURI) {
		return fileExtensions.contains(resourceURI.fileExtension());
	}

	public void build(IBuildContext context, IProgressMonitor monitor)
			throws CoreException {
		for (Delta delta : context.getDeltas()) {
			URI xtextResourceURI = delta.getUri();
			if (isE4XtextResourceURI(xtextResourceURI)) {
				Resource xtextResource = context.getResourceSet().getResource(
						xtextResourceURI, true);
				URI xmiResourceURI = URI.createURI(((Application) xtextResource
						.getContents().get(0)).getOutputFile());
				XMIResource xmiResource = (XMIResource) context
						.getResourceSet().createResource(xmiResourceURI);
				transformXtext2Xmi(xtextResource, xmiResource);
				TreeIterator<EObject> i = xmiResource.getAllContents();
				while (i.hasNext()) {
					EObject o = (EObject) i.next();
					if (o instanceof MApplicationElement
							&& ((MApplicationElement) o).getId() != null)
						xmiResource.setID(o, ((MApplicationElement) o).getId());
					else
						xmiResource.setID(o, EcoreUtil.generateUUID());
				}
				// System.out.println(EmfFormatter.objToStr(xmiResource
				// .getContents().get(0)));
				try {
					xmiResource.save(null);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void transformXtext2Xmi(Resource xtextResource,
			XMIResource xmiResource) {
		xmiResource.getContents().clear();
		Transformer trans = new Transformer();
		for (EObject source : xtextResource.getContents()) {
			// System.out.println(EmfFormatter.objToStr(source));
			EObject o = trans.transform(source, MApplicationPackage.eINSTANCE);
			if (!(o instanceof MApplicationElement))
				xmiResource.setID(o, EcoreUtil.generateUUID());
			xmiResource.getContents().add(o);
		}
	}

}
