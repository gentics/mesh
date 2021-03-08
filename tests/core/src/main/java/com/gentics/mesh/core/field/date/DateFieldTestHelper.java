package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.util.Date;

import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface DateFieldTestHelper {

	// Clap the date using our ISO-8601 format
	public static final long DATEVALUE = fromISO8601(toISO8601(new Date().getTime()));

	public static final DataProvider FILL = (container, name) -> container.createDate(name).setDate(DATEVALUE);
	public static final DataProvider CREATE_EMPTY = (container, name) -> container.createDate(name).setDate(null);
	public static final FieldFetcher FETCH = (container, name) -> container.getDate(name);
}
