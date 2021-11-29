package com.gentics.mesh.graphql.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.user.HibUserTracking;

/**
 * Common fields that can be composed in various types
 */
public final class CommonFields {
	private CommonFields() {
	}

	/**
	 * Filters by uuid
	 * @deprecated Use {@link #hibUuidFilter()} instead.
	 */
	@Deprecated
	public static <T extends HibElement> FilterField<T, Map<String, ?>> uuidFilter() {
		return new MappedFilter<>("uuid", "Filters by uuid", StringFilter.filter(), HibElement::getUuid);
	}

	/**
	 * Filters by uuid
	 * 
	 * @param <T>
	 * @return
	 */
	public static <T extends HibBaseElement> FilterField<T, Map<String, ?>> hibUuidFilter() {
		return new MappedFilter<>("uuid", "Filters by uuid", StringFilter.filter(), HibBaseElement::getUuid);
	}

	/**
	 * Filters by name
	 */
	@Deprecated
	public static <T extends HibNamedElement> FilterField<T, Map<String, ?>> nameFilter() {
		return new MappedFilter<>("name", "Filters by name", StringFilter.filter(), HibNamedElement::getName);
	}

	/**
	 * Filters by name
	 */
	public static <T extends HibNamedElement> FilterField<T, Map<String, ?>> hibNameFilter() {
		return new MappedFilter<>("name", "Filters by name", StringFilter.filter(), HibNamedElement::getName);
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 * @deprecated Use {@link #hibUserTrackingFilter()}
	 */
	@Deprecated
	public static <T extends HibUserTracking> List<FilterField<T, Map<String, ?>>> userTrackingFilter() {
		return userTrackingFilter(UserFilter.filter());
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 */
	public static <T extends HibUserTracking> List<FilterField<T, Map<String, ?>>> hibUserTrackingFilter() {
		return hibUserTrackingFilter(UserFilter.filter());
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 * 
	 * @param userFilter
	 *            The user filter to use for creator and editor
	 */
	@Deprecated
	public static <T extends HibUserTracking> List<FilterField<T, Map<String, ?>>> userTrackingFilter(UserFilter userFilter) {
		return Arrays.asList(
			new MappedFilter<>("created", "Filters by creation timestamp", DateFilter.filter(), HibUserTracking::getCreationTimestamp),
			new MappedFilter<>("edited", "Filters by update timestamp", DateFilter.filter(), HibUserTracking::getLastEditedTimestamp),
			new MappedFilter<>("creator", "Filters by creator", userFilter, HibUserTracking::getCreator),
			new MappedFilter<>("editor", "Filters by editor", userFilter, HibUserTracking::getEditor));
	}

	/**
	 * Filters by created time, creator, edited time and editor.
	 * 
	 * @param userFilter
	 *            The user filter to use for creator and editor
	 */
	public static <T extends HibUserTracking> List<FilterField<T, Map<String, ?>>> hibUserTrackingFilter(UserFilter userFilter) {
		return Arrays.asList(
			new MappedFilter<>("created", "Filters by creation timestamp", DateFilter.filter(), HibUserTracking::getCreationTimestamp),
			new MappedFilter<>("edited", "Filters by update timestamp", DateFilter.filter(), HibUserTracking::getLastEditedTimestamp),
			new MappedFilter<>("creator", "Filters by creator", userFilter, HibUserTracking::getCreator),
			new MappedFilter<>("editor", "Filters by editor", userFilter, HibUserTracking::getEditor));
	}
}
