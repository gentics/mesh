package com.gentics.mesh.search.impl;

import javax.inject.Singleton;

import com.gentics.mesh.search.SearchProvider;

import dagger.Module;
import dagger.Provides;

@Singleton
@Module
public class DummySearchProviderModule {

	@Provides
	public SearchProvider searchProvider() {
		return new DummySearchProvider();
	}

}
