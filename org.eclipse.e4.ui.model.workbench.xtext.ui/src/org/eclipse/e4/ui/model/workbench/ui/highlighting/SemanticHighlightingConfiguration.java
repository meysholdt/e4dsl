/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.e4.ui.model.workbench.ui.highlighting;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor;
import org.eclipse.xtext.ui.editor.utils.TextStyle;

/**
 * @author Sven Efftinge - Initial contribution and API
 * 
 */
public class SemanticHighlightingConfiguration extends
		DefaultHighlightingConfiguration {

	public final static String CROSS_REF = "CrossReference";
	public final static String JAVA_REF = "JavaCrossReference";
	public final static String META_REF = "MetaReference";
	public final static String FQN = "FQN";

	@Override
	public void configure(IHighlightingConfigurationAcceptor acceptor) {
		super.configure(acceptor);
		acceptor.acceptDefaultHighlighting(CROSS_REF, "Cross References",
				crossReferenceTextStyle());
		acceptor.acceptDefaultHighlighting(JAVA_REF, "Java References",
				crossJavaReferenceTextStyle());
		acceptor.acceptDefaultHighlighting(META_REF, "Meta References",
				crossMetaReferenceTextStyle());
		acceptor.acceptDefaultHighlighting(FQN, "Name Declarations",
				crossNameDeclarations());
	}

	@Override
	public TextStyle defaultTextStyle() {
		TextStyle textStyle = new TextStyle();
		textStyle.setBackgroundColor(new RGB(255, 255, 255));
		textStyle.setColor(new RGB(0, 0, 0));
		return textStyle;
	}

	public TextStyle crossReferenceTextStyle() {
		TextStyle textStyle = defaultTextStyle().copy();
		textStyle.setStyle(SWT.ITALIC);
		return textStyle;
	}

	public TextStyle crossJavaReferenceTextStyle() {
		TextStyle textStyle = defaultTextStyle().copy();
		textStyle.setColor(new RGB(0, 0, 155));
		textStyle.setStyle(SWT.ITALIC);
		return textStyle;
	}
	
	public TextStyle crossMetaReferenceTextStyle() {
		TextStyle textStyle = defaultTextStyle().copy();
		textStyle.setColor(new RGB(127, 0, 85));
		textStyle.setStyle(SWT.ITALIC);
		return textStyle;
	}

	public TextStyle crossNameDeclarations() {
		TextStyle textStyle = defaultTextStyle().copy();
		textStyle.setStyle(SWT.BOLD);
		return textStyle;
	}
}
