package com.gentics.mesh.dagger;

import java.util.Random;

import javax.inject.Singleton;

import com.gentics.mesh.hibernate.util.UuidGenerator;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger 2 module for providing various utility bindings.
 * 
 * @author plyhun
 *
 */
@Module
public class UtilModule {
	@Provides
	@Singleton
	public UuidGenerator uuidGenerator() {
		return new UuidGenerator(new Random());
	}
}
