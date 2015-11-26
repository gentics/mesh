package com.gentics.mesh.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.query.impl.ImageRequestParameter;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import rx.Observable;

public class ImgscalrImageManipulatorTest {

	private ImgscalrImageManipulator manipulator;
	private File cacheDir;
	private ImageManipulatorOptions options = new ImageManipulatorOptions();

	@Before
	public void setup() {
		cacheDir = new File("target/cacheDir_" + System.currentTimeMillis());
		manipulator = new ImgscalrImageManipulator(Vertx.vertx(), options);
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(cacheDir);
	}

	@Test
	public void testResize() throws Exception {

		List<String> imageNames = IOUtils.readLines(getClass().getResourceAsStream("/pictures/images.lst"));
		for (String imageName : imageNames) {
			System.out.println("Handling " + imageName);
			InputStream ins = getClass().getResourceAsStream("/pictures/" + imageName);
			Observable<Buffer> obs = manipulator.handleResize(ins, imageName, new ImageRequestParameter().setWidth(150).setHeight(180));
			CountDownLatch latch = new CountDownLatch(1);
			obs.subscribe(buffer -> {
				assertNotNull(buffer);
				latch.countDown();
			});
			if (!latch.await(5, TimeUnit.SECONDS)) {
				fail("Timeout reached");
			}
		}
	}

	@Test
	public void testResizeImage() {
		// Width only
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, new ImageRequestParameter().setWidth(200));
		assertEquals(200, bi.getWidth());
		assertEquals(400, bi.getHeight());

		// Same width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		BufferedImage outputImage = manipulator.resizeIfRequested(bi, new ImageRequestParameter().setWidth(100));
		assertEquals(100, bi.getWidth());
		assertEquals(200, bi.getHeight());
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(),
				outputImage.hashCode());

		// Height only
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, new ImageRequestParameter().setHeight(50));
		assertEquals(25, bi.getWidth());
		assertEquals(50, bi.getHeight());

		// Same height
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, new ImageRequestParameter().setHeight(200));
		assertEquals(100, bi.getWidth());
		assertEquals(200, bi.getHeight());
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(),
				outputImage.hashCode());

		// Height and Width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, new ImageRequestParameter().setWidth(200).setHeight(300));
		assertEquals(200, bi.getWidth());
		assertEquals(300, bi.getHeight());

		// No parameters
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, new ImageRequestParameter());
		assertEquals(100, outputImage.getWidth());
		assertEquals(200, outputImage.getHeight());
		assertEquals("The image should not have been resized since no parameters were set.", bi.hashCode(), outputImage.hashCode());

		// Same height / width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, new ImageRequestParameter().setWidth(100).setHeight(200));
		assertEquals(100, bi.getWidth());
		assertEquals(200, bi.getHeight());
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(),
				outputImage.hashCode());

	}

	@Test(expected = HttpStatusCodeErrorException.class)
	public void testIncompleteCropParameters() {
		// Only one parameter
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.cropIfRequested(bi, new ImageRequestParameter().setCroph(100));
	}

	@Test(expected = HttpStatusCodeErrorException.class)
	public void testCropStartOutOfBounds() throws Exception {
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		manipulator.cropIfRequested(bi, new ImageRequestParameter().setStartx(500).setStarty(500).setCroph(20).setCropw(25));
	}

	@Test(expected = HttpStatusCodeErrorException.class)
	public void testCropAreaOutOfBounds() throws Exception {
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		manipulator.cropIfRequested(bi, new ImageRequestParameter().setStartx(1).setStarty(1).setCroph(400).setCropw(400));
	}

	@Test
	public void testCropImage() {

		// No parameters
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		BufferedImage outputImage = manipulator.cropIfRequested(bi, new ImageRequestParameter());
		assertEquals(100, outputImage.getWidth());
		assertEquals(200, outputImage.getHeight());
		assertEquals("No cropping operation should have occured", bi.hashCode(), outputImage.hashCode());

		// Valid cropping
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.cropIfRequested(bi, new ImageRequestParameter().setStartx(1).setStarty(1).setCroph(20).setCropw(25));
		assertEquals(25, outputImage.getWidth());
		assertEquals(20, outputImage.getHeight());

	}

}
