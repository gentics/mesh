package com.gentics.mesh.etc.config;

/**
 * Image manipulation mode.
 * 
 * @author plyhun
 *
 */
public enum ImageManipulationMode {

	/**
	 * The image is transformed on read demand. The result is cached, if the image cache is set up.
	 */
	ON_DEMAND(1),

	/**
	 * The image is transformed on an explicit transformation demand, storing the result in the binary storage. The image cache is not used.
	 */
	MANUAL(2),

	/**
	 * The image is transformed on an explicit transformation demand, storing the result in the binary storage. The image cache is used to pick the already transformed variant from.
	 */
	MANUAL_HYBRID(3),

	/**
	 * No image manipulation allowed.
	 */
	OFF(0);

	private final int mode;

	private ImageManipulationMode(int mode) {
		this.mode = mode;
	}

	public int getMode() {
		return mode;
	}
}
