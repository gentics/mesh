package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

import rx.Observable;

public interface HtmlGraphFieldList extends ListGraphField<HtmlGraphField, HtmlFieldListImpl, String> {

	String TYPE = "html";
	FieldTransformator HTML_LIST_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		HtmlGraphFieldList htmlFieldList = container.getHTMLList(fieldKey);
		if (htmlFieldList == null) {
			return Observable.just(new HtmlFieldListImpl());
		} else {
			return htmlFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater HTML_LIST_UPDATER = (container, ac, fieldKey, restField, fieldSchema, schema) -> {
		HtmlGraphFieldList graphHtmlFieldList = container.getHTMLList(fieldKey);
		GraphField.failOnMissingMandatoryField(ac, graphHtmlFieldList, restField, fieldSchema, fieldKey, schema);
		HtmlFieldListImpl htmlList = (HtmlFieldListImpl) restField;

		if (htmlList.getItems().isEmpty()) {
			if (graphHtmlFieldList != null) {
				graphHtmlFieldList.removeField(container);
			}
		} else {
			graphHtmlFieldList = container.createHTMLList(fieldKey);
			for (String item : htmlList.getItems()) {
				graphHtmlFieldList.createHTML(item);
			}
		}
	};

	FieldGetter  HTML_LIST_GETTER = (container, fieldSchema) -> {
		return container.getHTMLList(fieldSchema.getName());
	};

	/**
	 * Create a new html graph field.
	 * 
	 * @param html
	 * @return
	 */
	HtmlGraphField createHTML(String html);

	/**
	 * Return the html graph field at the given index position.
	 * 
	 * @param index
	 * @return
	 */
	HtmlGraphField getHTML(int index);

}
