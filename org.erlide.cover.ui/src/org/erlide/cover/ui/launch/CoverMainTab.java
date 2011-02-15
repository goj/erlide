package org.erlide.cover.ui.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlProject;
import org.erlide.cover.runtime.launch.FrameworkType;
import org.erlide.cover.runtime.launch.ICoverAttributes;
import org.erlide.cover.runtime.launch.LaunchType;
import org.erlide.cover.ui.launch.helpers.ProjectElement;
import org.erlide.cover.ui.launch.helpers.ProjectLabelProvider;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.ui.editors.erl.outline.ErlangElementImageProvider;

/**
 * Main panel of EUnit run configuration
 * 
 * @author Aleksandra Lipiec <aleksandra.lipiec@erlang.solutions.com>
 * 
 */
public class CoverMainTab extends AbstractLaunchConfigurationTab {

    private ItemBrowser projectMBr; // projects browser
    private ItemBrowser moduleBr; // module browser
    private ItemBrowser fileBr; // file/directory browser
    private ItemBrowser projectAppBr; // project browser for applications
    private ItemBrowser appBr; // application browser
    private Button singleRadio; // radio button for single module
    private Button allRadio; // radio button for running all tests
    private Button appRadio; // radio button for application
    private Combo testCombo; // framework

    private ElementListSelectionDialog moduleDialog;

    public void createControl(final Composite parent) {
        final Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);

        final GridLayout mainLayout = new GridLayout();
        mainLayout.numColumns = 3;
        mainLayout.verticalSpacing = 15;
        mainLayout.horizontalSpacing = 20;
        mainLayout.marginHeight = 15;
        mainLayout.marginHeight = 15;
        comp.setLayout(mainLayout);

        final GridData gData = new GridData();
        gData.horizontalAlignment = GridData.FILL;
        gData.horizontalSpan = 3;
        // gData.widthHint = 400;

        singleRadio = new Button(comp, SWT.RADIO);
        singleRadio.setText("Run tests for a single module");
        singleRadio.setLayoutData(gData);
        singleRadio.addSelectionListener(radioSelectionListener);

        createModuleGroup(comp);

        allRadio = new Button(comp, SWT.RADIO);
        allRadio.setText("Run all tests in specific project");
        allRadio.setLayoutData(gData);
        allRadio.addSelectionListener(radioSelectionListener);

        createAllTestsGroup(comp);

        appRadio = new Button(comp, SWT.RADIO);
        appRadio.setText("Run tests for an application");
        appRadio.setLayoutData(gData);
        appRadio.addSelectionListener(radioSelectionListener);
        appRadio.setEnabled(false);

        createApplicationGroup(comp);

        final Label testLabel = new Label(comp, SWT.NONE);
        testLabel.setText("Testing framework: ");
        testCombo = new Combo(comp, SWT.NONE);
        testCombo.setItems(new String[] { FrameworkType.EUNIT.getRepr(),
                FrameworkType.CT.getRepr(), FrameworkType.QC.getRepr() });
        testCombo.addModifyListener(basicModifyListener);

