package com.gentics.mesh.core.data.node.field;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.handler.ActionContext;

public interface HibHtmlField extends HibListableField, HibBasicField<HtmlField> {

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
	default HibField cloneTo(HibFieldContainer container) {
		HibHtmlField clone = container.createHTML(getFieldKey());
		clone.setHtml(getHTML());
		return clone;
	}

	@Override
	default HtmlField transformToRest(ActionContext ac) {
		HtmlFieldImpl htmlField = new HtmlFieldImpl();
		String html = getHTML();
		// TODO really empty string for unset field value?!
		htmlField.setHTML(html == null ? StringUtils.EMPTY : html);
		return htmlField;
	}

	default boolean htmlEquals(Object obj) {
		if (obj instanceof HibHtmlField) {
			String htmlA = getHTML();
			String htmlB = ((HibHtmlField) obj).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		if (obj instanceof HtmlField) {
			String htmlA = getHTML();
			String htmlB = ((HtmlField) obj).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		return false;
	}
}
