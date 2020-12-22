package com.gentics.mesh.metric;

/**
 * Marker interface for metrics.
 */
public interface Metric {

	/**
	 * Key of the metric.
	 * 
	 * @return
	 */
	String key();

	/**
	 * Description of the metric.
	 * 
	 * @return
	 */
	String description();
}
