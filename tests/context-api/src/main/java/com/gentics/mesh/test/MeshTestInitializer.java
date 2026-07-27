package com.gentics.mesh.test;

/**
 * Interface for test initializers. The method {@link #init()} will be called before anything else happens
 */
public interface MeshTestInitializer {
	/**
	 * Do some test initialization
	 */
	void init();

	/**
	 * No-op implementation of {@link MeshTestInitializer}
	 */
	static final class NoOptionInitializer implements MeshTestInitializer {
		@Override
		public void init() {
		}
	}
}
