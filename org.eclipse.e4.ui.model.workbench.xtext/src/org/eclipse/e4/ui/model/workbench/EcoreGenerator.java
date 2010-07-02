package org.eclipse.e4.ui.model.workbench;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.codegen.ecore.generator.Generator;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenBase;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenJDKLevel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenClassGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenEnumGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenPackageGeneratorAdapter;
import org.eclipse.emf.codegen.util.ImportManager;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.mwe.core.ConfigurationException;
import org.eclipse.emf.mwe.core.WorkflowContext;
import org.eclipse.emf.mwe.core.issues.Issues;
import org.eclipse.emf.mwe.core.lib.AbstractWorkflowComponent;
import org.eclipse.emf.mwe.core.monitor.ProgressMonitor;
import org.eclipse.xtext.resource.ClasspathUriUtil;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.internal.Maps;

/**
 * Der EcoreGenerator wird von den MWE-Workflows verwendet, um aus Ecore- und
 * XSD-Modellen Javacode zu generieren. Dazu delegiert der EcoreGenerator an den
 * Codegenerator des EMFs und kuemmert sich um die automatische Erstellung der
 * Genmodels (und damit die Parametrisierung der Codegenerierung) und nimmt eine
 * Anpassung am Codegenerierungsprozess vor: Mittels
 * {@link #setOverrideClasses(OverrideClasses)} kann ein JavaPackage definiert
 * werden, dessen enthaltene Java Klassen automatisch in den generierten Code
 * integriert werden. Konkret sieht das so aus, dass wenn in der Codegenerierung
 * Import-Statements erstellt werden, das das Vorhandensein einer Java Klasse
 * mit einem bestimmten Namensmuster in dem besagten Java Package ueberprueft
 * wird. Ist diese Vorhanden, so wird sie anstelle Importiert und auch ihre Name
 * im generierten Code verwendet. So lassen sich beliebige eigene
 * Implementierungen in den generierten Code "Injizieren", ohne den generierten
 * Code anpassen zu muessen.
 * 
 * @author Moritz Eysholdt
 */
public class EcoreGenerator extends AbstractWorkflowComponent {

	private class ImportManagerHack extends ImportManager {

		private Map<String, String> classMapping;

		private Map<String, GenClass> genClasses;

		private GenModel genmodel;

		public ImportManagerHack(GenModel genmodel,
				String compilationUnitPackage) {
			super(compilationUnitPackage);
			this.genmodel = genmodel;
		}

		private Map<String, GenClass> getGenClasses() {
			if (genClasses == null) {
				genClasses = new HashMap<String, GenClass>();
				TreeIterator<EObject> it = genmodel.eAllContents();
				while (it.hasNext()) {
					EObject o = it.next();
					if (o instanceof GenClass)
						genClasses.put(((GenClass) o).getQualifiedClassName(),
								(GenClass) o);
				}
			}
			return genClasses;
		}

		@Override
		public String getImportedName(String qualifiedName, boolean autoImport) {
			String mapped = getMappedClass(qualifiedName);
			if (mapped != null) {
				log.debug("mapping " + qualifiedName + " to " + mapped);
				return super.getImportedName(mapped, autoImport);
			} else
				return super.getImportedName(qualifiedName, autoImport);
		}

		private String getMappedClass(String qualifiedName) {
			if (classMapping == null)
				classMapping = new HashMap<String, String>(
						overrideClasses.mappings);
			if (classMapping.containsKey(qualifiedName))
				return classMapping.get(qualifiedName);
			GenClass cls = getGenClasses().get(qualifiedName);
			if (cls != null) {
				String fn = overrideClasses.getOverrideFileName(cls.getName());
				URIConverter uric = genmodel.eResource().getResourceSet()
						.getURIConverter();
				if (uric.exists(URI.createURI(fn), Collections.emptyMap())) {
					String m = overrideClasses.getOverrideQualifiedName(cls
							.getName());
					classMapping.put(qualifiedName, m);
					return m;
				}
			}
			return null;
		}

	}

	public static class Mapping {
		private String from;
		private String to;

		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getTo() {
			return to;
		}

		public void setTo(String to) {
			this.to = to;
		}
	}

	public static class OverrideClasses {
		private String format;
		private String path;
		private String pkg;
		private Map<String, String> mappings = Maps.newHashMap();

