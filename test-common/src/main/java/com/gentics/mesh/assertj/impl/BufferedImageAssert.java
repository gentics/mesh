package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.assertj.core.api.AbstractAssert;

public class BufferedImageAssert extends AbstractAssert<BufferedImageAssert, BufferedImage> {

	public BufferedImageAssert(BufferedImage actual) {
		super(actual, BufferedImageAssert.class);
	}

	/**
	 * Validates the image width and height.
	 * 
	 * @param width
	 * @param height
	 * @return Fluent API
	 */
	public BufferedImageAssert hasSize(int width, int height) {
		assertEquals("Image height did not match", width, actual.getWidth());
		assertEquals("Image width did not match", height, actual.getHeight());
		return this;
	}

	/**
	 * Validate the pixels of the actual image with the reference image of the given name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public BufferedImageAssert matchesReference(String name) {
		try {
			BufferedImage refImage = ImageIO.read(new File("src/test/resources/references/" + name));
			assertNotNull("Could not find reference image", refImage);
			matches(refImage);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not load reference image");
		}
		return this;
	}

	/**
	 * Validate the pixels of the actual image with the given reference image. The pixel color will be reduced to a 8bit value.
	 * 
	 * @param refImage
	 * @return Fluent API
	 */
	public BufferedImageAssert matches(BufferedImage refImage) {
		assertEquals("Image height did not match", refImage.getWidth(), actual.getWidth());
		assertEquals("Image width did not match", refImage.getHeight(), actual.getHeight());
		for (int x = 0; x < refImage.getWidth(); x++) {
			for (int y = 0; y < refImage.getHeight(); y++) {
				// Get 8bit pixel color
				int pixel = actual.getRGB(x, y);
				int pixelRef = refImage.getRGB(x, y);
				assertEquals(getWritableAssertionInfo().description() + ": The 8bit pixel value of {" + x + "/" + y
					+ "} did not match with the reference image", pixelRef, pixel);
			}
		}
		return this;
	}

}
