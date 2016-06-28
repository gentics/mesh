package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.parameter.impl.LinkType;

import rx.Observable;

/**
 * The HtmlField Domain Model interface.
 * 
 * A html graph field is a basic node field which can be used to store html string values.
 */
public interface HtmlGraphField extends ListableGraphField, BasicGraphField<HtmlField> {

	FieldTransformator HTML_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		HtmlGraphField graphHtmlField = container.getHtml(fieldKey);
		if (graphHtmlField == null) {
			return Observable.just(new HtmlFieldImpl());
		} else {
			return graphHtmlField.transformToRest(ac).map(model -> {
				// If needed resolve links within the html
				if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
					Project project = ac.getProject();
					if (project == null) {
						project = parentNode.getProject();
					}
					model.setHTML(WebRootLinkReplacer.getInstance().replace(ac.getRelease(null).getUuid(), ContainerType.forVersion(ac.getVersioningParameters().getVersion()),
							model.getHTML(), ac.getNodeParameters().getResolveLinks(), project.getName(), languageTags));
				}
				return model;
			});
		}
	};

	FieldUpdater HTML_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HtmlField htmlField = fieldMap.getHtmlField(fieldKey);
		HtmlGraphField htmlGraphField = container.getHtml(fieldKey);
		boolean isHtmlFieldSetToNull = fieldMap.hasField(fieldKey) && (htmlField == null || htmlField.getHTML() == null);
		GraphField.failOnDeletionOfRequiredField(htmlGraphField, isHtmlFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean isHtmlFieldNull = htmlField == null || htmlField.getHTML() == null;
		GraphField.failOnMissingRequiredField(htmlGraphField, isHtmlFieldNull, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isHtmlFieldSetToNull && htmlGraphField != null) {
			htmlGraphField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (isHtmlFieldNull) {
			return;
		}

		// Handle Update / Create - Create new graph field if no existing one could be found
		if (htmlGraphField == null) {
			container.createHTML(fieldKey).setHtml(htmlField.getHTML());
		} else {
			htmlGraphField.setHtml(htmlField.getHTML());
		}
	};

	FieldGetter HTML_GETTER = (container, fieldSchema) -> {
		return container.getHtml(fieldSchema.getName());
	};

	/**
	 * Set the html field value for the field.
	 * 
	 * @param html
	 */
	void setHtml(String html);

	/**
	 * Return the html field value for the field.
	 * 
	 * @return
	 */
	String getHTML();

}