		public void addMapping(Mapping m) {
			mappings.put(m.getFrom(), m.getTo());
		}

		public String getFormat() {
			return format;
		}

		protected String getFormattedName(String name) {
			if (format == null || "".equals(format))
				return name;
			return MessageFormat.format(format, name);
		}

		protected String getOverrideFileName(String name) {
			StringBuffer buf = new StringBuffer();
			buf.append(path);
			buf.append("/");
			buf.append(pkg.replace('.', '/'));
			buf.append("/");
			buf.append(getFormattedName(name));
			buf.append(".java");
			return buf.toString();
		}

		protected String getOverrideQualifiedName(String name) {
			StringBuffer buf = new StringBuffer();
			buf.append(pkg);
			buf.append(".");
			buf.append(getFormattedName(name));
			return buf.toString();
		}

		public String getPath() {
			return path;
		}

		public String getPkg() {
			return pkg;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public void setPackage(String pkg) {
			this.pkg = pkg;
		}

		public void setPath(String dir) {
			this.path = dir;
		}

	}

	public static class PkgInfo {
		private String file;
		private String fileExt;

		public String getEcore() {
			return file;
		}

		public String getFileExt() {
			return fileExt;
		}

		public void setEcore(String file) {
			this.file = file;
		}

		public void setFileExt(String fileExt) {
			this.fileExt = fileExt;
		}

	}

	private static Adapter DONT_SAVE = new AdapterImpl();

	private static Logger log = Logger.getLogger(EcoreGenerator.class);
	static {
		EcorePackage.eINSTANCE.getEFactoryInstance();
		GenModelPackage.eINSTANCE.getEFactoryInstance();
	}
	protected String basePkg;
	protected String ecorePath;
	public ResourceSet resourceSet = new XtextResourceSet();
	private String editDirectory = null;
	private String editorDirectory = null;
	private String editorPluginID = null;
	private String editPluginID = null;
	private boolean generateEdit = false;
	private boolean generateEditor = false;
	protected String genmodelPath;
	protected String javaPath;
	protected String modelName;
	protected OverrideClasses overrideClasses;
	protected Map<String, PkgInfo> pkgInfo = new HashMap<String, PkgInfo>();

	protected String pluginName;

	protected String urisString;

	public void addEcorePackage(PkgInfo pkg) {
		EPackage e = (EPackage) resourceSet
				.getResource(URI.createURI(pkg.getEcore()), true).getContents()
				.get(0);
		e.eResource().eAdapters().add(DONT_SAVE);
		pkgInfo.put(e.getNsURI(), pkg);
	}

	public void checkConfiguration(Issues issues) {
	}

