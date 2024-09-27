package com.gentics.mesh.parameter;

import org.apache.commons.lang.BooleanUtils;

/**
 * Cache clear query parameters
 */
public interface CacheClearParameters extends ParameterProvider {

	public static final String CLEAR_IMAGE_CACHE_KEY = "imageCache";

	/**
	 * Set clear image cache flag.
	 * 
	 * @param flag
	 * @return
	 */
	default CacheClearParameters setClearImageCache(boolean flag) {
		setParameter(CLEAR_IMAGE_CACHE_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check if image cache clear requested.
	 * 
	 * @return
	 */
	default boolean isClearImageCache() {
		return BooleanUtils.toBooleanDefaultIfNull(Boolean.valueOf(getParameter(CLEAR_IMAGE_CACHE_KEY)), false);
	}
}
