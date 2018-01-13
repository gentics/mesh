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

import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;

@RunWith(Parameterized.class)
public class FocalPointCropperImageTest extends AbstractImageTest {

	private FocalPointCropper cropper = new FocalPointCropper();

	private Parameter parameter;

	public FocalPointCropperImageTest(Parameter parameter) {
		this.parameter = parameter;
	}

	@Parameterized.Parameters(name = "fp: {0}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> testData = new Vector<>();
		// blume2.jpeg: 1160x1376
		testData.add(new Object[] { new Parameter().setFocalPoint(0f, 0f).setTargetSize(100, 100).setImageName("blume2.jpeg") });
		testData.add(new Object[] { new Parameter().setFocalPoint(0.01f, 0.01f).setTargetSize(100, 100).setImageName("blume2.jpeg") });

		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 0.1f).setTargetSize(200, 700).setImageName("blume2.jpeg") });
		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 1f).setTargetSize(200, 700).setImageName("blume2.jpeg") });
		testData.add(new Object[] { new Parameter().setFocalPoint(1f, 0.1f).setTargetSize(200, 700).setImageName("blume2.jpeg") });

		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 0.1f).setTargetSize(500, 700).setImageName("blume2.jpeg") });
		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 1f).setTargetSize(500, 700).setImageName("blume2.jpeg") });

		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 1f).setTargetSize(700, 500).setImageName("blume2.jpeg") });

		// blume2_rotated.jpeg: 1376*1160

		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 0.1f).setTargetSize(200, 700).setImageName("blume2_rotated.jpeg") });
		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 1f).setTargetSize(200, 700).setImageName("blume2_rotated.jpeg") });
		testData.add(new Object[] { new Parameter().setFocalPoint(1f, 0.1f).setTargetSize(200, 700).setImageName("blume2_rotated.jpeg") });

		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 0.1f).setTargetSize(500, 700).setImageName("blume2_rotated.jpeg") });
		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 1f).setTargetSize(500, 700).setImageName("blume2.jpeg") });

		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 0.1f).setTargetSize(700, 500).setImageName("blume2_rotated.jpeg") });
		testData.add(new Object[] { new Parameter().setFocalPoint(0.1f, 1f).setTargetSize(700, 500).setImageName("blume2_rotated.jpeg") });

		return testData;
	}

	@Test
	public void testCrop() throws IOException {
		String imageName = parameter.getImageName();
		FocalPoint focalPoint = parameter.getFocalPoint();
		Point targetSize = parameter.getTargetSize();

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

	private static class Parameter {

		private FocalPoint focalPoint;
		private Point targetSize;
		private String imageName;

		public Parameter setTargetSize(int x, int y) {
			this.targetSize = new Point(x, y);
			return this;
		}

		public Parameter setFocalPoint(float x, float y) {
			this.focalPoint = new FocalPoint(x, y);
			return this;
		}

		public Parameter setImageName(String imageName) {
			this.imageName = imageName;
			return this;
		}

		public Point getTargetSize() {
			return targetSize;
		}

		public FocalPoint getFocalPoint() {
			return focalPoint;
		}

		public String getImageName() {
			return imageName;
		}

		@Override
		public String toString() {
			return "fp: " + focalPoint + " target:" + targetSize + " img:" + imageName;
		}

	}

}
