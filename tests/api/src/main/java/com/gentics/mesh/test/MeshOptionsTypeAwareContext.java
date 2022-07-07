package com.gentics.mesh.test;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.env.MeshOptionsContext;

/**
 * This interface may be used by the implementor unit test classes, 
 * that need an implementation of {@link MeshOptions} with keeping an actual implementation type,
 * e.g. for context depentent tests.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public interface MeshOptionsTypeAwareContext<T extends MeshOptions> extends MeshOptionsContext<T> {

	/**
	 * Get the options. By default the options are taken from the provider, set in {@link MeshOptionsProvider#ENV_OPTIONS_PROVIDER_CLASS} system property.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	default T getOptions() {
		try {
			return (T) MeshOptionsProvider.getProvider().getOptions();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
