package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.FieldTransformator;
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
