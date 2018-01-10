package com.gentics.mesh.parameter.image;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.test.Assert;
import com.gentics.mesh.parameter.ImageManipulationParameters;

public class ImageRectTest {

	@Test
	public void testImageRect() {
		ImageRect rect = new ImageRect("1,2,3,4");
		assertEquals(1, rect.getStartX());
		assertEquals(2, rect.getStartY());
		assertEquals(3, rect.getWidth());
		assertEquals(4, rect.getHeight());
	}

	@Test(expected = GenericRestException.class)
	public void testIncompleteCropParameters() {
		// Only one parameter
		ImageRect rect = new ImageRect("1");
		rect.validate();
	}

	@Test
	public void testValidation() {
		try {
			ImageRect rect = new ImageRect("0,0,0,10");
			rect.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.RECT_QUERY_PARAM_KEY, "0");
		}

		try {
			ImageRect rect = new ImageRect("0,0,10,0");
			rect.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.RECT_QUERY_PARAM_KEY, "0");
		}

		try {
			ImageRect rect = new ImageRect("-1,0,10,10");
			rect.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParameters.RECT_QUERY_PARAM_KEY, "-1");
		}

		try {
			ImageRect rect = new ImageRect("0,-1,10,10");
			rect.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParameters.RECT_QUERY_PARAM_KEY, "-1");
		}

		ImageRect rect = new ImageRect("0,0,1,1");
		rect.validate();

	}
}
