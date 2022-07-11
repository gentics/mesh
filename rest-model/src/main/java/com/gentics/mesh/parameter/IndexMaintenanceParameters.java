package com.gentics.mesh.parameter;

/**
 * Interface for index maintenance parameters
 */
public interface IndexMaintenanceParameters extends ParameterProvider {
	/**
	 * Key of the parameter to restrict the indices (via regex)
	 */
	public static final String INDEX_PARAMETER_KEY = "index";

	/**
	 * Regex to restrict the index maintenance action to specific indices (via regex)
	 * @return index parameter
	 */
	default String getIndex() {
		return getParameter(INDEX_PARAMETER_KEY);
	}

	/**
	 * Set the index parameter
	 * @param index parameter
	 * @return fluent API
	 */
	default IndexMaintenanceParameters setIndex(String index) {
		setParameter(INDEX_PARAMETER_KEY, index);
		return this;
	}
}
