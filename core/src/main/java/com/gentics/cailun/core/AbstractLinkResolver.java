package com.gentics.cailun.core;

import java.util.concurrent.Callable;

abstract class AbstractLinkResolver implements Callable<String> {

	private String text;

	public AbstractLinkResolver(String text) {
		this.text = text;
	}

	public void set(String text) {
		this.text = text;
	}

	public String get() {
		return text;
	}

	public boolean isSet() {
		return text != null;
	}

	@Override
	public String toString() {
		return text;
	}

	public abstract String call() throws Exception;

}
