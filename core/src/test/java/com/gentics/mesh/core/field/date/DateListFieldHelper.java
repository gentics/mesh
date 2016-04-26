package com.gentics.mesh.core.field.date;

import java.util.Date;

import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface DateListFieldHelper {
	public static final long DATEVALUE = new Date().getTime();

	public static final long OTHERDATEVALUE = 4711L;

	public static final DataProvider FILL = (container, name) -> {
		DateGraphFieldList field = container.createDateList(name);
		field.createDate(DATEVALUE);
		field.createDate(OTHERDATEVALUE);
	};

	public static final DataProvider CREATE_EMPTY = (container, name) -> container.createDateList(name);

	public static final FieldFetcher FETCH = (container, name) -> container.getDateList(name);

}
