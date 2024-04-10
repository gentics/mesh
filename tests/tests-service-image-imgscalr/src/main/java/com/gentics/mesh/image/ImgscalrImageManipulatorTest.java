package com.gentics.mesh.image;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.util.ImageTestUtil.createMockedBinary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.image.ImageInfo;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.test.util.ImageTestUtil;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * Images may be viewed via ImageTestUtil.displayImage(outputImage);
 */
public class ImgscalrImageManipulatorTest extends AbstractImageTest {

	private static final Logger log = LoggerFactory.getLogger(ImgscalrImageManipulatorTest.class);

	private ImgscalrImageManipulator manipulator;

	@Before
	public void setup() {
		super.setup();

		ImageManipulatorOptions options = new ImageManipulatorOptions();

		options.setImageCacheDirectory(cacheDir.getAbsolutePath());
		manipulator = new ImgscalrImageManipulator(Vertx.vertx(), options, null);
	}

	@Test
	public void testResize() throws Exception {
		checkImages((imageName, width, height, color, refImage, path, bs) -> {
			log.debug("Handling " + imageName);

			HibBinary hb = createMockedBinary(path);
			BinaryDao dao = mockBinaryDao();
			when(dao.openBlockingStream(any(HibBinary.class))).thenReturn(() -> ImageTestUtil.class.getResourceAsStream(path));
			Single<byte[]> obs = manipulator
				.handleResize(hb, (ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(150).setHeight(180))
				.map(file -> Files.readAllBytes(Paths.get(file)));
			CountDownLatch latch = new CountDownLatch(1);
			obs.subscribe(data -> {
				try {
					assertNotNull(data);
					try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
						BufferedImage resizedImage = ImageIO.read(bis);
						String referenceFilename = "outputImage-" + imageName.replace(".", "_") + "-resize-reference.png";
						// when you want to update the referenceImage, execute the code below
						// and copy the files to src/test/resources/references/
						// ImageTestUtil.writePngImage(resizedImage, new File("target/" + referenceFilename));
						// ImageTestUtil.displayImage(resizedImage);
						assertThat(resizedImage).as(imageName).hasSize(150, 180).matchesReference(referenceFilename);
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
		checkImages((imageName, width, height, color, refImage, origPath, stream) -> {
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
	 * When the original filename is <code>NAME.EXTENTION</code> the result will be <code>/references/NAME.reference.EXTENSION</code>.
	 *
	 * @param filename
	 *            The filename of the original image
	 * @return The filename for the corresponding reference file
	 */
	public static String getReferenceFilename(String filename) {
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

	private void checkImages(ImageAction action) throws JSONException,
		IOException {
		JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/pictures/images.json"), StandardCharsets.UTF_8));
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
			action.call(imageName, width, height, color, refImage, path, bs);
		}
	}

	@Test
	public void testResizeImage() {
		// Width only
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, (ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200));
		assertEquals(200, bi.getWidth());
		assertEquals(400, bi.getHeight());

		// Same width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		BufferedImage outputImage = manipulator.resizeIfRequested(bi, (ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(100));
		assertEquals(100, bi.getWidth());
		assertEquals(200, bi.getHeight());
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(), outputImage
			.hashCode());

		// Height only
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, (ImageManipulationParameters) new ImageManipulationParametersImpl().setHeight(50));
		assertEquals(25, bi.getWidth());
		assertEquals(50, bi.getHeight());

		// Same height
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, (ImageManipulationParameters) new ImageManipulationParametersImpl().setHeight(200));
		assertEquals(100, bi.getWidth());
		assertEquals(200, bi.getHeight());
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", bi.hashCode(), outputImage
			.hashCode());

		// Height and Width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, (ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(300));
		assertEquals(200, bi.getWidth());
		assertEquals(300, bi.getHeight());

		// No parameters
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, (ImageManipulationParameters) new ImageManipulationParametersImpl());
		assertEquals(100, outputImage.getWidth());
		assertEquals(200, outputImage.getHeight());
		assertEquals("The image should not have been resized since no parameters were set.", bi.hashCode(), outputImage.hashCode());

		// Same height / width
		bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		outputImage = manipulator.resizeIfRequested(bi, (ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(100).setHeight(200));
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
	public void testSmartResize() throws IOException {
		// tests with horizontal input ...
		BufferedImage biH = ImageTestUtil.readImage("testgrid-horizontal-hd_1920x1080.png");

		// .. to horizontal output
		BufferedImage outputImage1 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(300));
		assertThat(outputImage1).matchesReference("outputImage1-smart-reference.png");

		// .. to vertical output
		BufferedImage outputImage2 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(300).setHeight(500));
		assertThat(outputImage2).matchesReference("outputImage2-smart-reference.png");

		// .. to square output
		BufferedImage outputImage3 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500));
		assertThat(outputImage3).matchesReference("outputImage3-smart-reference.png");

		// tests with vertical input ...
		BufferedImage biV = ImageTestUtil.readImage("testgrid-vertical-hd_1080x1920.png");

		// .. to horizontal output
		BufferedImage outputImage4 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(300));
		assertThat(outputImage4).matchesReference("outputImage4-smart-reference.png");

		// .. to vertical output
		BufferedImage outputImage5 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(300).setHeight(500));
		assertThat(outputImage5).matchesReference("outputImage5-smart-reference.png");

