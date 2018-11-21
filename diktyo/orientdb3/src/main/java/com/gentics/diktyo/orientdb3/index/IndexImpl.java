package com.gentics.diktyo.orientdb3.index;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.diktyo.index.AbstractIndex;

@Singleton
public class IndexImpl extends AbstractIndex {

	private String name;

	@Inject
	public IndexImpl() {
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void refresh() {
		throw new NotImplementedException();
	}

	@Override
	public void remove() {
		throw new NotImplementedException();
	}
}
