package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

public interface HibHtmlFieldList extends HibMicroschemaListableField, HibListField<HibHtmlField, HtmlFieldListImpl, String> {

	String TYPE = "html";

	/**
	 * Create a new html graph field.
	 * 
	 * @param html
	 * @return
	 */
	HibHtmlField createHTML(String html);

	/**
	 * Return the html graph field at the given index position.
	 * 
	 * @param index
	 * @return
	 */
	HibHtmlField getHTML(int index);

	@Override
	default HtmlFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		HtmlFieldListImpl restModel = new HtmlFieldListImpl();
		for (HibHtmlField item : getList()) {
			restModel.add(item.getHTML());
		}
		return restModel;
	}

	@Override
	default List<String> getValues() {
		return getList().stream().map(HibHtmlField::getHTML).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof HtmlFieldListImpl) {
			HtmlFieldListImpl restField = (HtmlFieldListImpl) obj;
			List<String> restList = restField.getItems();
			List<? extends HibHtmlField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> e.getHTML()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return HibListField.super.listEquals(obj);
	}
}
