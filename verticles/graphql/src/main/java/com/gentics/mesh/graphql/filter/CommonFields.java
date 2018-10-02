package com.gentics.mesh.graphql.filter;

import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.UserTrackingVertex;
import com.gentics.mesh.graphdb.model.MeshElement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Common fields that can be composed in various types
 */
public final class CommonFields {
	private CommonFields() {}

	/**
	 * Filters by uuid
	 */
	public static <T extends MeshElement> FilterField<T, Map<String, ?>> uuidFilter() {
		return new MappedFilter<>("uuid", "Filters by uuid", StringFilter.filter(), MeshElement::getUuid);
	}

	/**
	 * Filters by name
	 */
	public static <T extends NamedElement> FilterField<T, Map<String, ?>> nameFilter() {
		return new MappedFilter<>("name", "Filters by name", StringFilter.filter(), NamedElement::getName);
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 */
	public static <T extends UserTrackingVertex> List<FilterField<T, Map<String, ?>>> userTrackingFilter() {
		return userTrackingFilter(UserFilter.filter());
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 * @param userFilter The user filter to use for creator and editor
	 */
	public static <T extends UserTrackingVertex> List<FilterField<T, Map<String, ?>>> userTrackingFilter(UserFilter userFilter) {
		return Arrays.asList(
			new MappedFilter<>("created", "Filters by creation timestamp", DateFilter.filter(), UserTrackingVertex::getCreationTimestamp),
			new MappedFilter<>("edited", "Filters by update timestamp", DateFilter.filter(), UserTrackingVertex::getLastEditedTimestamp),
			new MappedFilter<>("creator", "Filters by creator", userFilter, UserTrackingVertex::getCreator),
			new MappedFilter<>("editor", "Filters by editor", userFilter, UserTrackingVertex::getEditor)
		);
	}
}
