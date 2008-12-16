/*******************************************************************************
 * Copyright (c) 2005 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *     Jakob C
 *******************************************************************************/
package org.erlide.ui.launch;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlFolder;
import org.erlide.core.erlang.IErlModel;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.erlang.IOpenable;
import org.erlide.core.erlang.IParent;
import org.erlide.core.util.ErlideUtil;
import org.erlide.runtime.ErlLogger;
import org.erlide.runtime.backend.ErlangLaunchConfigurationDelegate;
import org.erlide.runtime.backend.IErlLaunchAttributes;
import org.erlide.runtime.debug.ErlDebugConstants;
import org.erlide.ui.util.SWTUtil;

/**
 * A tab in the Launch Config with erlang debugger parameters: the debug flags
 * for attaching and distruibuted debugging a checkbox tree of modules to
 * interpret upon launching. The checkbox tree classes are reused by
 * InterpretedModulesView
 * 
 */
public class DebugTab extends AbstractLaunchConfigurationTab {

	private CheckboxTreeViewer checkboxTreeViewer;
	private Button attachOnFirstCallCheck;
	private Button attachOnBreakpointCheck;
	private Button attachOnExitCheck;
	private Button distributedDebugCheck;
	private List<IErlModule> interpretedModules;

	public static class TreeLabelProvider extends LabelProvider {
		public TreeLabelProvider() {
			super();
		}

		@Override
		public String getText(final Object element) {
			if (element instanceof DebugTreeItem) {
				IErlElement item = ((DebugTreeItem) element).item;
				if (item == null) {
					ErlLogger.warn("Null item in DebugTreeItem %s", element
							.toString());
					return "---";
				}
				return item.getName();
			}
			return super.getText(element);
		}

		@Override
		public Image getImage(final Object element) {
			return null;
		}
	}

	public static class DebugTreeItem {
		final IErlElement item;
		final WeakReference<DebugTreeItem> parent;
		final List<DebugTreeItem> children = new ArrayList<DebugTreeItem>();

		public DebugTreeItem(final IErlElement item, final DebugTreeItem parent) {
			this.item = item;
			this.parent = new WeakReference<DebugTreeItem>(parent);
		}

		public DebugTreeItem getParent() {
			return parent.get();
		}

		public IErlElement getItem() {
			return item;
		}

		public List<DebugTreeItem> getChildren() {
			return children;
		}

		public boolean areChildrenChecked(final CheckboxTreeViewer tree) {
			for (final DebugTreeItem i : children) {
				if (!tree.getChecked(i) || tree.getGrayed(i)) {
					return false;
				}
			}
			return true;
		}

		public boolean areChildrenUnchecked(final CheckboxTreeViewer tree) {
			for (final DebugTreeItem i : children) {
				if (tree.getChecked(i) || tree.getGrayed(i)) {
					return false;
				}
			}
			return true;
		}

		boolean addAllErlangModules(final IErlElement elem) {
			if (elem instanceof IErlModule) {
				children.add(new DebugTreeItem(elem, this));
				return true;
			} else if (elem instanceof IParent) {
				try {
					if (elem instanceof IErlFolder) {
						final IErlFolder f = (IErlFolder) elem;
						if (!f.isSourcePathParent()) {
							return false;
						}
					}
					if (elem instanceof IOpenable) {
						final IOpenable o = (IOpenable) elem;
						o.open(null);
					}
					final DebugTreeItem dti = new DebugTreeItem(elem, this);
					final IParent p = (IParent) elem;
					boolean addedAny = false;
					for (final IErlElement i : p.getChildren()) {
						addedAny |= dti.addAllErlangModules(i);
					}
					if (addedAny) {
						children.add(dti);
					}
					return true;
				} catch (final ErlModelException e) {
					ErlLogger.warn(e);
				}
			}
			return false;
		}

		private void setGrayChecked(
				final CheckboxTreeViewer checkboxTreeViewer,
				final boolean grayed, final boolean checked) {
			checkboxTreeViewer.setGrayed(this, grayed);
			checkboxTreeViewer.setChecked(this, checked);
		}

