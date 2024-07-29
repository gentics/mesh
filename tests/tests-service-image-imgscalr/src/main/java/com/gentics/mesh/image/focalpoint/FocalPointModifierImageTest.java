package com.gentics.mesh.image.focalpoint;

import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.image.AbstractImageTest;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import static com.gentics.mesh.parameter.image.CropMode.FOCALPOINT;

@RunWith(Parameterized.class)
public class FocalPointModifierImageTest extends AbstractImageTest {

	private FocalPointModifier cropper = new FocalPointModifier(new ImageManipulatorOptions());

	private Parameter parameter;

	public FocalPointModifierImageTest(Parameter parameter) {
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

		// Center of flower
		testData.add(new Object[] { new Parameter().setFocalPoint(0.5f, 0.5f).setTargetSize(700, 500).setImageName("blume2_rotated.jpeg") });

		// Center of flower with zoom
		testData.add(
				new Object[] { new Parameter().setFocalPoint(0.5f, 0.5f).setZoom(2f).setTargetSize(700, 500).setImageName("blume2_rotated.jpeg") });

		// bottom right with zoom
		testData.add(new Object[] { new Parameter().setFocalPoint(1f, 1f).setZoom(2f).setTargetSize(700, 500).setImageName("blume2_rotated.jpeg") });

		// top left with zoom
		testData.add(new Object[] { new Parameter().setFocalPoint(0f, 0f).setZoom(2f).setTargetSize(700, 500).setImageName("blume2_rotated.jpeg") });

		// High zoom with offset FP on one axis
		testData.add(new Object[] { new Parameter().setFocalPoint(0.5f, 0.25f).setZoom(4f).setTargetSize(800, 600).setImageName("blume2.jpeg") });

		// case 5 - check out of bounds focus crop
		testData.add(new Object[] { new Parameter().setFocalPoint(0.5f, 0.5f).setZoom(2f).setImageName("blume2.jpeg").setTargetSize(500, 1250) });

		testData.add(new Object[] { new Parameter().setFocalPoint(0.5f, 0.5f).setZoom(2f).setImageName("blume2.jpeg").setTargetSize(1250, 500) });

		testData.add(new Object[] { new Parameter().setFocalPoint(1f, 1f).setZoom(1.1f).setImageName("blume2.jpeg").setTargetSize(1250, 1200) });

		testData.add(new Object[] { new Parameter().setFocalPoint(1f, 1f).setZoom(1.1f).setImageName("blume2.jpeg").setTargetSize(1250, 200) });

		testData.add(new Object[] { new Parameter().setFocalPoint(1f, 1f).setZoom(1.1f).setImageName("blume2.jpeg").setTargetSize(250, 1200) });

		return testData;
	}

	@Test
	public void testManipulator() throws IOException {
		String imageName = parameter.getImageName();
		FocalPoint focalPoint = parameter.getFocalPoint();
		Point targetSize = parameter.getTargetSize();
		Float zoom = parameter.getZoom();

		File targetFile = new File("target/output_" + imageName + "-" + focalPoint.toString() + "-" + targetSize.toString() + "-z" + zoom + ".jpg");
		targetFile.delete();

		BufferedImage img = getImage(imageName);
		ImageIO.write(img, "jpeg", new File("target", "source.jpg"));

		ImageManipulationParameters param = new ImageManipulationParametersImpl();
		param.setCropMode(FOCALPOINT);
		param.setFocalPointDebug(true);
		param.setFocalPoint(focalPoint);
		param.setSize(targetSize);
		param.setFocalPointZoom(zoom);
		param.validate();

		BufferedImage result = cropper.apply(img, param);
		ImageIO.write(result, "jpeg", targetFile);
	}

	private static class Parameter {

		private FocalPoint focalPoint;
		private Point targetSize;
		private String imageName;
		private Float zoom;

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
			return "fp: " + focalPoint + " target:" + targetSize + " zoom: " + zoom + " img:" + imageName;
		}

		public Parameter setZoom(Float zoom) {
			this.zoom = zoom;
			return this;
		}

		public Float getZoom() {
			return zoom;
		}
	}

}
