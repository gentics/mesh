package com.gentics.mesh.core.field.html;

import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

/**
 * Test helper for HTML list fields.
 */
public interface HtmlListFieldHelper {

	public static final String TEXT1 = "<i>one</i>";

	public static final String TEXT2 = "<b>two</b>";

	public static final String TEXT3 = "<u>three</u>";

	public static final DataProvider FILLTEXT = (container, name) -> {
		HibHtmlFieldList field = container.createHTMLList(name);
		field.createHTML(TEXT1);
		field.createHTML(TEXT2);
		field.createHTML(TEXT3);
	};

	public static final DataProvider FILLNUMBERS = (container, name) -> {
		HibHtmlFieldList field = container.createHTMLList(name);
		field.createHTML("1");
		field.createHTML("0");
	};

	public static final DataProvider CREATE_EMPTY = (container, name) -> container.createHTMLList(name);

	public static final DataProvider FILLTRUEFALSE = (container, name) -> {
		HibHtmlFieldList field = container.createHTMLList(name);
		field.createHTML("true");
		field.createHTML("false");
	};

	public static final FieldFetcher FETCH = (container, name) -> container.getHTMLList(name);

}
