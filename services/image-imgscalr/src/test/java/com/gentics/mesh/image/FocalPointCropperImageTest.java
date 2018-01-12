package com.gentics.mesh.image;

import static com.gentics.mesh.parameter.image.CropMode.FOCALPOINT;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.node.field.Point;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;

@RunWith(Parameterized.class)
public class FocalPointCropperImageTest extends AbstractImageTest {

	private FocalPointCropper cropper = new FocalPointCropper();

	private Point focalPoint;
	private Point targetSize;
	private String imageName;

	public FocalPointCropperImageTest(int fpx, int fpy, int width, int height, String imageName) {
		this.focalPoint = new Point(fpx, fpy);
		this.targetSize = new Point(width, height);
		this.imageName = imageName;
	}

	@Parameterized.Parameters(name = "fp: {0}:{1} - size: {2}:{3} - {4}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> testData = new Vector<>();
		// blume2.jpeg: 1160x1376
		testData.add(new Object[] { 0, 0, 100, 100, "blume2.jpeg" });
		testData.add(new Object[] { 1, 1, 100, 100, "blume2.jpeg" });

		testData.add(new Object[] { 100, 100, 200, 700, "blume2.jpeg" });
		testData.add(new Object[] { 100, 1300, 200, 700, "blume2.jpeg" });
		testData.add(new Object[] { 1100, 100, 200, 700, "blume2.jpeg" });

		testData.add(new Object[] { 100, 100, 500, 700, "blume2.jpeg" });
		testData.add(new Object[] { 100, 1300, 500, 700, "blume2.jpeg" });

		testData.add(new Object[] { 100, 100, 700, 500, "blume2.jpeg" });
		testData.add(new Object[] { 100, 1300, 700, 500, "blume2.jpeg" });

		// blume2_rotated.jpeg: 1376*1160

		testData.add(new Object[] { 100, 100, 200, 700, "blume2_rotated.jpeg" });
		testData.add(new Object[] { 100, 1100, 200, 700, "blume2_rotated.jpeg" });
		testData.add(new Object[] { 1100, 100, 200, 700, "blume2_rotated.jpeg" });

		testData.add(new Object[] { 100, 100, 500, 700, "blume2_rotated.jpeg" });
		testData.add(new Object[] { 100, 1100, 500, 700, "blume2.jpeg" });

		testData.add(new Object[] { 100, 100, 700, 500, "blume2_rotated.jpeg" });
		testData.add(new Object[] { 100, 1100, 700, 500, "blume2_rotated.jpeg" });

		return testData;
	}

	
	@Test
	public void testCrop() throws IOException {
		File targetFile = new File("target/output_" + imageName + "-" + focalPoint.toString() + "-" + targetSize.toString() + ".jpg");
		targetFile.delete();

		BufferedImage img = getImage(imageName);
		ImageIO.write(img, "jpeg", new File("target", "source.jpg"));

		ImageManipulationParameters param = new ImageManipulationParametersImpl();
		param.setCropMode(FOCALPOINT);
		param.setFocalPointDebug(true);
		param.setFocalPoint(focalPoint);
		param.setSize(targetSize);
		param.validate();

		BufferedImage result = cropper.apply(img, param);
		ImageIO.write(result, "jpeg", targetFile);
	}

}