		// .. to square output
		BufferedImage outputImage6 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500));
		assertThat(outputImage6).matchesReference("outputImage6-smart-reference.png");

		// tests with square input ...
		BufferedImage biS = ImageTestUtil.readImage("testgrid-square_1080x1080.png");
		// .. to horizontal output
		BufferedImage outputImage7 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(300));
		assertThat(outputImage7).matchesReference("outputImage7-smart-reference.png");
		// .. to vertical output
		BufferedImage outputImage8 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(300).setHeight(500));
		assertThat(outputImage8).matchesReference("outputImage8-smart-reference.png");
		// .. to square output
		BufferedImage outputImage9 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500));
		assertThat(outputImage9).matchesReference("outputImage9-smart-reference.png");

		// test if same input and ouput format omits resampling
		// 1920x1080
		BufferedImage outputImage10 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1920).setHeight(1080));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biH.hashCode(), outputImage10
			.hashCode());

		// 1080x1920
		BufferedImage outputImage11 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1080).setHeight(1920));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biV.hashCode(), outputImage11
			.hashCode());

		// 1080x1080
		BufferedImage outputImage12 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1080).setHeight(1080));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biS.hashCode(), outputImage12
			.hashCode());

		// when you want to update the referenceImage, execute the code below
		// and copy the files to src/test/resources/references/
		// ImageTestUtil.writePngImage(outputImage1, new File("target/outputImage1-smart-reference.png"));

	}

	@Test
	public void testPropResize() throws IOException {
		// tests with horizontal input ...
		BufferedImage biH = ImageTestUtil.readImage("testgrid-horizontal-hd_1920x1080.png");

		// .. fit to width
		BufferedImage outputImage1 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(400).setResizeMode(ResizeMode.PROP));
		assertThat(outputImage1).matchesReference("outputImage1-prop-reference.png");

		// .. fit to height
		BufferedImage outputImage2 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(2000).setHeight(500).setResizeMode(ResizeMode.PROP));
		assertThat(outputImage2).matchesReference("outputImage2-prop-reference.png");

		// tests with vertical input ...
		BufferedImage biV = ImageTestUtil.readImage("testgrid-vertical-hd_1080x1920.png");

		// .. fit to width
		BufferedImage outputImage3 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(1500).setResizeMode(ResizeMode.PROP));
		assertThat(outputImage3).matchesReference("outputImage3-prop-reference.png");
		// .. fit to height
		BufferedImage outputImage4 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(400).setHeight(500).setResizeMode(ResizeMode.PROP));
		assertThat(outputImage4).matchesReference("outputImage4-prop-reference.png");

		// tests with square input ...
		BufferedImage biS = ImageTestUtil.readImage("testgrid-square_1080x1080.png");

		// .. fit to width
		BufferedImage outputImage5 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(1000).setResizeMode(ResizeMode.PROP));
		assertThat(outputImage5).matchesReference("outputImage5-prop-reference.png");

		// .. fit to height
		BufferedImage outputImage6 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1000).setHeight(500).setResizeMode(ResizeMode.PROP));
		assertThat(outputImage6).matchesReference("outputImage6-prop-reference.png");

		// test if certain formats omit resampling
		// format that is horizontal, has same width as original image, but is higher
		BufferedImage outputImage7 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1920).setHeight(1200).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the resulting images dimensions match the source image dimension.", biH.hashCode(),
			outputImage7
				.hashCode());

		// format that is horizontal, has same height as original image, but is wider
		BufferedImage outputImage8 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(2000).setHeight(1080).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the resulting images dimensions match the source image dimension.", biH.hashCode(),
			outputImage8
				.hashCode());

		// ident horizontal format
		BufferedImage outputImage9 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1920).setHeight(1080).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biH.hashCode(), outputImage9
			.hashCode());

		// format that is vertical, has same width as original image, but is higher
		BufferedImage outputImage10 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1080).setHeight(2000).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the resulting images dimensions match the source image dimension.", biV.hashCode(),
			outputImage10
				.hashCode());

		// format that is vertical, has same height as original image, but is wider
		BufferedImage outputImage11 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1200).setHeight(1920).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the resulting images dimensions match the source image dimension.", biV.hashCode(),
			outputImage11
				.hashCode());

		// ident vertical format
		BufferedImage outputImage12 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1080).setHeight(1920).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biV.hashCode(), outputImage12
			.hashCode());

		// format that is square, has same width as original image, but is higher
		BufferedImage outputImage13 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1080).setHeight(1200).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the resulting images dimensions match the source image dimension.", biS.hashCode(),
			outputImage13
				.hashCode());

		// format that is vertical, has same height as original image, but is wider
		BufferedImage outputImage14 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1200).setHeight(1080).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the resulting images dimensions match the source image dimension.", biS.hashCode(),
			outputImage14
				.hashCode());

		// ident square format
		BufferedImage outputImage15 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1080).setHeight(1080).setResizeMode(ResizeMode.PROP));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biS.hashCode(), outputImage15
			.hashCode());

		// when you want to update the referenceImage, execute the code below
		// and copy the files to src/test/resources/references/
		// ImageTestUtil.writePngImage(outputImage1, new File("target/outputImage1-prop-reference.png"));
		// ImageTestUtil.writePngImage(outputImage2, new File("target/outputImage2-prop-reference.png"));
		// ImageTestUtil.writePngImage(outputImage3, new File("target/outputImage3-prop-reference.png"));
		// ImageTestUtil.writePngImage(outputImage4, new File("target/outputImage4-prop-reference.png"));
		// ImageTestUtil.writePngImage(outputImage5, new File("target/outputImage5-prop-reference.png"));
		// ImageTestUtil.writePngImage(outputImage6, new File("target/outputImage6-prop-reference.png"));

	}

	@Test
	public void testSmartResizeCrop() throws IOException {
		// tests with horizontal input ...
		BufferedImage biH = ImageTestUtil.readImage("testgrid-horizontal-hd_1920x1080.png");

		// .. to horizontal output
		BufferedImage outputImage1 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) (ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(200).setRect(0, 0, 540, 960).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));//
		assertThat(outputImage1).matchesReference("outputImage1-smart-crop-reference.png");

		// .. to vertical output
		BufferedImage outputImage2 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(500).setRect(0, 0, 540, 960).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));
		assertThat(outputImage2).matchesReference("outputImage2-smart-crop-reference.png");

		// .. to square output
		BufferedImage outputImage3 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setRect(0, 0, 540, 960).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));
		assertThat(outputImage3).matchesReference("outputImage3-smart-crop-reference.png");

		// tests with vertical input ...
		BufferedImage biV = ImageTestUtil.readImage("testgrid-vertical-hd_1080x1920.png");

		// .. to horizontal output
		BufferedImage outputImage4 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(200).setRect(0, 0, 960, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));
		assertThat(outputImage4).matchesReference("outputImage4-smart-crop-reference.png");

		// .. to vertical output
		BufferedImage outputImage5 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(500).setRect(0, 0, 960, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));
		assertThat(outputImage5).matchesReference("outputImage5-smart-crop-reference.png");

		// .. to square output
		BufferedImage outputImage6 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setRect(0, 0, 960, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));
		assertThat(outputImage6).matchesReference("outputImage6-smart-crop-reference.png");

		// tests with square input ...
		BufferedImage biS = ImageTestUtil.readImage("testgrid-square_1080x1080.png");

		// .. to horizontal output
		BufferedImage outputImage7 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(200).setRect(0, 0, 540, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));
		assertThat(outputImage7).matchesReference("outputImage7-smart-crop-reference.png");

		// .. to vertical output
		BufferedImage outputImage8 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(500).setRect(0, 0, 540, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));
		assertThat(outputImage8).matchesReference("outputImage8-smart-crop-reference.png");

		// .. to square output
		BufferedImage outputImage9 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setRect(0, 0, 540, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.SMART));
		assertThat(outputImage9).matchesReference("outputImage9-smart-crop-reference.png");

		// when you want to update the referenceImage, execute the code below
		// and copy the files to src/test/resources/references/

		// ImageTestUtil.writePngImage(outputImage1, new File("target/outputImage1-smart-crop-reference.png"));

	}

	@Test
	public void testForceResize() throws IOException {
		// tests with horizontal input ...
		BufferedImage biH = ImageTestUtil.readImage("testgrid-horizontal-hd_1920x1080.png");

		// .. to horizontal output
		BufferedImage outputImage1 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(200).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage1).matchesReference("outputImage1-force-reference.png");

		// .. to vertical output
		BufferedImage outputImage2 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(500).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage2).matchesReference("outputImage2-force-reference.png");

		// .. to square output
		BufferedImage outputImage3 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage3).matchesReference("outputImage3-force-reference.png");

		// tests with vertical input ...
		BufferedImage biV = ImageTestUtil.readImage("testgrid-vertical-hd_1080x1920.png");

		// .. to horizontal output
		BufferedImage outputImage4 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(200).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage4).matchesReference("outputImage4-force-reference.png");

		// .. to vertical output
		BufferedImage outputImage5 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(500).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage5).matchesReference("outputImage5-force-reference.png");

		// .. to square output
		BufferedImage outputImage6 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage6).matchesReference("outputImage6-force-reference.png");

		// tests with square input ...
		BufferedImage biS = ImageTestUtil.readImage("testgrid-square_1080x1080.png");

		// .. to horizontal output
		BufferedImage outputImage7 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(300).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage7).matchesReference("outputImage7-force-reference.png");

		// .. to vertical output
		BufferedImage outputImage8 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(300).setHeight(500).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage8).matchesReference("outputImage8-force-reference.png");
		// .. to square output
		BufferedImage outputImage9 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage9).matchesReference("outputImage9-force-reference.png");

		// test if same input and ouput format omits resampling
		// 1920x1080
		BufferedImage outputImage10 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1920).setHeight(1080).setResizeMode(ResizeMode.FORCE));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biH.hashCode(), outputImage10
			.hashCode());

		// 1080x1920
		BufferedImage outputImage11 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1080).setHeight(1920).setResizeMode(ResizeMode.FORCE));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biV.hashCode(), outputImage11
			.hashCode());

		// 1080x1080
		BufferedImage outputImage12 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(1080).setHeight(1080).setResizeMode(ResizeMode.FORCE));
		assertEquals("The image should not have been resized since the parameters match the source image dimension.", biS.hashCode(), outputImage12
			.hashCode());

		// when you want to update the referenceImage, execute the code below
		// and copy the files to src/test/resources/references/
		// ImageTestUtil.writePngImage(outputImage9, new File("target/outputImage9-force-reference.png"));
	}

	@Test
	public void testForceResizeCrop() throws IOException {
		// tests with horizontal input ...
		BufferedImage biH = ImageTestUtil.readImage("testgrid-horizontal-hd_1920x1080.png");
		// .. to horizontal output
		BufferedImage outputImage1 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(200).setRect(0, 0, 540, 960).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));//
		assertThat(outputImage1).matchesReference("outputImage1-force-crop-reference.png");
		// .. to vertical output
		BufferedImage outputImage2 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(500).setRect(0, 0, 540, 960).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage2).matchesReference("outputImage2-force-crop-reference.png");
		// .. to square output
		BufferedImage outputImage3 = manipulator.cropAndResize(biH,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setRect(0, 0, 540, 960).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage3).matchesReference("outputImage3-force-crop-reference.png");

		// tests with vertical input ...
		BufferedImage biV = ImageTestUtil.readImage("testgrid-vertical-hd_1080x1920.png");
		// .. to horizontal output
		BufferedImage outputImage4 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(200).setRect(0, 0, 960, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage4).matchesReference("outputImage4-force-crop-reference.png");

		// .. to vertical output
		BufferedImage outputImage5 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(500).setRect(0, 0, 960, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage5).matchesReference("outputImage5-force-crop-reference.png");

		// .. to square output
		BufferedImage outputImage6 = manipulator.cropAndResize(biV,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setRect(0, 0, 960, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage6).matchesReference("outputImage6-force-crop-reference.png");

		// tests with square input ...
		BufferedImage biS = ImageTestUtil.readImage("testgrid-square_1080x1080.png");

		// .. to horizontal output
		BufferedImage outputImage7 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(200).setRect(0, 0, 540, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage7).matchesReference("outputImage7-force-crop-reference.png");

		// .. to vertical output
		BufferedImage outputImage8 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(200).setHeight(500).setRect(0, 0, 540, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage8).matchesReference("outputImage8-force-crop-reference.png");

		// .. to square output
		BufferedImage outputImage9 = manipulator.cropAndResize(biS,
			(ImageManipulationParameters) new ImageManipulationParametersImpl().setWidth(500).setHeight(500).setRect(0, 0, 540, 540).setCropMode(CropMode.RECT)
				.setResizeMode(ResizeMode.FORCE));
		assertThat(outputImage9).matchesReference("outputImage9-force-crop-reference.png");

		// when you want to update the referenceImage, execute the code below
		// and copy the files to src/test/resources/references/
		// ImageTestUtil.writePngImage(outputImage9, new File("target/outputImage9-force-crop-reference.png"));
	}

	@Test
	public void testTikaMetadata() throws IOException, TikaException {
		InputStream ins = getClass().getResourceAsStream("/pictures/12382975864_09e6e069e7_o.jpg");
		Map<String, String> metadata = manipulator.getMetadata(ins).blockingGet();
		assertTrue(!metadata.isEmpty());
		for (String key : metadata.keySet()) {
			System.out.println(key + "=" + metadata.get(key));
		}
	}
}
