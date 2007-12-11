/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Joern Dinkla <devnull@dinkla.com> - bug 210264
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 114080
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.plugin.AbstractPluginModelBase;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.framework.Version;

public class PluginImportWizardDetailedPage extends BaseImportWizardSecondPage {

	
	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object element) {
			return fModels;
		}
	}

	private Label fCountLabel;
	private TableViewer fAvailableListViewer;
	private Text fFilterText;
	private VersionFilter fVersionFilter;
	private AvailableFilter fFilter;
	// fSelected is used to track the selection in a HashMap so we can efficiently
	// filter selected items out of the available item list
	private HashMap fSelected;
	// this job is used to delay the full filter refresh for 200 milliseconds in case the user is still typing
	private WorkbenchJob fFilterJob;
	// used to block the selection listeners from updating button enablement when programatically removing items
	private boolean fBlockSelectionListeners;
	private Button fAddButton;
	private Button fAddAllButton;
	private Button fRemoveButton;
	private Button fRemoveAllButton;
	private Button fAddRequiredButton;
	private Button fFilterOldVersionButton;
	
	private static final String SETTINGS_SHOW_LATEST = "showLatestPluginsOnly"; //$NON-NLS-1$
	
	private class AvailableFilter extends ViewerFilter {
		private Pattern fPattern;
		
		public AvailableFilter() {
			setPattern("*"); //$NON-NLS-1$
		}
		
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			// filter out any items that are currently selected
			// on a full refresh, these will have been added back to the list
			if (fSelected.containsKey(element))
				return false;
			if (!(element instanceof AbstractPluginModelBase))
				return false;
			String itemID = ((AbstractPluginModelBase)element).getPluginBase().getId();
			if (fPattern.matcher(itemID).matches())
				return true;
			return false;
		}
		
		public boolean setPattern(String newPattern) {
			if (!newPattern.endsWith("*")) //$NON-NLS-1$
				newPattern += "*"; //$NON-NLS-1$
			if (!newPattern.startsWith("*")) //$NON-NLS-1$
				newPattern = "*" + newPattern; //$NON-NLS-1$
			if (fPattern != null) {
				String oldPattern = fPattern.pattern();
				if (newPattern.equals(oldPattern))
					return false;
			}
			fPattern = PatternConstructor.createPattern(newPattern, true);
			return true;
		}
	}
	
	/**
	 * This filter is used to remove older plug-ins from view
	 * 
	 */
	private class VersionFilter extends ViewerFilter {
		private HashMap versions = new HashMap();

		public void setModel(IPluginModelBase[] plugins) {
			if (plugins != null && plugins.length > 0) {
				versions.clear();
			}
			for (int i = 0; i < plugins.length; ++i) {
				String name = plugins[i].getBundleDescription()
						.getSymbolicName();
				Version version = plugins[i].getBundleDescription()
						.getVersion();
				Version oldVersion = (Version) versions.get(name);
				if (oldVersion == null || oldVersion.compareTo(version) < 0) {
					versions.put(name, version);
				}
			}
		}

		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			IPluginModelBase plugin = (IPluginModelBase) element;
			Version hVersion = (Version) versions.get(plugin
					.getBundleDescription().getSymbolicName());
			if (hVersion == null) return true;
			return hVersion.equals(plugin.getBundleDescription().getVersion());
		}
		
		

	}
	
	public PluginImportWizardDetailedPage(String pageName, PluginImportWizardFirstPage firstPage) {
		super(pageName, firstPage);
		setTitle(PDEUIMessages.ImportWizard_DetailedPage_title); 
		setMessage(PDEUIMessages.ImportWizard_DetailedPage_desc);
		fSelected = new HashMap();
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
				
		createScrollArea(container);
		createAvailableList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		createButtonArea(container);
		createImportList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		updateCount();
		
		// create container for buttons
		Composite buttonContainer = new Composite(container, SWT.NONE);
		buttonContainer.setLayout(GridLayoutFactory.fillDefaults().create());
		createComputationsOption(buttonContainer, 3);	
		createFilterOption(buttonContainer, 3);
		
		addViewerListeners();
		addFilter();
		
		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PLUGIN_IMPORT_SECOND_PAGE);
	}
	
	private void createFilterOption(Composite container, int span) {
		Composite parent = new Composite(container, SWT.NONE);
		parent.setLayout(GridLayoutFactory.swtDefaults().margins(5, 0).create());
		fFilterOldVersionButton = new Button(parent, SWT.CHECK);
		fFilterOldVersionButton.setSelection(true);
		fFilterOldVersionButton.setText(PDEUIMessages.ImportWizard_DetailedPage_filterDesc);
		GridData gData = new GridData(GridData.FILL_HORIZONTAL);
		gData.horizontalSpan = span;
		fFilterOldVersionButton.setLayoutData(gData);
		
		if (getDialogSettings().get(SETTINGS_SHOW_LATEST) != null)
			fFilterOldVersionButton.setSelection(getDialogSettings().getBoolean(SETTINGS_SHOW_LATEST));
		else 
			fFilterOldVersionButton.setSelection(true);
		
		fFilterOldVersionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAvailableListViewer.removeFilter(fVersionFilter);
				if(fFilterOldVersionButton.getSelection()) {
					fAvailableListViewer.addFilter(fVersionFilter);
				}
				fAvailableListViewer.getTable().setRedraw(false);
				fAvailableListViewer.refresh();
				fAvailableListViewer.getTable().setRedraw(true);
			}

		});
	}

	private void addFilter() {
		fVersionFilter = new VersionFilter();
		fVersionFilter.setModel(fModels);
		fFilter = new AvailableFilter();
		fAvailableListViewer.addFilter(fFilter);
		fAvailableListViewer.addFilter(fVersionFilter);
		fFilterJob = new WorkbenchJob("FilterJob") { //$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				handleFilter();
				return Status.OK_STATUS;
			}
		};
		fFilterJob.setSystem(true);
	}

	private void initialize(){
		updateButtonEnablement(true, true);
		setPageComplete(false);
	}
	
	private void addViewerListeners() {
		fAvailableListViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleAdd();
			}
		});
				
		fImportListViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleRemove();
			}
		});

		fAvailableListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fBlockSelectionListeners)
					updateSelectionBasedEnablement(event.getSelection(), true);
			}
		});
		
		fImportListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fBlockSelectionListeners)
					updateSelectionBasedEnablement(event.getSelection(), false);
			}
		});
		
		fFilterText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				fFilterJob.cancel();
				fFilterJob.schedule(200);
			}
		});
		
	}

	private Composite createAvailableList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData());

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.ImportWizard_DetailedPage_availableList); 

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 225;
		table.setLayoutData(gd);

		fAvailableListViewer = new TableViewer(table);
		fAvailableListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fAvailableListViewer.setContentProvider(new ContentProvider());
		fAvailableListViewer.setInput(PDECore.getDefault().getModelManager());
		fAvailableListViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		
		return container;
	}
	
	
	private Composite createButtonArea(Composite parent) {
		ScrolledComposite comp = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		Composite container = new Composite(comp, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.verticalIndent = 15;
		container.setLayoutData(gd);
		
		Button button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_existing); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleExistingProjects();
			}
		});
		SWTUtil.setButtonDimensionHint(button);

		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_existingUnshared); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleExistingUnshared();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		fAddButton = new Button(container, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.ImportWizard_DetailedPage_add); 
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		SWTUtil.setButtonDimensionHint(fAddButton);
		
		fAddAllButton = new Button(container, SWT.PUSH);
		fAddAllButton.setText(PDEUIMessages.ImportWizard_DetailedPage_addAll); 
		fAddAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddAll();
			}
		});
		SWTUtil.setButtonDimensionHint(fAddAllButton);
		
		fRemoveButton = new Button(container, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.ImportWizard_DetailedPage_remove); 
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		
		fRemoveAllButton = new Button(container, SWT.PUSH);
		fRemoveAllButton.setText(PDEUIMessages.ImportWizard_DetailedPage_removeAll); 
		fRemoveAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		SWTUtil.setButtonDimensionHint(fRemoveAllButton);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_swap); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSwap();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		fAddRequiredButton = new Button(container, SWT.PUSH);
		fAddRequiredButton.setText(PDEUIMessages.ImportWizard_DetailedPage_addRequired); 
		fAddRequiredButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddRequiredButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddRequiredPlugins();
			}

		});
		SWTUtil.setButtonDimensionHint(fAddRequiredButton);
	
		
		fCountLabel = new Label(container, SWT.NONE);
		fCountLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		comp.setContent(container);
		comp.setMinHeight(250);
		comp.setExpandHorizontal(true);
		comp.setExpandVertical(true);
		return container;
	}
	
	private Composite createScrollArea(Composite parent){
		Group container = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2,false);
		layout.marginWidth = layout.marginHeight = 6;
		container.setLayout(layout);
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=3;
		container.setLayoutData(gd);
		container.setText(PDEUIMessages.ImportWizard_DetailedPage_filter); 
	
		Label filterLabel = new Label(container, SWT.NONE);
		filterLabel.setText(PDEUIMessages.ImportWizard_DetailedPage_search); 
		
		fFilterText = new Text(container, SWT.BORDER);
		fFilterText.setText(""); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fFilterText.setLayoutData(gd);
			
		return container;
	}
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			fFilterText.setFocus();
		setPageComplete(visible && fImportListViewer.getTable().getItemCount() > 0);
		
	}
	protected void refreshPage() {
		fImportListViewer.getTable().removeAll();
		fSelected = new HashMap();
		fFilter.setPattern("*"); //$NON-NLS-1$
		fVersionFilter.setModel(fModels);
		fAvailableListViewer.refresh();
		pageChanged();
	}
	protected void pageChanged() {
		pageChanged(false,false);
	}
	protected void pageChanged(boolean doAddEnablement, boolean doRemoveEnablement) {
		updateCount();
		updateButtonEnablement(doAddEnablement, doRemoveEnablement);
		setPageComplete(fImportListViewer.getTable().getItemCount() > 0);
	}
	private void updateCount() {
		fCountLabel.setText(
			NLS.bind(PDEUIMessages.ImportWizard_DetailedPage_count, (new String[] {
			new Integer(fImportListViewer.getTable().getItemCount()).toString(),
			new Integer(fModels.length).toString()})));
		fCountLabel.getParent().layout();
	}
	private void updateButtonEnablement(boolean doAddEnablement, boolean doRemoveEnablement) {
		int availableCount = fAvailableListViewer.getTable().getItemCount();
		int importCount = fImportListViewer.getTable().getItemCount();
		
		if (doAddEnablement)
			updateSelectionBasedEnablement(fAvailableListViewer.getSelection(), true);
		if (doRemoveEnablement)
			updateSelectionBasedEnablement(fImportListViewer.getSelection(), false);
			
		fAddAllButton.setEnabled(availableCount > 0);
		fRemoveAllButton.setEnabled(importCount > 0);
		fAddRequiredButton.setEnabled(importCount > 0);
	}
	private void updateSelectionBasedEnablement(ISelection theSelection, boolean available) {
		if (available)
			fAddButton.setEnabled(!theSelection.isEmpty());
		else
			fRemoveButton.setEnabled(!theSelection.isEmpty());
	}
	
	private void handleAdd() {
		IStructuredSelection ssel = (IStructuredSelection)fAvailableListViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = fAvailableListViewer.getTable();
			int index = table.getSelectionIndices()[0];
			Object[] selection = ssel.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (int i = 0; i < selection.length; i++) {
				doAdd(selection[i]);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
			pageChanged(true, false);
		}		
	}

	private void handleAddAll() {
		TableItem[] items = fAvailableListViewer.getTable().getItems();

		ArrayList data = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			data.add(items[i].getData());
		}
		if (data.size() > 0) {
			Object[] datas = data.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (int i = 0; i < datas.length; i++) {
				doAdd(datas[i]);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			pageChanged(true, false);
		}
	}
	
	private void handleFilter() {
		boolean changed = false;
		String newFilter;
		if (fFilterText == null || (newFilter = fFilterText.getText().trim()).length() == 0)
			newFilter = "*"; //$NON-NLS-1$
		changed = fFilter.setPattern(newFilter);
		if (changed) {
			fAvailableListViewer.getTable().setRedraw(false);
			fAvailableListViewer.refresh();
			fAvailableListViewer.getTable().setRedraw(true);
			updateButtonEnablement(false, false);
		}
	}
	
	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection)fImportListViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = fImportListViewer.getTable();
			int index = table.getSelectionIndices()[0];
			Object[] selection = ssel.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (int i = 0; i < selection.length; i++) {
				doRemove(selection[i]);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
			pageChanged(false, true);
		}
	}
	
	private void doAdd(Object o) {
		fImportListViewer.add(o);
		fAvailableListViewer.remove(o);
		fSelected.put(o, null);
	}
	
	private void doRemove(Object o) {
		fSelected.remove(o);
		fImportListViewer.remove(o);
		if (fFilter.select(fAvailableListViewer, null, o))
			fAvailableListViewer.add(o);
	}
	// used to prevent flicker during operations that move items between lists
	private void setRedraw(boolean redraw) {
		fAvailableListViewer.getTable().setRedraw(redraw);
		fImportListViewer.getTable().setRedraw(redraw);
	}
	private void handleRemoveAll() {
		TableItem[] items = fImportListViewer.getTable().getItems();
		
		ArrayList data = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			data.add(items[i].getData());
		}
		if (data.size() > 0) {
			Object[] datas = data.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (int i = 0; i < datas.length; i++) {
				doRemove(datas[i]);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			pageChanged(false, true);
		}		
	}
	private void handleSetImportSelection(ArrayList newSelectionList) {
		if (newSelectionList.size() == 0) {
			handleRemoveAll();
			pageChanged();
			return;
		}
		TableItem[] items = fImportListViewer.getTable().getItems();
		Object[] oldSelection = new Object[items.length];
		for (int i = 0; i < items.length; i++) {
			oldSelection[i] = items[i].getData();
		}
		setRedraw(false);
		// remove items that were in the old selection, but are not in the new one
		for (int i = 0; i < oldSelection.length; i++) {
			if (newSelectionList.contains(oldSelection[i])) {
				newSelectionList.remove(oldSelection[i]);
			} else {
				doRemove(oldSelection[i]);
			}
		}
		// add items that were not in the old selection and are in the new one
		Object[] newSelection = newSelectionList.toArray();
		for (int i = 0; i < newSelection.length; i++) {
			doAdd(newSelection[i]);
		}
		setRedraw(true);
		pageChanged();
	}
	
	private void handleSwap() {
		TableItem[] aItems = fAvailableListViewer.getTable().getItems();
		TableItem[] iItems = fImportListViewer.getTable().getItems();

		setRedraw(false);
		ArrayList data = new ArrayList();
		for (int i = 0; i < iItems.length; i++) {
			data.add(iItems[i].getData());
		}
		if (data.size() > 0) {
			Object[] datas = data.toArray();
			for (int i = 0; i < datas.length; i++) {
				doRemove(datas[i]);
			}
		}
		
		data.clear();
		for (int i = 0; i < aItems.length; i++) {
			data.add(aItems[i].getData());
		}
		if (data.size() > 0) {
			Object[] datas = data.toArray();
			for (int i = 0; i < datas.length; i++) {
				doAdd(datas[i]);
			}
		}
		setRedraw(true);
		pageChanged();		
	}
	
	private void handleExistingProjects() {
		ArrayList result = new ArrayList();
		for (int i = 0; i < fModels.length; i++) {
			String id = fModels[i].getPluginBase().getId();
			IProject project =
				(IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
			if (project != null
				&& project.isOpen()
				&& WorkspaceModelManager.isPluginProject(project)) {
				result.add(fModels[i]);
			}
		}
		handleSetImportSelection(result);
	}
	
	private void handleExistingUnshared() {
		ArrayList result = new ArrayList();
		for (int i = 0; i < fModels.length; i++) {
			String id = fModels[i].getPluginBase().getId();
			IProject project =
				(IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
			if (project != null
				&& WorkspaceModelManager.isUnsharedProject(project)
				&& WorkspaceModelManager.isPluginProject(project)) {
				result.add(fModels[i]);
			}
		}
		handleSetImportSelection(result);	
	}

	private void handleAddRequiredPlugins() {
		TableItem[] items = fImportListViewer.getTable().getItems();
		if (items.length == 0)
			return;
		if (items.length == 1) {
			IPluginModelBase model = (IPluginModelBase) items[0].getData();
			if (model.getPluginBase().getId().equals("org.eclipse.core.boot")) { //$NON-NLS-1$
				return;
			}
		}
						
		ArrayList result = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			addPluginAndDependencies((IPluginModelBase) items[i].getData(), result, fAddFragmentsButton.getSelection());
		}
		handleSetImportSelection(result);
	}
	
	public void dispose() {
		fFilterJob.cancel();
	}

	private void setBlockSelectionListeners(boolean blockSelectionListeners) {
		fBlockSelectionListeners = blockSelectionListeners;
	}
	
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(SETTINGS_SHOW_LATEST, fFilterOldVersionButton.getSelection());
		super.storeSettings();
	}
	
}
