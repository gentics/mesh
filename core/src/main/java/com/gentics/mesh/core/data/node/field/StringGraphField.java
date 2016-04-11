package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;

import rx.Observable;

/**
 * The StringField Domain Model interface.
 */
public interface StringGraphField extends ListableGraphField, BasicGraphField<StringField> {

	FieldTransformator STRING_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		// TODO validate found fields has same type as schema
		// StringGraphField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl(
		// fieldKey, this);
		StringGraphField graphStringField = container.getString(fieldKey);
		if (graphStringField == null) {
			return Observable.just(new StringFieldImpl());
		} else {
			return graphStringField.transformToRest(ac).map(stringField -> {
				if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
					Project project = ac.getProject();
					if (project == null) {
						project = parentNode.getProject();
					}
					stringField.setString(WebRootLinkReplacer.getInstance().replace(stringField.getString(), ac.getResolveLinksType(),
							project.getName(), languageTags));
				}
				return stringField;
			});
		}
	};

	/**
	 * Return the graph string value.
	 * 
	 * @return
	 */
	String getString();

	/**
	 * Set the string graph field value.
	 * 
	 * @param string
	 */
	void setString(String string);

}
