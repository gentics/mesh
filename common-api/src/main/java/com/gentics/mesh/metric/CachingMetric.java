package com.gentics.mesh.metric;

/**
 * Metric which keeps track of caching stats. 
 */
public class CachingMetric implements Metric {

	private final Event event;
	private final String name;

	public CachingMetric(Event event, String name) {
		this.event = event;
		this.name = name;
	}

	@Override
	public String key() {
		return "mesh_cache_" + name + "_" + event.name().toLowerCase();
	}

	@Override
	public String description() {
		return null;
	}

	/**
	 * Metric event types.
	 */
	public enum Event {
		HIT,
		MISS,
		CLEAR_SINGLE,
		CLEAR_ALL,
	}
}
