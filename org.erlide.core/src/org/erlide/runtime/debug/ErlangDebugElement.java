/*******************************************************************************
 * Copyright (c) 2004 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution.
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.runtime.debug;

import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;

public class ErlangDebugElement extends DebugElement {

	public ErlangDebugElement(final IDebugTarget target) {
		super(target);
	}

	public String getModelIdentifier() {
		return IErlDebugConstants.ID_ERLANG_DEBUG_MODEL;
	}

	// @SuppressWarnings("unchecked")
	// @Override
	// public Object getAdapter(Class adapter) {
	// if (adapter == IDebugElement.class) {
	// return this;
	// }
	//
	// return super.getAdapter(adapter);
	// }

}