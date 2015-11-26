package com.gentics.mesh.query.impl;

import static com.gentics.mesh.query.impl.ImageRequestParameter.HEIGHT_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.ImageRequestParameter.WIDTH_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.ImageRequestParameter.fromQuery;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.*;

import org.junit.Test;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.test.junit.Assert;

public class ImageRequestParameterTest {

	@Test
	public void testFromQuery() throws Exception {
		ImageRequestParameter parameter = fromQuery(HEIGHT_QUERY_PARAM_KEY + "=112&" + WIDTH_QUERY_PARAM_KEY + "=142");
		assertEquals(112, parameter.getHeight().intValue());
		assertEquals(142, parameter.getWidth().intValue());
		assertTrue(parameter.isSet());

		ImageRequestParameter param = new ImageRequestParameter();
		ImageRequestParameter paramsFromQuery = fromQuery(param.getQueryParameters());
		assertEquals(param.getCroph(), paramsFromQuery.getCroph());
		assertEquals(param.getCropw(), paramsFromQuery.getCropw());
		assertEquals(param.getStartx(), paramsFromQuery.getStartx());
		assertEquals(param.getStarty(), paramsFromQuery.getStarty());
		assertEquals(param.getWidth(), paramsFromQuery.getWidth());
		assertEquals(param.getHeight(), paramsFromQuery.getHeight());
		assertFalse(param.isSet());
		assertFalse(paramsFromQuery.isSet());

		param = new ImageRequestParameter();
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
		ImageRequestParameter request = new ImageRequestParameter();
		request.validate();

		try {
			request = new ImageRequestParameter();
			request.setWidth(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (HttpStatusCodeErrorException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageRequestParameter.WIDTH_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageRequestParameter();
			request.setHeight(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (HttpStatusCodeErrorException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageRequestParameter.HEIGHT_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageRequestParameter();
			request.setCroph(0);
			request.setCropw(10);
			request.setStartx(0);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (HttpStatusCodeErrorException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageRequestParameter.CROP_HEIGHT_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageRequestParameter();
			request.setCroph(10);
			request.setCropw(0);
			request.setStartx(0);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (HttpStatusCodeErrorException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageRequestParameter.CROP_WIDTH_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageRequestParameter();
			request.setCroph(10);
			request.setCropw(10);
			request.setStartx(-1);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (HttpStatusCodeErrorException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_start_not_negative", ImageRequestParameter.CROP_X_QUERY_PARAM_KEY, "-1");
		}

		try {
			request = new ImageRequestParameter();
			request.setCroph(10);
			request.setCropw(10);
			request.setStartx(0);
			request.setStarty(-1);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (HttpStatusCodeErrorException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_start_not_negative", ImageRequestParameter.CROP_Y_QUERY_PARAM_KEY, "-1");
		}

		request = new ImageRequestParameter();
		request.setCroph(1);
		request.setCropw(1);
		request.setStartx(0);
		request.setStarty(0);
		request.validate();

	}

	@Test
	public void testValidateCropBounds() throws Exception {
		try {
			ImageRequestParameter request = new ImageRequestParameter();
			request.setStartx(10);
			request.setStarty(10);
			request.setCroph(1);
			request.setCropw(1);
			request.validateCropBounds(10, 10);
			fail("The validation should fail but it did not.");
		} catch (HttpStatusCodeErrorException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_out_of_bounds", "10", "10");
		}

		// Exact crop captures the exact bounds of the source image 
		ImageRequestParameter request = new ImageRequestParameter();
		request.setStartx(10);
		request.setStarty(10);
		request.setCroph(1);
		request.setCropw(1);
		request.validateCropBounds(11, 11);
	}

	@Test
	public void testCacheKey() {
		String cacheKey = new ImageRequestParameter().getCacheKey();
		assertEquals("", cacheKey);

		cacheKey = new ImageRequestParameter().setWidth(100).setHeight(200).getCacheKey();
		assertEquals("rw100rh200", cacheKey);

		cacheKey = new ImageRequestParameter().setWidth(100).setHeight(200).setCroph(20).setCropw(21).setStartx(10).setStarty(22).getCacheKey();
		assertEquals("cx10cy22cw21ch20rw100rh200", cacheKey);
	}

}
