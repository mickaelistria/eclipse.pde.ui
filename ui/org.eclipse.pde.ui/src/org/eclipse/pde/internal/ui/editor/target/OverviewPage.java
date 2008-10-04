/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;

public class OverviewPage extends PDEFormPage implements IHyperlinkListener {

	public static final String PAGE_ID = "overview"; //$NON-NLS-1$

	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.OverviewPage_title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEUIMessages.OverviewPage_title);
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_DEFINITION));
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.TARGET_OVERVIEW_PAGE);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(true, 2));

		managedForm.addPart(new TargetDefinitionSection(this, body));
		managedForm.addPart(new LocationsSection(this, body));
		createContentsSection(body, toolkit);
		createEnvironmentSection(body, toolkit);
	}

	private void createContentsSection(Composite parent, FormToolkit toolkit) {
		Section section = createSection(parent, toolkit, PDEUIMessages.OverviewPage_contentTitle);
		createText(section, PDEUIMessages.OverviewPage_contentDescription, toolkit);
	}

	private void createEnvironmentSection(Composite parent, FormToolkit toolkit) {
		Section section = createSection(parent, toolkit, PDEUIMessages.OverviewPage_environmentTitle);
		createText(section, PDEUIMessages.OverviewPage_environmentDescription, toolkit);
	}

	private Section createSection(Composite parent, FormToolkit toolkit, String title) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setText(title);
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		return section;
	}

	private FormText createText(Section section, String content, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section, SWT.NONE);
		container.setLayout(FormLayoutFactory.createSectionClientTableWrapLayout(false, 1));
		section.setClient(container);
		FormText text = toolkit.createFormText(container, true);
		try {
			text.setText(content, true, false);
		} catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		data.maxWidth = 250;
		text.setLayoutData(data);
		text.addHyperlinkListener(this);
		return text;
	}

	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		if (href.equals("content")) //$NON-NLS-1$
			getEditor().setActivePage(ContentPage.PAGE_ID);
		else if (href.equals("environment")) //$NON-NLS-1$
			getEditor().setActivePage(EnvironmentPage.PAGE_ID);
	}

	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(e.getLabel());
	}

	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(null);
	}

	protected String getHelpResource() {
		return "/org.eclipse.pde.doc.user/guide/tools/editors/target_definition_editor/overview.htm"; //$NON-NLS-1$
	}

}