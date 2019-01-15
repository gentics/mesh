package com.gentics.mesh.image;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.gentics.mesh.core.image.spi.ImageInfo;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.util.PropReadFileStream;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

public class ImgscalrImageManipulatorTest extends AbstractImageTest {

	private static final Logger log = LoggerFactory.getLogger(ImgscalrImageManipulatorTest.class);

	private ImgscalrImageManipulator manipulator;

	@Before
	public void setup() {
		super.setup();

		ImageManipulatorOptions options = new ImageManipulatorOptions();

		options.setImageCacheDirectory(cacheDir.getAbsolutePath());
		manipulator = new ImgscalrImageManipulator(Vertx.vertx(), options);
	}

	@Test
	public void testResize() throws Exception {
		checkImages((imageName, width, height, color, refImage, bs) -> {
			log.debug("Handling " + imageName);

			Single<Buffer> obs = manipulator.handleResize(bs, imageName, new ImageManipulationParametersImpl().setWidth(150).setHeight(180)).map(
				PropReadFileStream::getFile).map(RxUtil::toBufferFlow).flatMap(RxUtil::readEntireData);
			CountDownLatch latch = new CountDownLatch(1);
			obs.subscribe(buffer -> {
				try {
					assertNotNull(buffer);
					byte[] data = buffer.getBytes();
					try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
						BufferedImage resizedImage = ImageIO.read(bis);
						assertThat(resizedImage).as(imageName).hasSize(150, 180).matches(refImage);
					}
				} catch (Exception e) {
					e.printStackTrace();
					fail("Error occured");
				}
				latch.countDown();
			});
			try {
				if (!latch.await(20, TimeUnit.SECONDS)) {
					fail("Timeout reached");
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

	}

	@Test
	public void testExtractImageInfo() throws IOException, JSONException {
		checkImages((imageName, width, height, color, refImage, stream) -> {
			String path = RxUtil.readEntireData(stream).map(data -> {
				File file = new File("/tmp/" + imageName + "reference.jpg");
				FileUtils.writeByteArrayToFile(file, data.getBytes());
				return file.getAbsolutePath();
			}).blockingGet();

			Single<ImageInfo> obs = manipulator.readImageInfo(path);
			ImageInfo info = obs.blockingGet();
			assertEquals("The width or image {" + imageName + "} did not match.", width, info.getWidth());
			assertEquals("The height or image {" + imageName + "} did not match.", height, info.getHeight());
			assertEquals("The dominant color of the image did not match {" + imageName + "}", color, info.getDominantColor());
		});
	}

	/**
	 * Get the corresponding reference filename for the given input filename.
	 *
	 * When the original filename is <code>NAME.EXTENTION</code> the result will be
	 * <code>/references/NAME.reference.EXTENSION</code>.
	 *
	 * @param filename The filename of the original image
	 * @return The filename for the corresponding reference file
	 */
	private String getReferenceFilename(String filename) {
		int idx = filename.lastIndexOf('.');
		String name;
		String ext;

		if (idx >= 0) {
			name = filename.substring(0, idx);
			ext = filename.substring(idx);
		} else {
			name = filename;
			ext = "";
		}

		return "/references/" + name + ".reference" + ext;
	}

	private void checkImages(ImageAction<String, Integer, Integer, String, BufferedImage, Flowable<Buffer>> action) throws JSONException,
		IOException {
		JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/pictures/images.json"), Charset.defaultCharset()));
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
			byte[] bytes = IOUtils.toByteArray(ins);
			Flowable<Buffer> bs = Flowable.just(Buffer.buffer(bytes));
			int width = image.getInt("w");
			int height = image.getInt("h");
			String color = image.getString("dominantColor");
			String refPath = getReferenceFilename(imageName);
			InputStream insRef = getClass().getResourceAsStream(refPath);
			if (insRef == null) {
				throw new RuntimeException("Could not find reference image {" + refPath + "}");
			}
			BufferedImage refImage = ImageIO.read(insRef);
			insRef.close();
			action.call(imageName, width, height, color, refImage, bs);
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
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(), outputImage
			.hashCode());

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
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(), outputImage
			.hashCode());

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
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(), outputImage
			.hashCode());

	}

	@Test(expected = GenericRestException.class)
	public void testCropStartOutOfBounds() throws Exception {
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		manipulator.crop(bi, new ImageManipulationParametersImpl().setRect(500, 500, 20, 25).getRect());
	}

	@Test(expected = GenericRestException.class)
	public void testCropAreaOutOfBounds() throws Exception {
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		manipulator.crop(bi, new ImageManipulationParametersImpl().setRect(1, 1, 400, 400).getRect());
	}

	@Test
	public void testCropImage() {

		// No parameters
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		BufferedImage outputImage = manipulator.crop(bi, null);
		assertEquals(100, outputImage.getWidth());
		assertEquals(200, outputImage.getHeight());
		assertEquals("No cropping operation should have occured", bi.hashCode(), outputImage.hashCode());

		// Valid cropping
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.crop(bi, new ImageManipulationParametersImpl().setRect(1, 1, 20, 25).getRect());
		assertEquals(25, outputImage.getWidth());
		assertEquals(20, outputImage.getHeight());

	}

	@Test
	public void testTikaMetadata() throws IOException, SAXException, TikaException {
		InputStream ins = getClass().getResourceAsStream("/pictures/12382975864_09e6e069e7_o.jpg");
		Map<String, String> metadata = manipulator.getMetadata(ins).blockingGet();
		assertTrue(!metadata.isEmpty());
		for (String key : metadata.keySet()) {
			System.out.println(key + "=" + metadata.get(key));
		}
	}

}
