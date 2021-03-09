package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

/**
 * @see StringGraphFieldList
 */
public class StringGraphFieldListImpl extends AbstractBasicGraphFieldList<HibStringField, StringFieldListImpl, String>
	implements StringGraphFieldList {

	public static FieldTransformer<StringFieldListImpl> STRING_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
		parentNode) -> {
		HibStringFieldList stringFieldList = container.getStringList(fieldKey);
		if (stringFieldList == null) {
			return null;
		} else {
			return stringFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	public static FieldUpdater STRING_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibStringFieldList graphStringList = container.getStringList(fieldKey);
		StringFieldListImpl stringList = fieldMap.getStringFieldList(fieldKey);
		boolean isStringListFieldSetToNull = fieldMap.hasField(fieldKey) && (stringList == null || stringList.getItems() == null);
		HibField.failOnDeletionOfRequiredField(graphStringList, isStringListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = stringList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphStringList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isStringListFieldSetToNull && graphStringList != null) {
			graphStringList.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphStringList = container.createStringList(fieldKey);

		// Handle Update
		graphStringList.removeAll();
		for (String item : stringList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
			graphStringList.createString(item);
		}
	};

	public static FieldGetter STRING_LIST_GETTER = (container, fieldSchema) -> {
		return container.getStringList(fieldSchema.getName());
	};

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(StringGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibStringField createString(String string) {
		HibStringField field = createField();
		field.setString(string);
		return field;
	}

	@Override
	public HibStringField getString(int index) {
		return getField(index);
	}

	@Override
	protected StringGraphField createField(String key) {
		return new StringGraphFieldImpl(key, this);
	}

	@Override
	public Class<? extends StringGraphField> getListType() {
		return StringGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

	@Override
	public StringFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		StringFieldListImpl restModel = new StringFieldListImpl();
		for (HibStringField item : getList()) {
			restModel.add(item.getString());
		}
		return restModel;
	}

	@Override
	public List<String> getValues() {
		return getList().stream().map(HibStringField::getString).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringFieldListImpl) {
			StringFieldListImpl restField = (StringFieldListImpl) obj;
			List<String> restList = restField.getItems();
			List<? extends HibStringField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> e.getString()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return super.equals(obj);
	}

}
