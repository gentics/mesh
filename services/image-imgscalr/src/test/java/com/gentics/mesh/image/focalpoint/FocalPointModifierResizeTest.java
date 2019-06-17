package com.gentics.mesh.image.focalpoint;

import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Vector;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class FocalPointModifierResizeTest {

	private FocalPointModifier cropper = new FocalPointModifier(new ImageManipulatorOptions());

	private Parameter param;

	@Parameterized.Parameters(name = "fp: size: {0}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> testData = new Vector<>();

		// source and target are same = no modification
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(50, 50).setExpectedSize(50, 50) });

		// enlarge - 1:1 - No need to crop we just resize to target
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setExpectedSize(100, 100) });

		// enlarge - 1:2 - No operation and crop by x
		testData.add(new Object[] { new Parameter().setImageSize(50, 100).setTargetSize(100, 100).setExpectedSize(100, 200) });

		// enlarge - 2:1 - Enlarge by x and crop y
		testData.add(new Object[] { new Parameter().setImageSize(100, 50).setTargetSize(100, 100).setExpectedSize(200, 100) });

		// reduce - 1:1 - No need to crop we just resize to target
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setExpectedSize(50, 50) });

		// reduce - 1:2 - No need to resize - y will be cropped
		testData.add(new Object[] { new Parameter().setImageSize(50, 100).setTargetSize(50, 50).setExpectedSize(50, 100) });

		// reduce - 2:1 - No need to resize - x will be cropped
		testData.add(new Object[] { new Parameter().setImageSize(100, 50).setTargetSize(50, 50).setExpectedSize(100, 50) });

		// case 1
		testData.add(new Object[] { new Parameter().setImageSize(1160, 1376).setTargetSize(200, 700).setExpectedSize(590, 700) });

		// case 2
		testData.add(new Object[] { new Parameter().setImageSize(1160, 1376).setTargetSize(500, 700).setExpectedSize(590, 700) });

		// case 3
		testData.add(new Object[] { new Parameter().setImageSize(1376, 1160).setTargetSize(700, 500).setExpectedSize(700, 590) });

		return testData;
	}

	public FocalPointModifierResizeTest(Parameter param) {
		this.param = param;
	}

	@Test
	public void testCalculateResize() {
		Point newSize = cropper.calculateResize(param.getImageSize(), param.getTargetSize());
		assertTrue("The resulting size {" + newSize + "} did not match the expected size {" + param.getExpectedSize() + "}",
				newSize.equals(param.getExpectedSize()));
	}

	private static class Parameter {

		private Point imageSize;
		private Point targetSize;
		private Point expectedSize;

		public Parameter setExpectedSize(int x, int y) {
			this.expectedSize = new Point(x, y);
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

		public Point getImageSize() {
			return imageSize;
		}

		public Point getTargetSize() {
			return targetSize;
		}

		public Point getExpectedSize() {
			return expectedSize;
		}

		@Override
		public String toString() {
			return "src: " + imageSize + " target:" + targetSize;
		}

	}
}