        Collection<IErlProject> projects;
        try {
            projects = ErlangCore.getModel().getErlangProjects();
            final List<String> ps = new ArrayList<String>();
            for (final IErlProject p : projects) {
                ps.add(p.getName());
            }
        } catch (final ErlModelException e) {
            ErlLogger.warn(e);
        }
    }

    public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
        config.setAttribute(ICoverAttributes.PROJECT, "");
        config.setAttribute(ICoverAttributes.MODULE, "");
        config.setAttribute(ICoverAttributes.FILE, "");
        config.setAttribute(ICoverAttributes.APPLICATION, "");
        config.setAttribute(ICoverAttributes.TYPE, LaunchType.MODULE.toString());
        config.setAttribute(ICoverAttributes.COMBO, "0");
    }

    public void initializeFrom(final ILaunchConfiguration config) {

        try {

            final String projectName = config.getAttribute(
                    ICoverAttributes.PROJECT, "");

            projectMBr.setText(projectName);

            if (projectName != null && projectName.length() > 0) {
                final IErlProject p = ErlangCore.getModel().getErlangProject(
                        projectName);
                if (p != null) {
                    moduleDialog.setElements(createModuleArray(p));
                }
            }

        } catch (final CoreException e) {
            projectMBr.setText("");
        }

        try {
            moduleBr.setText(config.getAttribute(ICoverAttributes.MODULE, ""));
        } catch (final CoreException e) {
            moduleBr.setText("");
        }

        try {
            fileBr.setText(config.getAttribute(ICoverAttributes.FILE, ""));
        } catch (final CoreException e) {
            fileBr.setText("");
        }

        try {
            projectAppBr.setText(config.getAttribute(
                    ICoverAttributes.APP_PROJECT, ""));
        } catch (final CoreException e) {
            projectAppBr.setText("");
        }

        try {
            appBr.setText(config.getAttribute(ICoverAttributes.APPLICATION, ""));
        } catch (final CoreException e) {
            appBr.setText("");
        }

        try {
            final String type = config.getAttribute(ICoverAttributes.TYPE,
                    LaunchType.MODULE.toString());

            final LaunchType typeT = LaunchType.valueOf(type);
            switch (typeT) {
            case MODULE:

                singleRadio.setSelection(true);

                projectMBr.setEnabled(true);
                moduleBr.setEnabled(true);
                fileBr.setEnabled(false);
                projectAppBr.setEnabled(false);
                appBr.setEnabled(false);

                break;

            case ALL:

                allRadio.setSelection(true);

                projectMBr.setEnabled(false);
                moduleBr.setEnabled(false);
                fileBr.setEnabled(true);
                projectAppBr.setEnabled(false);
                appBr.setEnabled(false);

                break;

            case APPLICATION:

                appRadio.setSelection(true);

                projectMBr.setEnabled(false);
                moduleBr.setEnabled(false);
                fileBr.setEnabled(false);
                projectAppBr.setEnabled(true);
                appBr.setEnabled(true);

                break;

            default:

                singleRadio.setSelection(true);

                projectMBr.setEnabled(true);
                moduleBr.setEnabled(true);
                fileBr.setEnabled(false);
                projectAppBr.setEnabled(false);
                appBr.setEnabled(false);

                break;
            }
        } catch (final CoreException e) {
            singleRadio.setSelection(true);
        }

        String combo;
        try {
            combo = config.getAttribute(ICoverAttributes.COMBO,
                    FrameworkType.EUNIT.getRepr());
            final int idx = testCombo.indexOf(combo);
            testCombo.select(idx);
        } catch (final CoreException e) {
            testCombo.select(0);
        }

    }

    public void performApply(final ILaunchConfigurationWorkingCopy config) {

        // TODO: inform, when nothing sellected; is it possible?
        LaunchType type;
        if (allRadio.getSelection()) {
            type = LaunchType.ALL;
        } else if (appRadio.getSelection()) {
            type = LaunchType.APPLICATION;
        } else {
            type = LaunchType.MODULE;
        }

        config.setAttribute(ICoverAttributes.PROJECT, projectMBr.getText());
        config.setAttribute(ICoverAttributes.MODULE, moduleBr.getText());
        config.setAttribute(ICoverAttributes.FILE, fileBr.getText());
        config.setAttribute(ICoverAttributes.APPLICATION, appBr.getText());
        config.setAttribute(ICoverAttributes.TYPE, type.toString());
        config.setAttribute(ICoverAttributes.COMBO, testCombo.getText());
    }

    public String getName() {
        return "Cover";
    }

    private void createModuleGroup(final Composite comp) {

        final ElementListSelectionDialog projectDialog = new ElementListSelectionDialog(
                getShell(), new ProjectLabelProvider());

        final Object[] elements = createProjectArray();

        projectDialog.setElements(elements);
        projectDialog.setTitle("Select project");
        projectDialog.setMessage("Select Erlang project: ");

        moduleDialog = new ElementListSelectionDialog(getShell(),
                new ProjectLabelProvider());

        moduleDialog.setElements(new Object[0]);
        moduleDialog.setTitle("Select module");
        moduleDialog.setMessage("Select Erlang module: ");

        projectMBr = browserWithLabel(comp, "Project:", projectDialog);
        projectMBr.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                updateLaunchConfigurationDialog();
                final String projectName = projectMBr.getText();
                if (projectName != null && projectName.length() > 0) {
                    final IErlProject p = ErlangCore.getModel()
                            .getErlangProject(projectName);
                    if (p != null) {
                        moduleDialog.setElements(createModuleArray(p));
                    }
                }

            }

        });

        moduleBr = browserWithLabel(comp, "Module:", moduleDialog);
        moduleBr.addModifyListener(basicModifyListener);

    }

    private Object[] createProjectArray() {
        Object[] array;
        try {
            final List<ProjectElement> res = new LinkedList<ProjectElement>();

            final Collection<IErlProject> projects = ErlangCore.getModel()
                    .getErlangProjects();

            for (final IErlProject p : projects) {
                final ProjectElement elem = new ProjectElement(p.getName(),
                        PlatformUI
                                .getWorkbench()
                                .getSharedImages()
                                .getImageDescriptor(
                                        IDE.SharedImages.IMG_OBJ_PROJECT)
                                .createImage());

                res.add(elem);
            }
            array = res.toArray();

        } catch (final ErlModelException e) {
            array = new Object[0];
            e.printStackTrace();
        }
        return array;
    }

    private Object[] createModuleArray(final IErlProject p) {
        Object[] array;
        try {
            final List<ProjectElement> res = new LinkedList<ProjectElement>();

            final Collection<IErlModule> modules = p.getModules();

            for (final IErlModule m : modules) {
                final ProjectElement elem = new ProjectElement(m.getName(),
                        ErlangElementImageProvider.getErlImageDescriptor(m,
                                ErlangElementImageProvider.SMALL_ICONS)
                                .createImage());
                res.add(elem);
            }
            array = res.toArray();

        } catch (final ErlModelException e) {
            array = new Object[0];
            e.printStackTrace();
        }
        return array;
    }

    private void createAllTestsGroup(final Composite comp) {

        /*
         * For later use:
         * 
         * ElementTreeSelectionDialog fileDialog = new
         * ElementTreeSelectionDialog(
         * 
         * this.getShell(), new WorkbenchLabelProvider(), new
         * BaseWorkbenchContentProvider());
         * fileDialog.setTitle("Select file ore directory");
         * fileDialog.setMessage("Select project, directory or file: ");
         * fileDialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
         * fileDialog.setAllowMultiple(false);
         * 
         * 
         * fileDialog.addFilter(new ViewerFilter() {
         * 
         * @Override public boolean select(Viewer viewer, Object parentElement,
         * Object element) {
         * 
         * IWorkspaceRoot workspaceRoot =
         * ResourcesPlugin.getWorkspace().getRoot();
         * 
         * if(parentElement.equals(workspaceRoot) && element instanceof
         * IProject){ String name = ((IProject)element).getName();
         * 
         * try { if(!ErlangCore.getModel().getErlangProject(name).
         * getModules().isEmpty()) { return true; } } catch (ErlModelException
         * e) { e.printStackTrace(); } return false; }
         * 
         * return true; }
         * 
         * });
         */

        final ElementListSelectionDialog projectDialog = new ElementListSelectionDialog(
                getShell(), new ProjectLabelProvider());

        final Object[] elements = createProjectArray();

        projectDialog.setElements(elements);
        projectDialog.setTitle("Select project");
        projectDialog.setMessage("Select Erlang project: ");

        fileBr = new ItemBrowser(comp, SWT.SINGLE | SWT.BORDER, projectDialog);
        fileBr.setFiledLength(600);
        fileBr.getTextGridData().horizontalSpan = 2;
        fileBr.addModifyListener(basicModifyListener);
    }

    private void createApplicationGroup(final Composite comp) {

        final ElementListSelectionDialog projectDialog = new ElementListSelectionDialog(
                getShell(), new ProjectLabelProvider());

        final Object[] elements = createProjectArray();

        projectDialog.setElements(elements);
        projectDialog.setTitle("Select project");
        projectDialog.setMessage("Select Erlang project: ");

        final ElementListSelectionDialog appDialog = new ElementListSelectionDialog(
                getShell(), new LabelProvider());

        // TODO: create model for appilcations
        appDialog.setElements(new Object[0]);
        appDialog.setTitle("Select application");
        appDialog.setMessage("Select Erlang application: ");

        projectAppBr = browserWithLabel(comp, "Project:", projectDialog);
        projectAppBr.addModifyListener(basicModifyListener);

        appBr = browserWithLabel(comp, "Module:", appDialog);
        appBr.addModifyListener(basicModifyListener);

    }

    private ItemBrowser browserWithLabel(final Composite comp,
            final String text, final SelectionDialog dialog) {

        final GridData gData = new GridData();

        final Label label = new Label(comp, SWT.NONE);
        label.setLayoutData(gData);
        label.setText(text);

        final ItemBrowser browser = new ItemBrowser(comp, SWT.SINGLE
                | SWT.BORDER, dialog);

        return browser;
    }

    private final ModifyListener basicModifyListener = new ModifyListener() {
        @SuppressWarnings("synthetic-access")
        public void modifyText(final ModifyEvent evt) {
            updateLaunchConfigurationDialog();
        }
    };

    private final SelectionListener radioSelectionListener = new SelectionListener() {

        public void widgetSelected(final SelectionEvent e) {

            updateLaunchConfigurationDialog();

            if (e.widget.equals(singleRadio)) {
                projectMBr.setEnabled(true);
                moduleBr.setEnabled(true);
                fileBr.setEnabled(false);
                projectAppBr.setEnabled(false);
                appBr.setEnabled(false);
            } else if (e.widget.equals(allRadio)) {
                projectMBr.setEnabled(false);
                moduleBr.setEnabled(false);
                fileBr.setEnabled(true);
                projectAppBr.setEnabled(false);
                appBr.setEnabled(false);
            } else if (e.widget.equals(appRadio)) {
                projectMBr.setEnabled(false);
                moduleBr.setEnabled(false);
                fileBr.setEnabled(false);
                projectAppBr.setEnabled(true);
                appBr.setEnabled(true);
            }
        }

        public void widgetDefaultSelected(final SelectionEvent e) {
            // TODO Auto-generated method stub

        }

    };

}