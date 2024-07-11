package com.gentics.mesh.core.data.node.field;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.rest.node.field.HtmlFieldModel;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.handler.ActionContext;

public interface HtmlField extends ListableField, BasicField<HtmlFieldModel> {

	/**
	 * Set the HTML field value for the field.
	 * 
	 * @param html
	 */
	void setHtml(String html);

	/**
	 * Return the HTML field value for the field.
	 * 
	 * @return
	 */
	String getHTML();

	@Override
	default Field cloneTo(FieldContainer container) {
		HtmlField clone = container.createHTML(getFieldKey());
		clone.setHtml(getHTML());
		return clone;
	}

	@Override
	default HtmlFieldModel transformToRest(ActionContext ac) {
		HtmlFieldImpl htmlField = new HtmlFieldImpl();
		String html = getHTML();
		// TODO really empty string for unset field value?!
		htmlField.setHTML(html == null ? StringUtils.EMPTY : html);
		return htmlField;
	}

	default boolean htmlEquals(Object obj) {
		if (obj instanceof HtmlField) {
			String htmlA = getHTML();
			String htmlB = ((HtmlField) obj).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		if (obj instanceof HtmlFieldModel) {
			String htmlA = getHTML();
			String htmlB = ((HtmlFieldModel) obj).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		return false;
	}
}
