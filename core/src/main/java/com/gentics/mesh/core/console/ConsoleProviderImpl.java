package com.gentics.mesh.core.console;

import java.io.Console;
import java.io.IOException;
import java.util.Scanner;

import javax.inject.Inject;

import groovy.lang.Singleton;

@Singleton
public class ConsoleProviderImpl implements ConsoleProvider {

	@Inject
	public ConsoleProviderImpl() {
	}

	@Override
	public int read() throws IOException {
		return System.in.read();
	}

	@Override
	public String readPassword() {
		Console console = System.console();
		// Console may be null in IDE
		if (console != null) {
			return new String(console.readPassword("Enter initial admin password: "));
		} else {
			Scanner scanIn = new Scanner(System.in);
			try {
				return scanIn.nextLine();
			} finally {
				scanIn.close();
			}
		}
	}
}
