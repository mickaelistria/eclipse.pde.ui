/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.details;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.editor.IDSMaster;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ds.ui.wizards.DSNewClassCreationWizard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class DSProvideDetails extends DSAbstractDetails {

	private IDSProvide fProvide;
	private Section fMainSection;
	private FormEntry fInterfaceEntry;

	public DSProvideDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);
	}

	public void createDetails(Composite parent) {
		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(Messages.DSProvideDetails_mainSectionText);
		fMainSection
				.setDescription(Messages.DSProvideDetails_mainSectionDesc);

		fMainSection.setLayout(FormLayoutFactory
				.createClearGridLayout(false, 1));

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		alignSectionHeaders(fMainSection);

		// Create container for main section
		Composite client = getToolkit()
				.createComposite(fMainSection);
		client.setLayout(FormLayoutFactory
				.createSectionClientGridLayout(false, 3));

		fInterfaceEntry = new FormEntry(client, getToolkit(),
				Messages.DSProvideDetails_interface, Messages.DSProvideDetails_browse,
				isEditable(), 0);
		
		// Bind widgets
		getToolkit().paintBordersFor(client);
		fMainSection.setClient(client);
		markDetailsPart(fMainSection);

	}

	public void hookListeners() {
		IActionBars actionBars = getPDEEditor().getEditorSite()
				.getActionBars();
		fInterfaceEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				fProvide.setInterface(entry.getValue());
			}

			public void linkActivated(HyperlinkEvent e) {
				String value = fInterfaceEntry.getValue();
				value = handleLinkActivated(value, true);
				if (value != null)
					fInterfaceEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_INTERFACES,
						fInterfaceEntry);
			}
		});
	}

	private String handleLinkActivated(String value, boolean isInter) {
		IProject project = getCommonProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$',
						'.'));
				if (element != null)
					JavaUI.openInEditor(element);
				else {
					// TODO create our own wizard for reuse here
					DSNewClassCreationWizard wizard = new DSNewClassCreationWizard(
							project, isInter, value);
					WizardDialog dialog = new WizardDialog(Activator
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					if (dialog.open() == Window.OK) {
						return wizard.getQualifiedName();
					}
				}
			}
		} catch (PartInitException e1) {
		} catch (CoreException e1) {
		}
		return null;
	}
	

	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fProvide == null) {
			return;
		}

		if (fProvide.getInterface() == null) {
			fInterfaceEntry.setValue("", true); //$NON-NLS-1$
		} else {
			// Attribute: interface
			fInterfaceEntry.setValue(fProvide.getInterface(), true);
		}
		fInterfaceEntry.setEditable(editable);

	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSProvide) == false) {
			return;
		}
		// Set data
		setData((IDSProvide) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSProvide object) {
		// Set data
		fProvide = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		fInterfaceEntry.commit();
	}
	
	private void doOpenSelectionDialog(int scopeType, FormEntry entry) {
		try {
			String filter = entry.getValue();
			filter = filter.substring(filter.lastIndexOf(".") + 1); //$NON-NLS-1$
			SelectionDialog dialog = JavaUI.createTypeDialog(Activator
					.getActiveWorkbenchShell(), PlatformUI.getWorkbench()
					.getProgressService(), SearchEngine.createWorkspaceScope(),
					scopeType, false, filter);
			dialog.setTitle(Messages.DSProvideDetails_selectType);
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				entry.setValue(type.getFullyQualifiedName('$'));
				entry.commit();
			}
		} catch (CoreException e) {
		}
	}

}