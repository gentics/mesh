package com.gentics.mesh.image;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.util.RxUtil.readEntireFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.exception.TikaException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.gentics.mesh.core.image.spi.ImageInfo;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import rx.Single;
import rx.functions.Action6;
import rx.functions.Func0;

public class ImgscalrImageManipulatorTest {

	private static final Logger log = LoggerFactory.getLogger(ImgscalrImageManipulatorTest.class);

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
		FileUtils.deleteDirectory(new File("data"));
		FileUtils.deleteDirectory(new File("target/data"));
	}

	@Test
	public void testResize() throws Exception {

		checkImages((imageName, width, height, color, refImage, ins) -> {
			log.debug("Handling " + imageName);
			Single<Buffer> obs = manipulator.handleResize(ins.call(), imageName, new ImageManipulationParametersImpl().setWidth(150).setHeight(180))
				.compose(readEntireFile);
			CountDownLatch latch = new CountDownLatch(1);
			obs.subscribe(buffer -> {
				try {
					assertNotNull(buffer);
					byte[] data = buffer.getBytes();
					ByteArrayInputStream bis = new ByteArrayInputStream(data);
					BufferedImage resizedImage = ImageIO.read(bis);
					bis.close();
					assertThat(resizedImage).hasSize(150, 180).matches(refImage);
					// FileUtils.writeByteArrayToFile(new File("/tmp/" + imageName + "reference.jpg"), data);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Error occured");
				}
				latch.countDown();
			});
			try {
				if (!latch.await(5, TimeUnit.SECONDS)) {
					fail("Timeout reached");
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

	}

	@Test
	public void testExtractImageInfo() throws IOException, JSONException {
		checkImages((imageName, width, height, color, refImage, ins) -> {
			Single<ImageInfo> obs = manipulator.readImageInfo(ins);
			ImageInfo info = obs.toBlocking().value();
			assertEquals("The width or image {" + imageName + "} did not match.", width, info.getWidth());
			assertEquals("The height or image {" + imageName + "} did not match.", height, info.getHeight());
			assertEquals("The dominant color of the image did not match {" + imageName + "}", color, info.getDominantColor());
		});
	}

	private void checkImages(Action6<String, Integer, Integer, String, BufferedImage, Func0<InputStream>> action) throws JSONException, IOException {
		JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/pictures/images.json")));
		JSONArray array = json.getJSONArray("images");
		for (int i = 0; i < array.length(); i++) {
			JSONObject image = array.getJSONObject(i);
			String imageName = image.getString("name");
			log.debug("Handling " + imageName);
			String path = "/pictures/" + imageName;
			InputStream ins = getClass().getResourceAsStream(path);
			if (ins == null) {
				throw new RuntimeException("Could not find image {" + path + "}");
			}
			int width = image.getInt("w");
			int height = image.getInt("h");
			String color = image.getString("dominantColor");
			String refPath = "/references/" + imageName + "reference.jpg";
			InputStream insRef = getClass().getResourceAsStream(refPath);
			if (insRef == null) {
				throw new RuntimeException("Could not find reference image {" + refPath + "}");
			}
			BufferedImage refImage = ImageIO.read(insRef);
			insRef.close();
			action.call(imageName, width, height, color, refImage, () -> ins);
		}
	}

	@Test
	public void testResizeImage() {
		// Width only
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, new ImageManipulationParametersImpl().setWidth(200));
		assertEquals(200, bi.getWidth());
		assertEquals(400, bi.getHeight());

		// Same width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		BufferedImage outputImage = manipulator.resizeIfRequested(bi, new ImageManipulationParametersImpl().setWidth(100));
		assertEquals(100, bi.getWidth());
		assertEquals(200, bi.getHeight());
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(),
				outputImage.hashCode());

		// Height only
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, new ImageManipulationParametersImpl().setHeight(50));
		assertEquals(25, bi.getWidth());
		assertEquals(50, bi.getHeight());

		// Same height
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, new ImageManipulationParametersImpl().setHeight(200));
		assertEquals(100, bi.getWidth());
		assertEquals(200, bi.getHeight());
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(),
				outputImage.hashCode());

		// Height and Width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, new ImageManipulationParametersImpl().setWidth(200).setHeight(300));
		assertEquals(200, bi.getWidth());
		assertEquals(300, bi.getHeight());

		// No parameters
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, new ImageManipulationParametersImpl());
		assertEquals(100, outputImage.getWidth());
		assertEquals(200, outputImage.getHeight());
		assertEquals("The image should not have been resized since no parameters were set.", bi.hashCode(), outputImage.hashCode());

		// Same height / width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, new ImageManipulationParametersImpl().setWidth(100).setHeight(200));
		assertEquals(100, bi.getWidth());
		assertEquals(200, bi.getHeight());
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(),
				outputImage.hashCode());

	}

	@Test(expected = GenericRestException.class)
	public void testIncompleteCropParameters() {
		// Only one parameter
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.cropIfRequested(bi, new ImageManipulationParametersImpl().setCroph(100));
	}

	@Test(expected = GenericRestException.class)
	public void testCropStartOutOfBounds() throws Exception {
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		manipulator.cropIfRequested(bi, new ImageManipulationParametersImpl().setStartx(500).setStarty(500).setCroph(20).setCropw(25));
	}

	@Test(expected = GenericRestException.class)
	public void testCropAreaOutOfBounds() throws Exception {
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		manipulator.cropIfRequested(bi, new ImageManipulationParametersImpl().setStartx(1).setStarty(1).setCroph(400).setCropw(400));
	}

	@Test
	public void testCropImage() {

		// No parameters
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		BufferedImage outputImage = manipulator.cropIfRequested(bi, new ImageManipulationParametersImpl());
		assertEquals(100, outputImage.getWidth());
		assertEquals(200, outputImage.getHeight());
		assertEquals("No cropping operation should have occured", bi.hashCode(), outputImage.hashCode());

		// Valid cropping
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.cropIfRequested(bi, new ImageManipulationParametersImpl().setStartx(1).setStarty(1).setCroph(20).setCropw(25));
		assertEquals(25, outputImage.getWidth());
		assertEquals(20, outputImage.getHeight());

	}

	@Test
	public void testTikaMetadata() throws IOException, SAXException, TikaException {
		InputStream ins = getClass().getResourceAsStream("/pictures/12382975864_09e6e069e7_o.jpg");
		Map<String, String> metadata = manipulator.getMetadata(ins).toBlocking().value();
		assertTrue(!metadata.isEmpty());
		for (String key : metadata.keySet()) {
			System.out.println(key + "=" + metadata.get(key));
		}
	}

}
