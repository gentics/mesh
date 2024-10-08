package com.gentics.mesh.image;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.etc.config.ResampleFilter;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;

import io.vertx.reactivex.core.Vertx;

@RunWith(Parameterized.class)
public class ImgscalrResizeFilterTest extends AbstractImageTest {

	private ImgscalrImageManipulator manipulator;
	private final ResampleFilter filter;

	public ImgscalrResizeFilterTest(ResampleFilter filter) {
		this.filter = filter;
	}

	@Before
	public void setup() {
		super.setup();

		ImageManipulatorOptions options = new ImageManipulatorOptions();
		options.setResampleFilter(filter);

		options.setImageCacheDirectory(cacheDir.getAbsolutePath());
		manipulator = new ImgscalrImageManipulator(Vertx.vertx(), options, null, null);
	}

	@Parameterized.Parameters(name = "filter={0}")
	public static Collection<Object> paramData() {
		return Arrays.asList(ResampleFilter.values());
	}

	@Test
	public void testResizeImage() {
		BufferedImage bi = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
		bi = manipulator.resizeIfRequested(bi, new ImageManipulationParametersImpl().setWidth(200));
		assertEquals(200, bi.getWidth());
		assertEquals(400, bi.getHeight());
	}
}