	public void generateEcoreJavaClasses(Collection<? extends EPackage> packs,
			final String basePackage, final String javaPath,
			final String ecorePath, final String genmodelPath,
			final String modelName, final String modelPLuginID)
			throws ConfigurationException {
		Collection<? extends GenPackage> usedGenPackages = getUsedGenPackages(resourceSet);
		for (GenPackage gp : usedGenPackages)
			if (packs.contains(gp.getEcorePackage()))
				packs.remove(gp.getEcorePackage());

		final GenModel genModel = GenModelPackage.eINSTANCE
				.getGenModelFactory().createGenModel();
		for (EPackage p : packs) {
			if (ecorePath != null)
				resourceSet
						.createResource(
								URI.createURI(ecorePath + "/" + p.getName()
										+ ".ecore")).getContents().add(p);
			else if (p.eResource() == null) {
				Resource r = resourceSet.createResource(URI.createURI(p
						.getName() + ".ecore"));
				r.eAdapters().add(DONT_SAVE);
				r.getContents().add(p);
			}
		}
		if (genmodelPath != null)
			resourceSet
					.createResource(
							URI.createURI(genmodelPath + "/" + modelName
									+ ".genmodel")).getContents().add(genModel);
		else {
			Resource r = resourceSet.createResource(URI.createURI(modelName
					+ ".genmodel"));
			r.getContents().add(genModel);
			r.eAdapters().add(DONT_SAVE);
		}

		genModel.initialize(packs);
		genModel.setModelDirectory(javaPath);
		genModel.setModelName(modelName.toLowerCase());
		genModel.setModelPluginID(modelPLuginID);
		if (editDirectory != null)
			genModel.setEditDirectory(editDirectory);
		if (editorDirectory != null)
			genModel.setEditorDirectory(editorDirectory);
		if (editPluginID != null)
			genModel.setEditPluginID(editPluginID);
		if (editorPluginID != null)
			genModel.setEditorPluginID(editorPluginID);

		genModel.setValidateModel(false);
		genModel.setForceOverwrite(true);
		genModel.setFacadeHelperClass(null);
		genModel.setBundleManifest(true);
		genModel.setUpdateClasspath(false);
		genModel.setComplianceLevel(GenJDKLevel.JDK50_LITERAL);
		genModel.setRootExtendsClass("org.eclipse.emf.ecore.impl.MinimalEObjectImpl$Container");

		for (GenPackage genPackage : genModel.getGenPackages()) {
			genPackage.setBasePackage(basePackage);
			if (pkgInfo != null) {
				PkgInfo i = pkgInfo
						.get(genPackage.getEcorePackage().getNsURI());
				if (i != null && i.getFileExt() != null)
					genPackage.setFileExtensions(i.getFileExt());
			}
		}
		genModel.getUsedGenPackages().addAll(usedGenPackages);

		// write models
		for (Resource r : resourceSet.getResources())
			if (!r.eAdapters().contains(DONT_SAVE)
					&& !ClasspathUriUtil.isClasspathUri(r.getURI()))
				try {
					r.save(null);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}

		genModel.setCanGenerate(true);
		genModel.reconcile();

		Generator generator = new Generator();
		generator.getAdapterFactoryDescriptorRegistry().addDescriptor(
				GenModelPackage.eNS_URI,
				new GeneratorAdapterFactory.Descriptor() {
					public GeneratorAdapterFactory createAdapterFactory() {
						return new GenModelGeneratorAdapterFactory() {

							@Override
							public Adapter createGenClassAdapter() {
								return new GenClassGeneratorAdapter(this) {
									@Override
									protected void createImportManager(
											String packageName, String className) {
										if (overrideClasses != null) {
											importManager = new ImportManagerHack(
													genModel, packageName);
											importManager.addMasterImport(
													packageName, className);
											if (generatingObject != null)

												((GenBase) generatingObject)
														.getGenModel()
														.setImportManager(
																importManager);
										} else
											super.createImportManager(
													packageName, className);
									}
								};
							}

							@Override
							public Adapter createGenEnumAdapter() {
								return new GenEnumGeneratorAdapter(this) {
									@Override
									protected void createImportManager(
											String packageName, String className) {
										if (overrideClasses != null) {
											importManager = new ImportManagerHack(
													genModel, packageName);
											importManager.addMasterImport(
													packageName, className);
											if (generatingObject != null)

												((GenBase) generatingObject)
														.getGenModel()
														.setImportManager(
																importManager);
										} else
											super.createImportManager(
													packageName, className);
									}
								};
							}

							@Override
							public Adapter createGenModelAdapter() {
								if (genModelGeneratorAdapter == null) {
									genModelGeneratorAdapter = new GenModelGeneratorAdapter(
											this) {
										@Override
										protected void createImportManager(
												String packageName,
												String className) {
											if (overrideClasses != null) {
												importManager = new ImportManagerHack(
														genModel, packageName);
												importManager.addMasterImport(
														packageName, className);
												if (generatingObject != null)

													((GenBase) generatingObject)
															.getGenModel()
															.setImportManager(
																	importManager);
											} else
												super.createImportManager(
														packageName, className);
										}

									};
								}
								return genModelGeneratorAdapter;
							}

							@Override
							public Adapter createGenPackageAdapter() {
								return new GenPackageGeneratorAdapter(this) {
									@Override
									protected void createImportManager(
											String packageName, String className) {
										if (overrideClasses != null) {
											importManager = new ImportManagerHack(
													genModel, packageName);
											importManager.addMasterImport(
													packageName, className);
											if (generatingObject != null)

												((GenBase) generatingObject)
														.getGenModel()
														.setImportManager(
																importManager);
										} else
											super.createImportManager(
													packageName, className);
									}

								};
							}
						};
					}
				});

		generator.setInput(genModel);
		Diagnostic diagnostic = generator.generate(genModel,
				GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE, new BasicMonitor());

		if (diagnostic.getSeverity() != Diagnostic.OK)
			log.info(diagnostic);

		if (generateEdit) {
			Diagnostic editDiag = generator.generate(genModel,
					GenBaseGeneratorAdapter.EDIT_PROJECT_TYPE,
					new BasicMonitor());
			if (editDiag.getSeverity() != Diagnostic.OK)
				log.info(editDiag);
		}

		if (generateEditor) {
			Diagnostic editorDiag = generator.generate(genModel,
					GenBaseGeneratorAdapter.EDITOR_PROJECT_TYPE,
					new BasicMonitor());
			if (editorDiag.getSeverity() != Diagnostic.OK)
				log.info(editorDiag);
		}
	}

