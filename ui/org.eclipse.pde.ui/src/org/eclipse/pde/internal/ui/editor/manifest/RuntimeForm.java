package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.*;

public class RuntimeForm extends ScrollableSectionForm {
	private ManifestRuntimePage page;
	private LibrarySection librarySection;
	public static final String TITLE = "ManifestEditor.RuntimeForm.title";
	private JarsSection jarsSection;
	private ExportSection exportSection;
	private PackagePrefixesSection prefixesSection;

public RuntimeForm(ManifestRuntimePage page) {
	this.page = page;
	//setScrollable(false);
	setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	layout.makeColumnsEqualWidth=true;
	librarySection = new LibrarySection(page);
	Control control = librarySection.createControl(parent, getFactory());
	GridData gd = new GridData(GridData.FILL_BOTH);
	//gd.widthHint = 250;
	//gd.heightHint = 300;
	control.setLayoutData(gd);

	exportSection = new ExportSection(page);
	control = exportSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	jarsSection = new JarsSection(page);
	control = jarsSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	
	prefixesSection = new PackagePrefixesSection(page);
	prefixesSection.setCollapsable(true);
	prefixesSection.setCollapsed(true);
	control = prefixesSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(librarySection, exportSection);
	manager.linkSections(librarySection, jarsSection);
	manager.linkSections(librarySection, prefixesSection);

	registerSection(librarySection);
	registerSection(exportSection);
	registerSection(jarsSection);
	registerSection(prefixesSection);

	if (((ManifestEditor)page.getEditor()).isFragmentEditor()	)
		WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME);
	else
		WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_PLUGIN_RUNTIME);		
}
public void expandTo(Object object) {
   librarySection.expandTo(object);
}
public void initialize(Object model) {
	setHeadingText(PDEPlugin.getResourceString(TITLE));
	super.initialize(model);
	((Composite)getControl()).layout(true);
}
}
