package com.gentics.mesh.query.impl;

import static com.gentics.mesh.parameter.impl.ImageManipulationParameters.HEIGHT_QUERY_PARAM_KEY;
import static com.gentics.mesh.parameter.impl.ImageManipulationParameters.WIDTH_QUERY_PARAM_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map.Entry;

import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.test.junit.Assert;
import com.gentics.mesh.util.HttpQueryUtils;

import io.vertx.core.MultiMap;

public class ImageManipulationParametersTest {

	public ActionContext getActionContext(String query) {
		MultiMap params = MultiMap.caseInsensitiveMultiMap();

		for (Entry<String, String> entry : HttpQueryUtils.splitQuery(query).entrySet()) {
			params.add(entry.getKey(), entry.getValue());
		}
		ActionContext mock = Mockito.mock(ActionContext.class);
		Mockito.when(mock.getParameters()).thenReturn(params);

		Mockito.when(mock.getParameter(Mockito.anyString())).thenAnswer(answer -> {
			String key = (String) answer.getArguments()[0];
			return params.get(key);
		});
		return mock;
	}

	@Test
	public void testFromAC() throws Exception {

		ImageManipulationParameters parameter = new ImageManipulationParameters(
				getActionContext(HEIGHT_QUERY_PARAM_KEY + "=112&" + WIDTH_QUERY_PARAM_KEY + "=142"));
		assertEquals(112, parameter.getHeight().intValue());
		assertEquals(142, parameter.getWidth().intValue());
		assertTrue(parameter.isSet());

		ImageManipulationParameters param = new ImageManipulationParameters();
		ImageManipulationParameters paramsFromQuery = new ImageManipulationParameters(getActionContext(param.getQueryParameters()));
		assertEquals(param.getCroph(), paramsFromQuery.getCroph());
		assertEquals(param.getCropw(), paramsFromQuery.getCropw());
		assertEquals(param.getStartx(), paramsFromQuery.getStartx());
		assertEquals(param.getStarty(), paramsFromQuery.getStarty());
		assertEquals(param.getWidth(), paramsFromQuery.getWidth());
		assertEquals(param.getHeight(), paramsFromQuery.getHeight());
		assertFalse(param.isSet());
		assertFalse(paramsFromQuery.isSet());

		param = new ImageManipulationParameters();
		param.setCroph(100);
		param.setCropw(101);
		param.setWidth(103);
		param.setHeight(104);
		param.setStartx(105);
		param.setStarty(106);
		assertTrue(param.isSet());
		paramsFromQuery = new ImageManipulationParameters(getActionContext(param.getQueryParameters()));
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
		ImageManipulationParameters request = new ImageManipulationParameters();
		request.validate();

		try {
			request = new ImageManipulationParameters();
			request.setWidth(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.WIDTH_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParameters();
			request.setHeight(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.HEIGHT_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParameters();
			request.setCroph(0);
			request.setCropw(10);
			request.setStartx(0);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.CROP_HEIGHT_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParameters();
			request.setCroph(10);
			request.setCropw(0);
			request.setStartx(0);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.CROP_WIDTH_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParameters();
			request.setCroph(10);
			request.setCropw(10);
			request.setStartx(-1);
			request.setStarty(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParameters.CROP_X_QUERY_PARAM_KEY, "-1");
		}

		try {
			request = new ImageManipulationParameters();
			request.setCroph(10);
			request.setCropw(10);
			request.setStartx(0);
			request.setStarty(-1);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParameters.CROP_Y_QUERY_PARAM_KEY, "-1");
		}

		request = new ImageManipulationParameters();
		request.setCroph(1);
		request.setCropw(1);
		request.setStartx(0);
		request.setStarty(0);
		request.validate();

	}

	@Test
	public void testValidateCropBounds() throws Exception {
		try {
			ImageManipulationParameters request = new ImageManipulationParameters();
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
		ImageManipulationParameters request = new ImageManipulationParameters();
		request.setStartx(10);
		request.setStarty(10);
		request.setCroph(1);
		request.setCropw(1);
		request.validateCropBounds(11, 11);
	}

	@Test
	public void testCacheKey() {
		String cacheKey = new ImageManipulationParameters().getCacheKey();
		assertEquals("", cacheKey);

		cacheKey = new ImageManipulationParameters().setWidth(100).setHeight(200).getCacheKey();
		assertEquals("rw100rh200", cacheKey);

		cacheKey = new ImageManipulationParameters().setWidth(100).setHeight(200).setCroph(20).setCropw(21).setStartx(10).setStarty(22).getCacheKey();
		assertEquals("cx10cy22cw21ch20rw100rh200", cacheKey);
	}

}
