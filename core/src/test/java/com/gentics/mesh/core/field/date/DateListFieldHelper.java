package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.util.Date;

import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface DateListFieldHelper {

	public static final long DATEVALUE = fromISO8601(toISO8601(new Date().getTime()));

	public static final long OTHERDATEVALUE = fromISO8601(toISO8601(4711000));

	public static final DataProvider FILL = (container, name) -> {
		DateGraphFieldList field = container.createDateList(name);
		field.createDate(DATEVALUE);
		field.createDate(OTHERDATEVALUE);
	};

	public static final DataProvider CREATE_EMPTY = (container, name) -> container.createDateList(name);

	public static final FieldFetcher FETCH = (container, name) -> container.getDateList(name);

}
