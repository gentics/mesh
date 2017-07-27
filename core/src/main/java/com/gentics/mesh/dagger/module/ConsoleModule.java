package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.console.ConsoleProvider;
import com.gentics.mesh.core.console.ConsoleProviderImpl;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class ConsoleModule {

	@Binds
	abstract ConsoleProvider bindConsole(ConsoleProviderImpl e);
}
