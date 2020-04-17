package com.gentics.mesh.query.impl;

import static com.gentics.mesh.parameter.ImageManipulationParameters.HEIGHT_QUERY_PARAM_KEY;
import static com.gentics.mesh.parameter.ImageManipulationParameters.WIDTH_QUERY_PARAM_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map.Entry;

import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.test.Assert;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.util.HttpQueryUtils;
import com.gentics.mesh.util.NumberUtils;
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

		ImageManipulationParametersImpl parameter = new ImageManipulationParametersImpl(getActionContext(HEIGHT_QUERY_PARAM_KEY + "=112&"
			+ WIDTH_QUERY_PARAM_KEY + "=142"));
		assertEquals(112, NumberUtils.toInt(parameter.getHeight(), 0));
		assertEquals(142, NumberUtils.toInt(parameter.getWidth(), 0));

		ImageManipulationParametersImpl param = new ImageManipulationParametersImpl();
		ImageManipulationParametersImpl paramsFromQuery = new ImageManipulationParametersImpl(getActionContext(param.getQueryParameters()));
		assertNull("No rectangular was specified.", param.getRect());
		assertEquals(param.getWidth(), paramsFromQuery.getWidth());
		assertEquals(param.getHeight(), paramsFromQuery.getHeight());

		param = new ImageManipulationParametersImpl();
		param.setRect(105, 106, 100, 101);
		param.setWidth(103);
		param.setHeight(104);
		paramsFromQuery = new ImageManipulationParametersImpl(getActionContext(param.getQueryParameters()));
		assertEquals(param.getRect(), paramsFromQuery.getRect());
		assertEquals(param.getWidth(), paramsFromQuery.getWidth());
		assertEquals(param.getHeight(), paramsFromQuery.getHeight());
	}

	@Test
	public void testCropMode() {
		ImageManipulationParametersImpl request = new ImageManipulationParametersImpl();
		for (CropMode mode : CropMode.values()) {
			request.setCropMode(mode);
			request.validate();
		}

		try {
			request = new ImageManipulationParametersImpl();
			request.setCropMode("blub");
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_invalid", ImageManipulationParameters.CROP_MODE_QUERY_PARAM_KEY, "blub");
		}

	}

	@Test
	public void testSizeValidation() {
		ImageManipulationParametersImpl request = new ImageManipulationParametersImpl();
		request.validate();

		try {
			request = new ImageManipulationParametersImpl();
			request.setWidth(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.WIDTH_QUERY_PARAM_KEY, "0");
		}

		try {
			request = new ImageManipulationParametersImpl();
			request.setHeight(0);
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.HEIGHT_QUERY_PARAM_KEY, "0");
		}

	}

	@Test
	public void testFocalPointValidation() {

		try {
			ImageManipulationParametersImpl request = new ImageManipulationParametersImpl(getActionContext(
				ImageManipulationParameters.FOCAL_POINT_X_QUERY_PARAM_KEY + "=0.1"));
			request.validate();
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_incomplete_focalpoint_parameters",
				ImageManipulationParameters.FOCAL_POINT_Y_QUERY_PARAM_KEY);
		}

		ImageManipulationParametersImpl request = new ImageManipulationParametersImpl();
		request.setFocalPoint(new FocalPoint(0.1f, 0.2f));
		request.validate();
	}

	@Test
	public void testValidateCropBounds() throws Exception {
		try {
			ImageManipulationParametersImpl request = new ImageManipulationParametersImpl();
			request.setRect(10, 10, 1, 1);
			request.getRect().validateCropBounds(10, 10);
			fail("The validation should fail but it did not.");
		} catch (GenericRestException e) {
			Assert.assertException(e, BAD_REQUEST, "image_error_crop_out_of_bounds", "10", "10");
		}

		// Exact crop captures the exact bounds of the source image
		ImageManipulationParametersImpl request = new ImageManipulationParametersImpl();
		request.setRect(10, 10, 1, 1);
		request.getRect().validateCropBounds(11, 11);
	}

	@Test
	public void testCacheKey() {
		String cacheKey = new ImageManipulationParametersImpl().getCacheKey();
		assertEquals("resizeSMARTfp0.5-0.5", cacheKey);

		cacheKey = new ImageManipulationParametersImpl().setResizeMode(ResizeMode.FORCE).getCacheKey();
		assertEquals("resizeFORCEfp0.5-0.5", cacheKey);

		cacheKey = new ImageManipulationParametersImpl().setResizeMode(ResizeMode.PROP).getCacheKey();
		assertEquals("resizePROPfp0.5-0.5", cacheKey);

		cacheKey = new ImageManipulationParametersImpl().setWidth(100).setHeight(200).getCacheKey();
		assertEquals("resizeSMARTrw100rh200fp0.5-0.5", cacheKey);

		cacheKey = new ImageManipulationParametersImpl().setWidth(100).setHeight(200).setRect(10, 22, 20, 21).getCacheKey();
		assertEquals("rect10,22,21,20resizeSMARTrw100rh200fp0.5-0.5", cacheKey);
	}

}
