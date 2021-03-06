package org.erlide.core.search;

import erlang.ErlangSearchPattern;

public abstract class NamePattern extends ErlangSearchPattern {

	private final String name;

	protected NamePattern(final String name, final int limitTo) {
		super(limitTo);
		this.name = name;
	}

	@Override
	public String patternString() {
		return getName();
	}

	public String getName() {
		return name;
	}

}