	private Collection<? extends GenPackage> getUsedGenPackages(ResourceSet rs) {
		Set<GenPackage> result = new LinkedHashSet<GenPackage>();
		if (urisString != null) {
			for (String uri : urisString.split(",")) {
				try {
					Resource resource = rs.getResource(
							URI.createURI(uri.trim()), true);
					resource.eAdapters().add(DONT_SAVE);
					GenModel genmodel = (GenModel) resource.getContents()
							.get(0);
					EList<GenPackage> genPackages = genmodel.getGenPackages();
					for (GenPackage genPackage : genPackages) {
						genPackage.getEcorePackage().getEClassifiers();
						result.add(genPackage);
					}
				} catch (Exception e) {
					log.error("Couldn't find genmodel for uri '" + uri + "'");
					throw new RuntimeException(e);
				}
			}
		}
		return result;
	}

	@Override
	protected void invokeInternal(WorkflowContext ctx, ProgressMonitor monitor,
			Issues issues) {
		ArrayList<EPackage> pkgs = new ArrayList<EPackage>();
		if (resourceSet != null)
			for (Resource r : resourceSet.getResources())
				for (EObject o : r.getContents())
					if (o instanceof EPackage)
						pkgs.add((EPackage) o);
		generateEcoreJavaClasses(pkgs, basePkg, javaPath, ecorePath,
				genmodelPath, modelName, pluginName);
	}

	public void setBasePackage(String basePkg) {
		this.basePkg = basePkg;
	}

	public void setEcorePath(String ecorePath) {
		this.ecorePath = ecorePath;
	}

	public void setEditDirectory(String editDirectory) {
		this.editDirectory = editDirectory;
	}

	/**
	 * Sets the target directory for the generated EMF-editor code. Only needed
	 * if you want to generate an EMF editor plug_in.
	 * 
	 * @param editDirectory
	 */
	public void setEditorDirectory(String editorDirectory) {
		this.editorDirectory = editorDirectory;
	}

	/**
	 * Sets the plug-in ID of the generated EMF editor plug-in. Only needed if
	 * you want to generate an EMF editor plug_in.
	 * 
	 * @param editPluginId
	 */
	public void setEditorPluginID(String editorPluginId) {
		editorPluginID = editorPluginId;
	}

	/**
	 * Sets the plug-in ID of the generated EMF edit plug-in. Only needed if you
	 * want to generate an EMF editor plug_in.
	 * 
	 * @param editPluginId
	 */
	public void setEditPluginID(String editPluginId) {
		editPluginID = editPluginId;
	}

	/**
	 * If true, the EMF-edit code will be generated as well.
	 * 
	 * @param gen
	 * @see #setEditDirectory(String)
	 * @see #setEditPluginID(String)
	 */
	public void setGenerateEdit(boolean gen) {
		this.generateEdit = gen;
	}

	/**
	 * If true, the EMF editor code will be generated as well.
	 * 
	 * @param gen
	 * @see #setEditorDirectory(String)
	 * @see #setEditorPluginID(String)
	 */
	public void setGenerateEditor(boolean gen) {
		this.generateEditor = gen;
	}

	public void setGenmodelPath(String genmodelPath) {
		this.genmodelPath = genmodelPath;
	}

	public void setGenModels(String uris) {
		if ("".equals(uris))
			return;
		this.urisString = uris;
	}

	public void setJavaPath(String javaPath) {
		this.javaPath = javaPath;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public void setOverrideClasses(OverrideClasses overrideClasses) {
		this.overrideClasses = overrideClasses;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public void setXmiPath(String xmiString) {
		this.ecorePath = this.genmodelPath = xmiString;
	}

}
