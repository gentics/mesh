package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.console.ConsoleProvider;
import com.gentics.mesh.core.console.FakeConsoleProviderImpl;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class FakeConsoleModule {

	@Binds
	abstract ConsoleProvider fakeConsole(FakeConsoleProviderImpl e);

}
