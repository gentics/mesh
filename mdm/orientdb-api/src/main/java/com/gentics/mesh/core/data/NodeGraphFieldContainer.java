package com.gentics.mesh.core.data;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;

/**
 * A node field container is an aggregation node that holds localized fields (e.g.: StringField, NodeField...)
 */
public interface NodeGraphFieldContainer extends HibNodeFieldContainer, GraphFieldContainer, EditorTrackingVertex, GraphDBBucketableElement {

	/**
	 * Retrieve a conflicting edge for the given segment info, branch uuid and type, or null if there's no conflicting
	 * edge
	 * @param segmentInfo
	 * @param branchUuid
	 * @param type
	 * @param edge
	 * @return the conflicting edge, or null if no conflicting edge exists
	 */
	HibNodeFieldContainerEdge getConflictingEdgeOfWebrootPath(String segmentInfo, String branchUuid, ContainerType type, GraphFieldContainerEdge edge);

	/**
	 * Retrieve a conflicting edge for the given urlFieldValue, branch uuid and type, or null if there's no conflicting
	 * edge
	 * @param edge
	 * @param urlFieldValue
	 * @param branchUuid
	 * @param type
	 * @return the conflicting edge, or null if no conflicting edge exists
	 */
	HibNodeFieldContainerEdge getConflictingEdgeOfWebrootField(GraphFieldContainerEdge edge, String urlFieldValue, String branchUuid, ContainerType type);

	/**
	 * Return an iterator over the edges for the given type and branch.
	 * 
	 * @param type
	 * @param branchUuid
	 * @return
	 */
	Iterator<GraphFieldContainerEdge> getContainerEdge(ContainerType type, String branchUuid);

	/**
	 * A container is purgeable when it is not being utilized as draft, published or initial version in any branch.
	 *
	 * @return
	 */
	boolean isPurgeable();

	/**
	 * Determine the display field value by checking the schema and the referenced field and store it as a property.
	 */
	void updateDisplayFieldValue();

	/**
	 * Update the current segment field and increment any found postfix number.
	 */
	void postfixSegmentFieldValue();

	/**
	 * Get all micronode fields that have a micronode using the given microschema container version.
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode fields
	 */
	List<HibMicronodeField> getMicronodeFields(HibMicroschemaVersion version);

	/**
	 * Get all micronode list fields that have at least one micronode using the given microschema container version.
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode list fields
	 */
	Result<HibMicronodeFieldList> getMicronodeListFields(HibMicroschemaVersion version);

	/**
	 * Check whether this field container has the given type for any branch.
	 * 
	 * @param type
	 * @return true if it matches the type, false if not
	 */
	boolean isType(ContainerType type);

	/**
	 * Check whether this field container has the given type in the given branch.
	 * 
	 * @param type
	 * @param branchUuid
	 * @return true if it matches the type, false if not
	 */
	boolean isType(ContainerType type, String branchUuid);

	/**
	 * Get the branch Uuids for which this container is the container of given type.
	 * 
	 * @param type
	 *            type
	 * @return set of branch Uuids (may be empty, but never null)
	 */
	Set<String> getBranches(ContainerType type);

	/**
	 * Get the version of the schema of this container.
	 */
	HibSchemaVersion getSchemaContainerVersion();

	/**
	 * Delete the field container. This will also delete linked elements like lists. If the container has a "next" container, that container will be deleted as
	 * well.
	 * 
	 * @param bac
	 */
	void delete(BulkActionContext bac);

	/**
	 * Delete the field container. This will also delete linked elements like lists.
	 * 
	 * @param bac
	 * @param deleteNext
	 *            true to also delete all "next" containers, false to only delete this container
	 */
	void delete(BulkActionContext bac, boolean deleteNext);

	/**
	 * "Delete" the field container from the branch. This will not actually delete the container itself, but will remove DRAFT and PUBLISHED edges
	 *
	 * @param branch
	 * @param bac
	 */
	void deleteFromBranch(HibBranch branch, BulkActionContext bac);

	@Override
	default boolean isValid() {
		return getElement() != null;
	}
}
