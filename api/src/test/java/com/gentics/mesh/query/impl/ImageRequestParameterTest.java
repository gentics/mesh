package com.gentics.mesh.query.impl;

import static com.gentics.mesh.query.impl.ImageRequestParameter.HEIGHT_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.ImageRequestParameter.WIDTH_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.ImageRequestParameter.fromQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
