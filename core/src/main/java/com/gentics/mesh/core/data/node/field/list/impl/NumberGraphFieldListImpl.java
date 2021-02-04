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
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

/**
 * @see NumberGraphFieldList
 */
public class NumberGraphFieldListImpl extends AbstractBasicGraphFieldList<HibNumberField, NumberFieldListImpl, Number>
	implements NumberGraphFieldList {

	public static FieldTransformer<NumberFieldListImpl> NUMBER_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
		parentNode) -> {
		HibNumberFieldList numberFieldList = container.getNumberList(fieldKey);
		if (numberFieldList == null) {
			return null;
		} else {
			return numberFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	public static FieldUpdater NUMBER_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NumberFieldListImpl numberList = fieldMap.getNumberFieldList(fieldKey);

		HibNumberFieldList graphNumberFieldList = container.getNumberList(fieldKey);
		boolean isNumberListFieldSetToNull = fieldMap.hasField(fieldKey) && numberList == null;
		HibField.failOnDeletionOfRequiredField(graphNumberFieldList, isNumberListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = numberList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphNumberFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isNumberListFieldSetToNull && graphNumberFieldList != null) {
			graphNumberFieldList.removeField(container);
			return;
		}

		// Rest model is null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphNumberFieldList = container.createNumberList(fieldKey);

		// Handle Update
		graphNumberFieldList.removeAll();
		for (Number item : numberList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
			graphNumberFieldList.createNumber(item);
		}

	};

	public static FieldGetter NUMBER_LIST_GETTER = (container, fieldSchema) -> {
		return container.getNumberList(fieldSchema.getName());
	};

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NumberGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibNumberField createNumber(Number number) {
		HibNumberField field = createField();
		field.setNumber(number);
		return field;
	}

	@Override
	public HibNumberField getNumber(int index) {
		return getField(index);
	}

	@Override
	protected NumberGraphField createField(String key) {
		return new NumberGraphFieldImpl(key, this);
	}

	@Override
	public Class<? extends NumberGraphField> getListType() {
		return NumberGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext context) {
		getElement().remove();
	}

	@Override
	public NumberFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		NumberFieldListImpl restModel = new NumberFieldListImpl();
		for (HibNumberField item : getList()) {
			restModel.add(item.getNumber());
		}
		return restModel;
	}

	@Override
	public List<Number> getValues() {
		return getList().stream().map(HibNumberField::getNumber).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NumberFieldListImpl) {
			NumberFieldListImpl restField = (NumberFieldListImpl) obj;
			List<Number> restList = restField.getItems();
			List<? extends HibNumberField> graphList = getList();
			List<Number> graphStringList = graphList.stream().map(e -> e.getNumber()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return super.equals(obj);
	}
}
