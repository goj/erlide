/*******************************************************************************
 * Copyright (c) 2004 Eric Merritt and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eric Merritt
 *******************************************************************************/
package org.erlide.core.util;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.erlide.core.ErlangPlugin;
import org.erlide.runtime.ErlangProjectProperties;
import org.erlide.runtime.PreferencesUtils;

/**
 * Simple utility functions
 * 
 * 
 * @author Eric Merritt [cyberlync at yahoo dot com] Vlad Jakob C
 */
public class PluginUtils {

	/**
	 * Displays an error that occured during the project creation. *
	 * 
	 * @param x
	 *            details on the error
	 * @return IStatus
	 */
	public static IStatus makeStatus(final Exception x) {
		return new Status(IStatus.ERROR, ErlangPlugin.PLUGIN_ID, 0, x
				.getMessage(), x);
	}

	/**
	 * Checks to see if the specified container is on the source path
	 * 
	 * @param con
	 *            the container
	 * @return the indicator of the source path
	 */
	public static boolean isOnSourcePath(final IContainer con) {
		final IProject project = con.getProject();
		/*
		 * Get the project settings so that we can find the source nodes
		 */
		final ErlangProjectProperties prefs = new ErlangProjectProperties(
				project);
		final List<String> sourcePaths = PreferencesUtils.unpackList(prefs
				.getSourceDirsString());
		final IPath path = con.getFullPath();
		for (final String i : sourcePaths) {
			if (project.getFolder(i).getFullPath().equals(path)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSourcePathParent(IFolder con) {
		final IProject project = con.getProject();
		/*
		 * Get the project settings so that we can find the source nodes
		 */
		final ErlangProjectProperties prefs = new ErlangProjectProperties(
				project);
		final List<String> sourcePaths = PreferencesUtils.unpackList(prefs
				.getSourceDirsString());
		final IPath path = con.getFullPath();
		for (final String i : sourcePaths) {
			if (path.isPrefixOf(project.getFolder(i).getFullPath())) {
				return true;
			}
		}
		return false;
	}
}
