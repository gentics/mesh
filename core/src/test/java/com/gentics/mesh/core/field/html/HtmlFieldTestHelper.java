package com.gentics.mesh.core.field.html;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.field.DataProvider;

import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.test.util.TestUtils;

public interface HtmlFieldTestHelper {
	DataProvider FILLLONGTEXT = (container, name) -> container.createHTML(name).setHtml(TestUtils.getRandomHash(40000));
	DataProvider FILLTEXT = (container, name) -> container.createHTML(name).setHtml("<b>HTML</b> content");
	DataProvider FILLTRUE = (container, name) -> container.createHTML(name).setHtml("true");
	DataProvider FILLFALSE = (container, name) -> container.createHTML(name).setHtml("false");
	DataProvider FILL0 = (container, name) -> container.createHTML(name).setHtml("0");
	DataProvider FILL1 = (container, name) -> container.createHTML(name).setHtml("1");
	DataProvider FILLNULL = (container, name) -> container.createHTML(name).setHtml(null);
	DataProvider CREATE_EMPTY = GraphFieldContainer::createHTML;
	FieldFetcher FETCH = GraphFieldContainer::getHtml;
}
