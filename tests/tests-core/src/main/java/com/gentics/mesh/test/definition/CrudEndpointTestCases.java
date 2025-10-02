package com.gentics.mesh.test.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.parameter.SortingParameters;
import com.gentics.mesh.parameter.client.SortingParametersImpl;

public interface CrudEndpointTestCases {

	// Create
	void testCreate() throws Exception;

	void testCreateReadDelete() throws Exception;

	void testCreateWithNoPerm() throws Exception;

	void testCreateWithUuid() throws Exception;

	@Deprecated
	/**
	 * @deprecated Not valid, if dup UUIDs allowed across the entity types
	 * @throws Exception
	 */
	void testCreateWithDuplicateUuid() throws Exception;

	// Read
	void testReadByUUID() throws Exception;

	void testReadByUuidWithRolePerms();

	void testReadByUUIDWithMissingPermission() throws Exception;

	void testReadMultiple() throws Exception;

	void testReadPermittedSorted() throws Exception;

	void testReadPermittedSortedWrongField() throws Exception;

	void testPermissionResponse();

	// Update
	void testUpdate() throws Exception;

	void testUpdateByUUIDWithoutPerm() throws Exception;

	void testUpdateWithBogusUuid() throws GenericRestException, Exception;

	// Delete
	void testDeleteByUUID() throws Exception;

	void testDeleteByUUIDWithNoPermission() throws Exception;

	/**
	 * Verify that sorting works. This will either accept case sensitive or case insensitive ordering
	 * @param <T> type of returned REST Models
	 * @param listFunction function that gets a list of REST Models
	 * @param extractor data extractor
	 * @param sortedAttribute sorted attribute
	 * @param description description
	 */
	default <T> void verifySorting(Function<SortingParameters, ListResponse<T>> listFunction,
			Function<? super T, String> extractor, String sortedAttribute, String description) {
		ListResponse<T> sortedResponse = listFunction
				.apply(new SortingParametersImpl().putSort(sortedAttribute, SortOrder.ASCENDING));
		List<String> sortedData = sortedResponse.getData().stream().map(extractor).collect(Collectors.toList());
		try {
			// assert that elements are sorted by natural order (case sensitive)
			assertThat(sortedData).as(description).isNotEmpty().isSorted();
		} catch (AssertionError e) {
			// if this fails, we assert that elements are sorted case insensitive
			assertThat(sortedData).as(description).isNotEmpty().isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
		}

		sortedResponse = listFunction.apply(new SortingParametersImpl().putSort(sortedAttribute, SortOrder.DESCENDING));
		sortedData = sortedResponse.getData().stream().map(extractor).collect(Collectors.toList());
		try {
			// assert that elements are sorted by reversed natural order (case sensitive)
			assertThat(sortedData).as(description).isNotEmpty().isSortedAccordingTo(Comparator.reverseOrder());
		} catch (AssertionError e) {
			// if this fails, we assert that elements are sorted reversed case insensitive
			assertThat(sortedData).as(description).isNotEmpty().isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER.reversed());
		}
	}
}
