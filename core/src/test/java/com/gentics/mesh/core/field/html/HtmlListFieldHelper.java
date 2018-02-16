package com.gentics.mesh.core.field.html;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface HtmlListFieldHelper {

	String TEXT1 = "<i>one</i>";

	String TEXT2 = "<b>two</b>";

	String TEXT3 = "<u>three</u>";

	DataProvider FILLTEXT = (container, name) -> {
		HtmlGraphFieldList field = container.createHTMLList(name);
		field.createHTML(TEXT1);
		field.createHTML(TEXT2);
		field.createHTML(TEXT3);
	};

	DataProvider FILLNUMBERS = (container, name) -> {
		HtmlGraphFieldList field = container.createHTMLList(name);
		field.createHTML("1");
		field.createHTML("0");
	};

	DataProvider CREATE_EMPTY = GraphFieldContainer::createHTMLList;

	DataProvider FILLTRUEFALSE = (container, name) -> {
		HtmlGraphFieldList field = container.createHTMLList(name);
		field.createHTML("true");
		field.createHTML("false");
	};

	FieldFetcher FETCH = GraphFieldContainer::getHTMLList;

}
