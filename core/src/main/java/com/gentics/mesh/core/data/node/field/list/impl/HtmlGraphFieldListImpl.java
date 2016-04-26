package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

import rx.Observable;

/**
 * @see HtmlGraphFieldList
 */
public class HtmlGraphFieldListImpl extends AbstractBasicGraphFieldList<HtmlGraphField, HtmlFieldListImpl, String> implements HtmlGraphFieldList {

	@Override
	public HtmlGraphField createHTML(String html) {
		HtmlGraphField field = createField();
		field.setHtml(html);
		return field;
	}

	@Override
	protected HtmlGraphField createField(String key) {
		return new HtmlGraphFieldImpl(key, getImpl());
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
	public void delete() {
		getElement().remove();
	}

	@Override
	public Observable<HtmlFieldListImpl> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		HtmlFieldListImpl restModel = new HtmlFieldListImpl();
		for (HtmlGraphField item : getList()) {
			restModel.add(item.getHTML());
		}
		return Observable.just(restModel);
	}

	@Override
	public List<String> getValues() {
		return getList().stream().map(HtmlGraphField::getHTML).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HtmlFieldListImpl) {
			HtmlFieldListImpl restField= (HtmlFieldListImpl)obj;
			List<String> restList = restField.getItems();
			List<? extends HtmlGraphField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> e.getHTML()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return super.equals(obj);
	}
}
