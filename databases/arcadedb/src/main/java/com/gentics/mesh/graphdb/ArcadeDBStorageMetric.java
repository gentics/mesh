package com.gentics.mesh.graphdb;

import com.gentics.mesh.metric.Metric;

/**
 * ArcadeDB storage metrics
 */
public enum ArcadeDBStorageMetric implements Metric {
	/**
	 * Total disk space
	 */
	DISK_TOTAL("storage_disk_total", "Total disk space for ArcadeDB storage."),

	/**
	 * Usable disk space
	 */
	DISK_USABLE("storage_disk_usable", "Usable disk space for ArcadeDB storage.");

	private String key;

	private String description;

	/**
	 * Create instance
	 * @param key metric key
	 * @param description metric description
	 */
	private ArcadeDBStorageMetric(String key, String description) {
		this.key = key;
		this.description = description;
	}

	@Override
	public String key() {
		return "mesh_" + key;
	}

	@Override
	public String description() {
		return description;
	}
}
