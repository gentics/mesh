package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

import rx.Observable;

public interface BooleanGraphFieldList extends ListGraphField<BooleanGraphField, BooleanFieldListImpl, Boolean> {

	String TYPE = "boolean";

	FieldTransformator BOOLEAN_LIST_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		BooleanGraphFieldList booleanFieldList = container.getBooleanList(fieldKey);
		if (booleanFieldList == null) {
			return Observable.just(new BooleanFieldListImpl());
		} else {
			return booleanFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater BOOLEAN_LIST_UPDATER = (container, ac, fieldKey, restField, fieldSchema, schema) -> {
		BooleanGraphFieldList graphBooleanFieldList = container.getBooleanList(fieldKey);
		GraphField.failOnMissingMandatoryField(ac, graphBooleanFieldList, restField, fieldSchema, fieldKey, schema);
		BooleanFieldListImpl booleanList = (BooleanFieldListImpl) restField;

		if (booleanList.getItems().isEmpty()) {
			if (graphBooleanFieldList != null) {
				graphBooleanFieldList.removeField(container);
			}
		} else {
			graphBooleanFieldList = container.createBooleanList(fieldKey);
			for (Boolean item : booleanList.getItems()) {
				graphBooleanFieldList.createBoolean(item);
			}
		}
	};

	/**
	 * Return the boolean graph field at index position.
	 * 
	 * @param index
	 * @return
	 */
	BooleanGraphField getBoolean(int index);

	/**
	 * Create a boolean graph field within the list.
	 * 
	 * @param flag
	 * @return
	 */
	BooleanGraphField createBoolean(Boolean flag);
}
