package com.gentics.mesh.core.console;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConsoleProviderImpl implements ConsoleProvider {

	@Inject
	public ConsoleProviderImpl() {
	}
	
	@Override
	public int read() throws IOException {
		return System.in.read();
	}
}
