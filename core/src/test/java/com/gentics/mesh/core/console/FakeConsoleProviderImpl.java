package com.gentics.mesh.core.console;

import java.io.IOException;

import javax.inject.Inject;

import com.gentics.mesh.util.UUIDUtil;

import groovy.lang.Singleton;

/**
 * Fake console provider which will not block tests.
 */
@Singleton
public class FakeConsoleProviderImpl implements ConsoleProvider {

	@Inject
	public FakeConsoleProviderImpl() {
	}

	@Override
	public int read() throws IOException {
		return 0;
	}

	@Override
	public String readPassword() {
		return UUIDUtil.randomUUID();
	}

}
