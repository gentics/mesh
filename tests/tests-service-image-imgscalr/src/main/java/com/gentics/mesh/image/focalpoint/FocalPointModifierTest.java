package com.gentics.mesh.image.focalpoint;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.image.AbstractImageTest;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static com.gentics.mesh.parameter.image.CropMode.FOCALPOINT;
import static org.junit.Assert.assertEquals;

public class FocalPointModifierTest extends AbstractImageTest {

	@Test
	public void testCropViaFocalPoint() {
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		BufferedImage outputImage = new FocalPointModifier(new ImageManipulatorOptions()).apply(bi,
				(ImageManipulationParameters) new ImageManipulationParametersImpl().setFocalPoint(0.1f, 0.1f).setCropMode(FOCALPOINT).setSize(50, 50));
		assertEquals(50, outputImage.getWidth());
		assertEquals(50, outputImage.getHeight());
	}

}