		private void updateMenuCategoryCheckedState(
				final CheckboxTreeViewer checkboxTreeViewer) {
			if (areChildrenUnchecked(checkboxTreeViewer)) {
				setGrayChecked(checkboxTreeViewer, false, false);
			} else if (areChildrenChecked(checkboxTreeViewer)) {
				setGrayChecked(checkboxTreeViewer, false, true);
			} else {
				setGrayChecked(checkboxTreeViewer, true, true);
			}
			if (getParent() != null) {
				getParent().updateMenuCategoryCheckedState(checkboxTreeViewer);
			}
		}

		public void setChecked(final CheckboxTreeViewer checkboxTreeViewer,
				final Collection<IErlModule> list) {
			setGrayChecked(checkboxTreeViewer, false, list.contains(item));
			for (final DebugTreeItem c : children) {
				c.setChecked(checkboxTreeViewer, list);
				c.getParent()
						.updateMenuCategoryCheckedState(checkboxTreeViewer);
			}
		}
	}

	public static class TreeContentProvider implements
			IStructuredContentProvider, ITreeContentProvider {
		private DebugTreeItem root;

		public TreeContentProvider() {
			super();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
			try {
				setRoot(new DebugTreeItem(null, null));
				if (newInput instanceof ILaunchConfiguration) {
					final ILaunchConfiguration input = (ILaunchConfiguration) newInput;
					final String projs = input.getAttribute(
							IErlLaunchAttributes.PROJECTS, "").trim();
					if (projs.length() == 0) {
						return;
					}
					final String[] projNames = projs.split(";");
					if (projNames == null) {
						return;
					}
					final IErlModel model = ErlangCore.getModel();
					for (final String projName : projNames) {
						final IErlElement prj = model
								.getErlangProject(projName);
						final DebugTreeItem dti = new DebugTreeItem(prj,
								getRoot());
						dti.addAllErlangModules(prj);
						getRoot().children.add(dti);
					}
				}
			} catch (final CoreException e1) {
			}
		}

		public void dispose() {
		}

		public Object[] getElements(final Object inputElement) {
			return getChildren(getRoot());
		}

		public Object[] getChildren(final Object parentElement) {
			final DebugTreeItem dti = (DebugTreeItem) parentElement;
			return dti.children.toArray();
		}

		public Object getParent(final Object element) {
			final DebugTreeItem dti = (DebugTreeItem) element;
			return dti.getParent();
		}

		public boolean hasChildren(final Object element) {
			return getChildren(element).length > 0;
		}

		public DebugTreeItem getRoot() {
			return root;
		}

		public void setRoot(final DebugTreeItem root) {
			this.root = root;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse
	 * .swt.widgets.Composite)
	 */
	public void createControl(final Composite parent) {
		interpretedModules = new ArrayList<IErlModule>();

		final Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		final GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);

		distributedDebugCheck = createCheckButton(comp,
				"Debug all connected nodes");

		final Group attachGroup = SWTUtil.createGroup(comp, "Auto Attach", 1,
				GridData.FILL_BOTH);
		attachGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false));
		attachOnFirstCallCheck = createCheckButton(attachGroup, "First &call");
		attachOnBreakpointCheck = createCheckButton(attachGroup, "&Breakpoint");
		attachOnExitCheck = createCheckButton(attachGroup, "E&xit");

		distributedDebugCheck.addSelectionListener(fBasicSelectionListener);
		attachOnFirstCallCheck.addSelectionListener(fBasicSelectionListener);
		attachOnBreakpointCheck.addSelectionListener(fBasicSelectionListener);
		attachOnExitCheck.addSelectionListener(fBasicSelectionListener);

		final Group interpretedModulesGroup = new Group(comp, SWT.NONE);
		interpretedModulesGroup.setText("Interpreted modules");
		final GridData gd_interpretedModulesGroup = new GridData();
		interpretedModulesGroup.setLayoutData(gd_interpretedModulesGroup);
		interpretedModulesGroup.setLayout(new GridLayout());

		final Label anyModuleHavingLabel = new Label(interpretedModulesGroup,
				SWT.WRAP);
		anyModuleHavingLabel.setLayoutData(new GridData(279, SWT.DEFAULT));
		anyModuleHavingLabel
				.setText("Any module having breakpoints enabled will be dynamically added to the list.");

		checkboxTreeViewer = new CheckboxTreeViewer(interpretedModulesGroup,
				SWT.BORDER);
		checkboxTreeViewer.addCheckStateListener(new ICheckStateListener() {
			@SuppressWarnings("synthetic-access")
			public void checkStateChanged(final CheckStateChangedEvent event) {
				final DebugTreeItem item = (DebugTreeItem) event.getElement();
				final boolean checked = event.getChecked();
				updateOnCheck(item, checked);

				checkboxTreeViewer.setSubtreeChecked(item, checked);
				// set gray state of the element's category subtree, all items
				// should not be grayed
				for (final DebugTreeItem i : item.children) {
					checkboxTreeViewer.setGrayed(i, false);
				}
				checkboxTreeViewer.setGrayed(item, false);
				if (item.getParent() != null) {
					item.getParent().updateMenuCategoryCheckedState(
							checkboxTreeViewer);
				}

				updateLaunchConfigurationDialog();
			}

		});
		checkboxTreeViewer.setLabelProvider(new TreeLabelProvider());
		checkboxTreeViewer.setContentProvider(new TreeContentProvider());
		final Tree tree = checkboxTreeViewer.getTree();
		final GridData gd_tree = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd_tree.minimumWidth = 250;
		gd_tree.minimumHeight = 120;
		gd_tree.widthHint = 256;
		gd_tree.heightHint = 220;
		tree.setLayoutData(gd_tree);
	}

	/**
	 * Recursively update checkboxes
	 * 
	 * @param item
	 *            item to check or uncheck
	 * @param checked
	 *            true if checked
	 */
	protected void updateOnCheck(final DebugTreeItem item, final boolean checked) {
		if (item.item instanceof IErlModule) {
			final IErlModule m = (IErlModule) item.item;
			if (checked) {
				if (!interpretedModules.contains(m)) {
					interpretedModules.add(m);
				}
			} else {
				interpretedModules.remove(m);
			}
		} else {
			for (final DebugTreeItem i : item.children) {
				updateOnCheck(i, checked);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.
	 * debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@SuppressWarnings("unchecked")
	public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
		List<String> interpret;
		String prjs;
		try {
			interpret = config.getAttribute(
					IErlLaunchAttributes.DEBUG_INTERPRET_MODULES,
					new ArrayList<String>());
			prjs = config.getAttribute(IErlLaunchAttributes.PROJECTS, "")
					.trim();
		} catch (final CoreException e1) {
			interpret = new ArrayList<String>();
			prjs = "";
		}
		final String[] projectNames = prjs.length() == 0 ? new String[] {}
				: prjs.split(";");
		final Set<IProject> projects = new HashSet<IProject>();
		for (final String s : projectNames) {
			final IProject project = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(s);
			if (project == null) {
				continue;
			}
			projects.add(project);
		}

		interpret = ErlangLaunchConfigurationDelegate
				.addBreakpointProjectsAndModules(projects, interpret);
		interpretedModules = new ArrayList<IErlModule>();

		addModules(interpret, interpretedModules);

		int debugFlags;
		try {
			debugFlags = config.getAttribute(IErlLaunchAttributes.DEBUG_FLAGS,
					ErlDebugConstants.DEFAULT_DEBUG_FLAGS);
		} catch (final CoreException e) {
			debugFlags = ErlDebugConstants.DEFAULT_DEBUG_FLAGS;
		}
		setFlagCheckboxes(debugFlags);

		if (checkboxTreeViewer != null) {
			checkboxTreeViewer.setInput(config);
			checkboxTreeViewer.expandAll();
			final DebugTreeItem root = ((TreeContentProvider) checkboxTreeViewer
					.getContentProvider()).getRoot();
			root.setChecked(checkboxTreeViewer, interpretedModules);
		}
	}

	/**
	 * Find modules from string list add to IFile-list
	 * 
	 * @param interpret
	 *            the list of strings from prefs (projectName:fileName;... or
	 *            moduleName;...)
	 * @param interpretedModules
	 *            collection that the IFile-s are added to
	 */
	public static void addModules(final Collection<String> interpret,
			final Collection<IErlModule> interpretedModules) {
		final IErlModel model = ErlangCore.getModel();
		for (final String i : interpret) {
			final String[] pm = i.split(":");
			IErlModule m = null;
			if (pm.length > 1) {
				final IErlProject p = model.getErlangProject(pm[0]);
				final String mName = pm[1];
				try {
					final String s = ErlideUtil.hasModuleExtension(mName) ? mName
							: mName + ".erl";
					m = p.getModule(s);
				} catch (final ErlModelException e) {
					ErlLogger.warn(e);
				}
			} else {
				m = model.getModule(i + ".erl");
			}
			if (m != null) {
				if (!interpretedModules.contains(m)) {
					interpretedModules.add(m);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse
	 * .debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(final ILaunchConfiguration config) {
		try {
			setDefaults(config.getWorkingCopy());
		} catch (final CoreException e) {
			ErlLogger.warn(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse
	 * .debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(final ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IErlLaunchAttributes.DEBUG_FLAGS,
				getFlagChechboxes());
		final List<String> r = new ArrayList<String>();
		for (final IErlModule m : interpretedModules) {
			r.add(m.getProject().getName() + ":"
					+ ErlideUtil.withoutExtension(m.getName()));
		}
		config.setAttribute(IErlLaunchAttributes.DEBUG_INTERPRET_MODULES, r);
	}

	public String getName() {
		return "Debug";
	}

	@Override
	public boolean isValid(final ILaunchConfiguration config) {
		return true;
	}

	/**
	 * check or uncheck the four flag checkboxes
	 * 
	 * @param debugFlags
	 *            flags
	 */
	private void setFlagCheckboxes(final int debugFlags) {
		if (attachOnFirstCallCheck == null) {
			// I don't know why these are null sometimes...
			return;
		}
		int flag = debugFlags & ErlDebugConstants.ATTACH_ON_FIRST_CALL;
		attachOnFirstCallCheck.setSelection(flag != 0);
		flag = debugFlags & ErlDebugConstants.ATTACH_ON_BREAKPOINT;
		attachOnBreakpointCheck.setSelection(flag != 0);
		flag = debugFlags & ErlDebugConstants.ATTACH_ON_EXIT;
		attachOnExitCheck.setSelection(flag != 0);
		flag = debugFlags & ErlDebugConstants.DISTRIBUTED_DEBUG;
		distributedDebugCheck.setSelection(flag != 0);
	}

	/**
	 * get flag settings by reading checkboxes
	 * 
	 * @return flags as int
	 */
	private int getFlagChechboxes() {
		int result = 0;
		if (attachOnFirstCallCheck.getSelection()) {
			result |= ErlDebugConstants.ATTACH_ON_FIRST_CALL;
		}
		if (attachOnBreakpointCheck.getSelection()) {
			result |= ErlDebugConstants.ATTACH_ON_BREAKPOINT;
		}
		if (attachOnExitCheck.getSelection()) {
			result |= ErlDebugConstants.ATTACH_ON_EXIT;
		}
		if (distributedDebugCheck.getSelection()) {
			result |= ErlDebugConstants.DISTRIBUTED_DEBUG;
		}
		return result;
	}

	private final SelectionListener fBasicSelectionListener = new SelectionListener() {
		@SuppressWarnings("synthetic-access")
		public void widgetDefaultSelected(SelectionEvent e) {
			updateLaunchConfigurationDialog();
		}

		@SuppressWarnings("synthetic-access")
		public void widgetSelected(SelectionEvent e) {
			updateLaunchConfigurationDialog();
		}
	};

}
