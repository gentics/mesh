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
		testData.add(new Object[] { new Parameter().setImageSize(50, 50).setTargetSize(100, 100).setFocalPoint(25, 25).setExpectedStart(0, 0) });
		return testData;
	}

	public FocalPointCropperCropTest(Parameter param) {
		this.param = param;
	}

	@Test
	public void testCalculateCropStart() {
		boolean alignX = param.getTargetSize().getX() > param.getTargetSize().getY();
		Point newSize = cropper.calculateCropStart(alignX, param.getTargetSize(), param.getImageSize(), param.getFocalPoint());
		assertTrue("The expected crop start position did not match.", newSize.equals(param.getExpectedStart()));
	}

}

class Parameter {

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
