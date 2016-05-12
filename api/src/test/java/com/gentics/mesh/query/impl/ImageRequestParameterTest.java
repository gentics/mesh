package com.gentics.mesh.query.impl;

import static com.gentics.mesh.query.impl.ImageManipulationParameter.HEIGHT_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.ImageManipulationParameter.WIDTH_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.ImageManipulationParameter.fromQuery;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.*;

import org.junit.Test;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.test.junit.Assert;

public class ImageRequestParameterTest {

	@Test
	public void testFromQuery() throws Exception {
		ImageManipulationParameter parameter = fromQuery(HEIGHT_QUERY_PARAM_KEY + "=112&" + WIDTH_QUERY_PARAM_KEY + "=142");
		assertEquals(112, parameter.getHeight().intValue());
		assertEquals(142, parameter.getWidth().intValue());
		assertTrue(parameter.isSet());

		ImageManipulationParameter param = new ImageManipulationParameter();
		ImageManipulationParameter paramsFromQuery = fromQuery(param.getQueryParameters());
		assertEquals(param.getCroph(), paramsFromQuery.getCroph());
		assertEquals(param.getCropw(), paramsFromQuery.getCropw());
		assertEquals(param.getStartx(), paramsFromQuery.getStartx());
		assertEquals(param.getStarty(), paramsFromQuery.getStarty());
		assertEquals(param.getWidth(), paramsFromQuery.getWidth());
		assertEquals(param.getHeight(), paramsFromQuery.getHeight());
		assertFalse(param.isSet());
		assertFalse(paramsFromQuery.isSet());

		param = new ImageManipulationParameter();
		param.setCroph(100);
		param.setCropw(101);
		param.setWidth(103);
		param.setHeight(104);
		param.setStartx(105);
		param.setStarty(106);
		assertTrue(param.isSet());
		paramsFromQuery = fromQuery(param.getQueryParameters());
		assertEquals(param.getCroph(), paramsFromQuery.getCroph());
		assertEquals(param.getCropw(), paramsFromQuery.getCropw());
		assertEquals(param.getStartx(), paramsFromQuery.getStartx());
		assertEquals(param.getStarty(), paramsFromQuery.getStarty());
		assertEquals(param.getWidth(), paramsFromQuery.getWidth());
		assertEquals(param.getHeight(), paramsFromQuery.getHeight());
		assertTrue(paramsFromQuery.isSet());
	}

	@Test
	public void testValidation() {
		ImageManipulationParameter request = new ImageManipulationParameter();
		request.validate();

		try {
			request = new ImageManipulationParameter();
			request.setWidth(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameter.WIDTH_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParameter();
			request.setHeight(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameter.HEIGHT_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParameter();
			request.setCroph(0);
			request.setCropw(10);
			request.setStartx(0);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameter.CROP_HEIGHT_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParameter();
			request.setCroph(10);
			request.setCropw(0);
			request.setStartx(0);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameter.CROP_WIDTH_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParameter();
			request.setCroph(10);
			request.setCropw(10);
			request.setStartx(-1);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParameter.CROP_X_QUERY_PARAM_KEY, "-1");
		}

		try {
			request = new ImageManipulationParameter();
			request.setCroph(10);
			request.setCropw(10);
			request.setStartx(0);
			request.setStarty(-1);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParameter.CROP_Y_QUERY_PARAM_KEY, "-1");
		}

		request = new ImageManipulationParameter();
		request.setCroph(1);
		request.setCropw(1);
		request.setStartx(0);
		request.setStarty(0);
		request.validate();

	}

	@Test
	public void testValidateCropBounds() throws Exception {
		try {
			ImageManipulationParameter request = new ImageManipulationParameter();
			request.setStartx(10);
			request.setStarty(10);
			request.setCroph(1);
			request.setCropw(1);
			request.validateCropBounds(10, 10);
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_out_of_bounds", "10", "10");
		}

		// Exact crop captures the exact bounds of the source image 
		ImageManipulationParameter request = new ImageManipulationParameter();
		request.setStartx(10);
		request.setStarty(10);
		request.setCroph(1);
		request.setCropw(1);
		request.validateCropBounds(11, 11);
	}

	@Test
	public void testCacheKey() {
		String cacheKey = new ImageManipulationParameter().getCacheKey();
		assertEquals("", cacheKey);

		cacheKey = new ImageManipulationParameter().setWidth(100).setHeight(200).getCacheKey();
		assertEquals("rw100rh200", cacheKey);

		cacheKey = new ImageManipulationParameter().setWidth(100).setHeight(200).setCroph(20).setCropw(21).setStartx(10).setStarty(22).getCacheKey();
		assertEquals("cx10cy22cw21ch20rw100rh200", cacheKey);
	}

}
