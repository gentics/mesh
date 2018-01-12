package com.gentics.mesh.image;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Vector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.node.field.Point;

@RunWith(Parameterized.class)
public class FocalPointCropperCropTest {

	private FocalPointCropper cropper = new FocalPointCropper();

	private Parameter param;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> testData = new Vector<>();
		// 1:1 source and enlarge
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(25, 25).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(0, 0).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(50, 50).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(0, 50).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(50, 0).setExpectedStart(0, 0) });

		// 1:1 source and reduce
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(25, 25).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(0, 0).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(50, 50).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(0, 50).setExpectedStart(0, 0) });
		testData.add(new Object[] { new Parameter().setImageSize(100, 100).setTargetSize(50, 50).setFocalPoint(50, 0).setExpectedStart(0, 0) });
		
		// 1:2 source
		testData.add(new Object[] { new Parameter().setImageSize(100, 200).setTargetSize(100, 100).setFocalPoint(0, 0).setExpectedStart(0, 0) });

		// 1:2 source - fp lower right corner - should crop y
		testData.add(new Object[] { new Parameter().setImageSize(100, 200).setTargetSize(50, 50).setFocalPoint(100, 200).setExpectedStart(0, 50) });

		// 1:2 source - fp lower left corner - should crop y
		testData.add(new Object[] { new Parameter().setImageSize(100, 200).setTargetSize(50, 50).setFocalPoint(0, 200).setExpectedStart(0, 50) });
		
		// 2:1 source - fp center and target 1:1 - should crop x
		testData.add(new Object[] { new Parameter().setImageSize(200, 100).setTargetSize(50, 50).setFocalPoint(100, 50).setExpectedStart(25, 0) });
		

		return testData;
	}

	public FocalPointCropperCropTest(Parameter param) {
		this.param = param;
	}

	@Test
	public void testCalculateCropStart() {
		Point imageSize = cropper.calculateResize(param.getImageSize(), param.getTargetSize());
		boolean alignX = cropper.calculateAlignment(imageSize, param.getTargetSize());
		Point cropPos = cropper.calculateCropStart(alignX, param.getTargetSize(), imageSize, param.getFocalPoint());
		assertTrue("The expected crop start position {" + param.getExpectedStart() + "} did not match with {" + cropPos + "} for a size of {"
				+ imageSize + "}", cropPos.equals(param.getExpectedStart()));
	}

	private static class Parameter {

		private Point imageSize;
		private Point targetSize;
		private Point focalPoint;
		private Point expectedStart;

		public Parameter setFocalPoint(int x, int y) {
			this.focalPoint = new Point(x, y);
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

		public Point getFocalPoint() {
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