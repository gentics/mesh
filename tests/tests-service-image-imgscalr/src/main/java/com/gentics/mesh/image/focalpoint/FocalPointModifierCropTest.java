package com.gentics.mesh.image.focalpoint;

import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Vector;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class FocalPointModifierCropTest {

	private FocalPointModifier cropper = new FocalPointModifier(new ImageManipulatorOptions());

	private Parameter param;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> testData = new Vector<>();
		// 1:1 source and enlarge
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(0.25f, 0.25f).setExpectedStart(0,
				0) });
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(0f, 0f).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(1f, 1f).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(0, 1f).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(1f, 0f).setExpectedStart(0, 0) });

		// 1:1 source and reduce
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(0.25f, 0.25f).setExpectedStart(0,
				0) });
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(0f, 0f).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(0.5f, 0.5f).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(0f, 0.5f).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(0.5f, 0f).setExpectedStart(0, 0) });

		// 1:2 source
		testData.add(new Object[] { new Parameter().setImageSize(100, 200).setTargetSize(100, 100).setFocalPoint(0f, 0f).setExpectedStart(0, 0) });

		// 1:2 source - fp lower right corner - should crop y
		testData.add(new Object[] { new Parameter().setImageSize(100, 200).setTargetSize(50, 50).setFocalPoint(1f, 1f).setExpectedStart(0, 50) });

		// 1:2 source - fp lower left corner - should crop y
		testData.add(new Object[] { new Parameter().setImageSize(100, 200).setTargetSize(50, 50).setFocalPoint(0f, 1f).setExpectedStart(0, 50) });

		// 2:1 source - fp center and target 1:1 - should crop x
		testData.add(new Object[] { new Parameter().setImageSize(200, 100).setTargetSize(50, 50).setFocalPoint(0.5f, 0.5f).setExpectedStart(25, 0) });

		// case 1 - fp top right corner- should crop x
		testData.add(new Object[] { new Parameter().setImageSize(1160, 1376).setTargetSize(200, 700).setFocalPoint(1f, 0.1f).setExpectedStart(390,
				0) });

		// case 2 - fp top left corner
		testData.add(new Object[] { new Parameter().setImageSize(1160, 1376).setTargetSize(500, 700).setFocalPoint(0.1f, 0.1f).setExpectedStart(0,
				0) });

		// case 3 - fp at top left corner
		testData.add(new Object[] { new Parameter().setImageSize(1376, 1160).setTargetSize(700, 500).setFocalPoint(0.1f, 0.1f).setExpectedStart(0,
				0) });

		// case 4 - fp at lower left corner - should crop by y
		testData.add(new Object[] { new Parameter().setImageSize(1376, 1160).setTargetSize(700, 500).setFocalPoint(0.1f, 1f).setExpectedStart(0,
				90) });

		return testData;
	}

	public FocalPointModifierCropTest(Parameter param) {
		this.param = param;
	}

	@Test
	public void testCalculateCropStart() {
		Point imageSize = cropper.calculateResize(param.getImageSize(), param.getTargetSize());
		boolean alignX = cropper.calculateAlignment(imageSize, param.getTargetSize());
		Point cropPos = cropper.calculateCropStart(alignX, param.getTargetSize(), imageSize, param.getFocalPoint());
		if (param.getImageSize().getRatio() == param.getTargetSize().getRatio()) {
			assertNull("The aspect ratio of the source and target are the same. No cropping is needed.", cropPos);
		} else {
			assertTrue("The expected crop start position {" + param.getExpectedStart() + "} did not match with {" + cropPos + "} for a size of {"
					+ imageSize + "}", cropPos.equals(param.getExpectedStart()));
		}
	}

	private static class Parameter {

		private Point imageSize;
		private Point targetSize;
		private FocalPoint focalPoint;
		private Point expectedStart;

		public Parameter setFocalPoint(float x, float y) {
			this.focalPoint = new FocalPoint(x, y);
			return this;
		}

		public Parameter setExpectedStart(int x, int y) {
			this.expectedStart = new Point(x, y);
			return this;
		}

		public Parameter setImageSize(int x, int y) {
			this.imageSize = new Point(x, y);
			return this;
		}

		public Parameter setTargetSize(int x, int y) {
			this.targetSize = new Point(x, y);
			return this;
		}

		public FocalPoint getFocalPoint() {
			return focalPoint;
		}

		public Point getImageSize() {
			return imageSize;
		}

		public Point getTargetSize() {
			return targetSize;
		}

		public Point getExpectedStart() {
			return expectedStart;
		}

		@Override
		public String toString() {
			return "size:" + imageSize.toString() + " targetSize:" + targetSize.toString() + " fp: " + focalPoint.toString();
		}
	}
}