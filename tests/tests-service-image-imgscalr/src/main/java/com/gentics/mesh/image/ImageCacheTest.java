package com.gentics.mesh.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.image.spi.ImageCacheCleaner;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.buffer.Buffer;
import io.vertx.reactivex.core.Vertx;

/**
 * Test cases for the image cache
 */
public class ImageCacheTest extends AbstractImageTest {
	private BinaryStorage mockedBinaryStorage;

	private HibBinary mockedBinary;

	private ImgscalrImageManipulator manipulator;

	@Before
	public void setup() {
		super.setup();

		ImageManipulatorOptions options = new ImageManipulatorOptions();
		// when running the test on windows, we set the option to touch the cache file when accessing it
		// the reason for this is that on windows, setting the last access time is typically disabled by default
		options.setImageCacheTouch(SystemUtils.IS_OS_WINDOWS);

		String binaryUUID = UUIDUtil.randomUUID();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream in = getClass().getResourceAsStream("/pictures/blume.jpg")) {
			IOUtils.copy(in, out);
		} catch (IOException e) {
			fail("Generating hash failed", e);
		}
		String hash = FileUtils.hash(Buffer.buffer(out.toByteArray())).blockingGet();
		mockedBinary = mock(HibBinary.class);
		when(mockedBinary.getUuid()).thenReturn(binaryUUID);
		when(mockedBinary.getSHA512Sum()).thenReturn(hash);

		options.setImageCacheDirectory(cacheDir.getAbsolutePath());
		mockedBinaryStorage = mock(BinaryStorage.class);
		try {
			when(mockedBinaryStorage.openBlockingStream(binaryUUID)).thenAnswer(new Answer<InputStream>() {
				@Override
				public InputStream answer(InvocationOnMock invocation) throws Throwable {
					return getClass().getResourceAsStream("/pictures/blume.jpg");
				}
			});
		} catch (IOException e) {
			fail("Mocking binary storage failed", e);
		}

		manipulator = new ImgscalrImageManipulator(Vertx.vertx(), options, mockedBinaryStorage, null);
	}

	/**
	 * Test that resizing an image creates a file in the cache and that the file is used when getting the same resized image again
	 * @throws IOException
	 */
	@Test
	public void testFileInCache() throws IOException {
		// resize the image
		String cachePath = manipulator.handleResize(mockedBinary, new ImageManipulationParametersImpl().setWidth(200)).blockingGet();
		// we expect that this fetched the inputstream of the binary
		verify(mockedBinaryStorage).openBlockingStream(Mockito.anyString());

		// cache file must now exist
		assertThat(cachePath).as("Cache path").isNotNull();
		assertThat(new File(cachePath)).as("Cache file").exists();

		// resize the image again (with same parameters)
		String cachePathSecondCall = manipulator.handleResize(mockedBinary, new ImageManipulationParametersImpl().setWidth(200)).blockingGet();

		// the returned path must be the same as before
		assertThat(cachePathSecondCall).as("Cache path from second call").isEqualTo(cachePath);
		// we expect that this did not fetch the inputstream again (because file was found in the cache)
		verify(mockedBinaryStorage).openBlockingStream(Mockito.anyString());
	}

	/**
	 * Test that the cleaner removes too old files
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	@Test
	public void testCleaner() throws IOException, InterruptedException {
		// resize the image
		String cachePath = manipulator.handleResize(mockedBinary, new ImageManipulationParametersImpl().setWidth(200)).blockingGet();
		assertThat(new File(cachePath)).as("Cache file").exists();

		// run the image cache cleaner with allowed idle time of 1 hour
		new ImageCacheCleaner(cacheDir, Duration.of(1, ChronoUnit.HOURS).get(ChronoUnit.SECONDS)).run();
		// cache file should still exist
		assertThat(new File(cachePath)).as("Cache file").exists();

		// wait 2 seconds
		Thread.sleep(Duration.of(2, ChronoUnit.SECONDS).getSeconds() * 1000L);

		// access the image again (should set the last access time)
		manipulator.handleResize(mockedBinary, new ImageManipulationParametersImpl().setWidth(200)).blockingGet();

		// run the image cache cleaner with allowed idle time of 1 second
		new ImageCacheCleaner(cacheDir, 1).run();
		// cache file should still exist
		assertThat(new File(cachePath)).as("Cache file").exists();

		// wait 2 seconds
		Thread.sleep(Duration.of(2, ChronoUnit.SECONDS).getSeconds() * 1000L);

		// run the image cache cleaner with allowed idle time of 1 second
		new ImageCacheCleaner(cacheDir, 1).run();
		// cache file should be removed now
		assertThat(new File(cachePath)).as("Cache file").doesNotExist();
	}
}
