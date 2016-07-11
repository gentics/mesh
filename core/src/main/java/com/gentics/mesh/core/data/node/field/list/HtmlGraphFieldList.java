package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

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
			return Observable.just(null);
		} else {
			return htmlFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater HTML_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HtmlGraphFieldList graphHtmlFieldList = container.getHTMLList(fieldKey);
		HtmlFieldListImpl htmlList = fieldMap.getHtmlFieldList(fieldKey);
		boolean isHtmlListFieldSetToNull = fieldMap.hasField(fieldKey) && htmlList == null;
		GraphField.failOnDeletionOfRequiredField(graphHtmlFieldList, isHtmlListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = htmlList == null;
		GraphField.failOnMissingRequiredField(graphHtmlFieldList, htmlList == null, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isHtmlListFieldSetToNull && graphHtmlFieldList != null) {
			graphHtmlFieldList.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list. 
		// This will effectively unlink the old list and create a new one. 
		// Otherwise the list which is linked to old versions would be updated. 
		graphHtmlFieldList = container.createHTMLList(fieldKey);

		// Add items from rest model
		for (String item : htmlList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
			graphHtmlFieldList.createHTML(item);
		}
	};

	FieldGetter HTML_LIST_GETTER = (container, fieldSchema) -> {
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
