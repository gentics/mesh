package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.CompareUtils;

/**
 * @see HtmlGraphFieldList
 */
public class HtmlGraphFieldListImpl extends AbstractBasicGraphFieldList<HtmlGraphField, HtmlFieldListImpl, String> implements HtmlGraphFieldList {

	public static FieldTransformer<HtmlFieldListImpl> HTML_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
			parentNode) -> {
		HtmlGraphFieldList htmlFieldList = container.getHTMLList(fieldKey);
		if (htmlFieldList == null) {
			return null;
		} else {
			return htmlFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	public static FieldUpdater HTML_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HtmlGraphFieldList graphHtmlFieldList = container.getHTMLList(fieldKey);
		HtmlFieldListImpl htmlList = fieldMap.getHtmlFieldList(fieldKey);
		boolean isHtmlListFieldSetToNull = fieldMap.hasField(fieldKey) && htmlList == null;
		GraphField.failOnDeletionOfRequiredField(graphHtmlFieldList, isHtmlListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = htmlList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			GraphField.failOnMissingRequiredField(graphHtmlFieldList, htmlList == null, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isHtmlListFieldSetToNull && graphHtmlFieldList != null) {
			graphHtmlFieldList.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphHtmlFieldList = container.createHTMLList(fieldKey);

		// Add items from rest model
		for (String item : htmlList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
			graphHtmlFieldList.createHTML(item);
		}
	};

	public static FieldGetter HTML_LIST_GETTER = (container, fieldSchema) -> {
		return container.getHTMLList(fieldSchema.getName());
	};

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(HtmlGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HtmlGraphField createHTML(String html) {
		HtmlGraphField field = createField();
		field.setHtml(html);
		return field;
	}

	@Override
	protected HtmlGraphField createField(String key) {
		return new HtmlGraphFieldImpl(key, this);
	}

	@Override
	public HtmlGraphField getHTML(int index) {
		return getField(index);
	}

	@Override
	public Class<? extends HtmlGraphField> getListType() {
		return HtmlGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

	@Override
	public HtmlFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		HtmlFieldListImpl restModel = new HtmlFieldListImpl();
		for (HtmlGraphField item : getList()) {
			restModel.add(item.getHTML());
		}
		return restModel;
	}

	@Override
	public List<String> getValues() {
		return getList().stream().map(HtmlGraphField::getHTML).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HtmlFieldListImpl) {
			HtmlFieldListImpl restField = (HtmlFieldListImpl) obj;
			List<String> restList = restField.getItems();
			List<? extends HtmlGraphField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> e.getHTML()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return super.equals(obj);
	}
}
