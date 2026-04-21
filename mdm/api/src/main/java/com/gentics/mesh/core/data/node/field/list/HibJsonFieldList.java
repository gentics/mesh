package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.HibJsonField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.JsonContent;
import com.gentics.mesh.core.rest.node.field.list.impl.JsonFieldListImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.CompareUtils;

public interface HibJsonFieldList extends HibMicroschemaListableField, HibListField<HibJsonField, JsonFieldListImpl, JsonContent> {

	String TYPE = "json";

	/**
	 * Add another sql field to the list of sql fields.
	 * 
	 * @param json
	 *            Json to be set for the new field
	 * @return
	 */
	HibJsonField createJson(JsonContent json);

	/**
	 * Create an ordered list of json fields from values, adding all to the list.
	 * 
	 * @param jsons
	 */
	default void createJsons(List<JsonContent> jsons) {
		jsons.stream().forEach(this::createJson);
	}

	/**
	 * Return the json field at the given index of the list.
	 * 
	 * @param index
	 * @return
	 */
	HibJsonField getJson(int index);

	@Override
	default JsonFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		JsonFieldListImpl restModel = new JsonFieldListImpl();
		for (HibJsonField item : getList()) {
			restModel.add(item.getJson());
		}
		return restModel;
	}

	@Override
	default List<JsonContent> getValues() {
		return getList().stream().map(HibJsonField::getJson).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof JsonFieldListImpl) {
			JsonFieldListImpl restField = (JsonFieldListImpl) obj;
			List<JsonContent> restList = restField.getItems();
			List<? extends HibJsonField> sqlList = getList();
			List<JsonContent> valueList = sqlList.stream().map(e -> e.getJson()).collect(Collectors.toList());
			return CompareUtils.equals(restList, valueList, Optional.of((a, b) -> JsonUtil.COMPARATOR.compare(a, b) == 0));
		}
		return HibListField.super.listEquals(obj);
	}
}
