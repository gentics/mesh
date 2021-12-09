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
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

/**
 * @see BooleanGraphFieldList
 */
public class BooleanGraphFieldListImpl extends AbstractBasicGraphFieldList<HibBooleanField, BooleanFieldListImpl, Boolean>
	implements BooleanGraphFieldList {

	public static FieldTransformer<BooleanFieldListImpl> BOOLEAN_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
		parentNode) -> {
		HibBooleanFieldList booleanFieldList = container.getBooleanList(fieldKey);
		if (booleanFieldList == null) {
			return null;
		} else {
			return booleanFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	public static FieldUpdater BOOLEAN_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibBooleanFieldList graphBooleanFieldList = container.getBooleanList(fieldKey);
		BooleanFieldListImpl booleanList = fieldMap.getBooleanFieldList(fieldKey);
		boolean isBooleanListFieldSetToNull = fieldMap.hasField(fieldKey) && booleanList == null;
		HibField.failOnDeletionOfRequiredField(graphBooleanFieldList, isBooleanListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = booleanList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphBooleanFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isBooleanListFieldSetToNull && graphBooleanFieldList != null) {
			container.removeField(graphBooleanFieldList);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphBooleanFieldList = container.createBooleanList(fieldKey);

		// Handle Update
		// Remove all and add the listed items
		graphBooleanFieldList.removeAll();
		for (Boolean item : booleanList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
			graphBooleanFieldList.createBoolean(item);
		}

	};

	public static FieldGetter BOOLEAN_LIST_GETTER = (container, fieldSchema) -> {
		return container.getBooleanList(fieldSchema.getName());
	};

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BooleanGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibBooleanField getBoolean(int index) {
		return getField(index);
	}

	@Override
	public HibBooleanField createBoolean(Boolean flag) {
		HibBooleanField field = createField();
		field.setBoolean(flag);
		return field;
	}

	@Override
	protected BooleanGraphField createField(String key) {
		return new BooleanGraphFieldImpl(key, this);
	}

	@Override
	public Class<? extends BooleanGraphField> getListType() {
		return BooleanGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

	@Override
	public BooleanFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		BooleanFieldListImpl restModel = new BooleanFieldListImpl();
		for (HibBooleanField item : getList()) {
			restModel.add(item.getBoolean());
		}
		return restModel;
	}

	@Override
	public List<Boolean> getValues() {
		return getList().stream().map(HibBooleanField::getBoolean).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BooleanFieldListImpl) {
			BooleanFieldListImpl restField = (BooleanFieldListImpl) obj;
			List<Boolean> restList = restField.getItems();
			List<? extends HibBooleanField> graphList = getList();
			List<Boolean> graphStringList = graphList.stream().map(e -> e.getBoolean()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return super.equals(obj);
	}
}
