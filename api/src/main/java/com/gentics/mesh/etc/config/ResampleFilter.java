package com.gentics.mesh.etc.config;

import java.awt.*;

public enum ResampleFilter {
	/**
	 * Undefined interpolation, filter method will use default filter.
	 */
	UNDEFINED(0),
	/**
	 * Point interpolation (also known as "nearest neighbour").
	 * Very fast, but low quality
	 * (similar to {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR}
	 * and {@link Image#SCALE_REPLICATE}).
	 */
	POINT(1),
	/**
	 * Box interpolation. Fast, but low quality.
	 */
	BOX(2),
	/**
	 * Triangle interpolation (also known as "linear" or "bilinear").
	 * Quite fast, with acceptable quality
	 * (similar to {@link RenderingHints#VALUE_INTERPOLATION_BILINEAR} and
	 * {@link Image#SCALE_AREA_AVERAGING}).
	 */
	TRIANGLE(3),
	/**
	 * Hermite interpolation.
	 */
	HERMITE(4),
	/**
	 * Hanning interpolation.
	 */
	HANNING(5),
	/**
	 * Hamming interpolation.
	 */
	HAMMING(6),
	/**
	 * Blackman interpolation..
	 */
	BLACKMAN(7),
	/**
	 * Gaussian interpolation.
	 */
	GAUSSIAN(8),
	/**
	 * Quadratic interpolation.
	 */
	QUADRATIC(9),
	/**
	 * Cubic interpolation.
	 */
	CUBIC(10),
	/**
	 * Catrom interpolation.
	 */
	CATROM(11),
	/**
	 * Mitchell interpolation. High quality.
	 */
	MITCHELL(12),
	/**
	 * Lanczos interpolation. High quality.
	 */
	LANCZOS(13), // IM default
	/**
	 * Blackman-Bessel interpolation. High quality.
	 */
	BLACKMAN_BESSEL(14),
	/**
	 * Blackman-Sinc interpolation. High quality.
	 */
	BLACKMAN_SINC(15);


	private final int filter;

	ResampleFilter(int filter) {
		this.filter = filter;
	}

	public int getFilter() {
		return filter;
	}
}
