package com.gentics.mesh.core.field.html;

import com.gentics.mesh.core.field.DataProvider;

import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.test.util.TestUtils;

/**
 * Test helper for HTML fields.
 */
public interface HtmlFieldTestHelper {
	public static final DataProvider FILLLONGTEXT = (container, name) -> container.createHTML(name).setHtml(TestUtils.getRandomHash(40000));
	public static final DataProvider FILLTEXT = (container, name) -> container.createHTML(name).setHtml("<b>HTML</b> content");
	public static final DataProvider FILLTRUE = (container, name) -> container.createHTML(name).setHtml("true");
	public static final DataProvider FILLFALSE = (container, name) -> container.createHTML(name).setHtml("false");
	public static final DataProvider FILL0 = (container, name) -> container.createHTML(name).setHtml("0");
	public static final DataProvider FILL1 = (container, name) -> container.createHTML(name).setHtml("1");
	public static final DataProvider FILLNULL = (container, name) -> container.createHTML(name).setHtml(null);
	public static final DataProvider CREATE_EMPTY = (container, name) -> container.createHTML(name);
	public static final FieldFetcher FETCH = (container, name) -> container.getHtml(name);
}
