package com.gentics.mesh.monitor.jmx;

/**
 * Interface for the Liveness mbean
 */
public interface LivenessMBean {
	/**
	 * Name of the Liveness mbean
	 */
	public static String NAME = "com.gentics.mesh.mbeans:type=Probes,name=Liveness";

	/**
	 * Check whether Mesh is "live"
	 * @return true if Mesh is live
	 */
	boolean isLive();
}
