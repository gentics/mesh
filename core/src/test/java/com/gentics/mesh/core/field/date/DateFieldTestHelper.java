package com.gentics.mesh.core.field.date;

import java.util.Date;

import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface DateFieldTestHelper {

	public static final long DATEVALUE = new Date().getTime();

	public static final DataProvider FILL = (container, name) -> container.createDate(name).setDate(DATEVALUE);
	public static final DataProvider CREATE_EMPTY = (container, name) -> container.createDate(name).setDate(null);
	public static final FieldFetcher FETCH = (container, name) -> container.getDate(name);
}
