package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.util.Date;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface DateFieldTestHelper {

	// Clap the date using our ISO-8601 format
    long DATEVALUE = fromISO8601(toISO8601(new Date().getTime()));

	DataProvider FILL = (container, name) -> container.createDate(name).setDate(DATEVALUE);
	DataProvider CREATE_EMPTY = (container, name) -> container.createDate(name).setDate(null);
	FieldFetcher FETCH = GraphFieldContainer::getDate;
}
