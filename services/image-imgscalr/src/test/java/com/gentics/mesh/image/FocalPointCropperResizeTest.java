package com.gentics.mesh.image;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Vector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.node.field.Point;

@RunWith(Parameterized.class)
public class FocalPointCropperResizeTest {

	private FocalPointCropper cropper = new FocalPointCropper();

	private Point imageSize;
	private Point targetSize;
	private Point expectedSize;

	@Parameterized.Parameters(name = "fp: size: {0} {1} size: {2}:{3} - {4}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> testData = new Vector<>();
		testData.add(new Object[] { new Point(100, 100), new Point(50, 50), new Point(20, 20) });
		// testData.add(new Object[] { 100, 100, 25, 50 });<
		// testData.add(new Object[] { 100, 100, 50, 25 });
		// testData.add(new Object[] { 100, 100, 100, 100 });
		// testData.add(new Object[] { 100, 100, 150, 150 });
		// testData.add(new Object[] { 100, 100, 150, 100 });
		// testData.add(new Object[] { 100, 100, 100, 150 });
		return testData;
	}

	public FocalPointCropperResizeTest(Point source, Point target, Point expectedSize) {
		this.imageSize = source;
		this.targetSize = target;
		this.expectedSize = expectedSize;
	}

	@Test
	public void testCalculateResize() {
		Point newSize = cropper.calculateResize(imageSize, targetSize);
		assertTrue("The size {" + newSize + "} did not match the size {" + expectedSize + "}", newSize.equals(expectedSize));
	}

}
