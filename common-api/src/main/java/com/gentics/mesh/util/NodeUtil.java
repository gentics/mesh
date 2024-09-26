package com.gentics.mesh.util;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Various utility functions regarding nodes.
 */
public class NodeUtil {
	private static final Set<String> processableImageTypes = ImmutableSet.of(
		"image/gif", "image/png", "image/jpeg", "image/bmp", "image/webp",
		// Not an actual mime type, but with this,
		// Mesh will behave as intended by the user even if he made a mistake.
		"image/jpg"
	);

	/**
	 * Tests if a binary is a readable image by checking its mime type.
	 * The formats ImageIO can read are bmp, jpg, png and gif.
	 */
	public static boolean isProcessableImage(String mimeType) {
		return processableImageTypes.contains(mimeType);
	}
}
