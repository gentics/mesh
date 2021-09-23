package com.gentics.mesh.core.data.util;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibContent;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.BranchVersionEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchVersionAssignment;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.ElementFrame;

/**
 * Converter which can transform MDM domain model objects to graph domain element.
 */
public final class HibClassConverter {

	private HibClassConverter() {
	}

	/**
	 * Convert the hib element to a generic graph element.
	 * 
	 * @param element
	 * @return
	 */
	public static MeshElement toGraph(HibBaseElement element) {
		return checkAndCast(element, MeshElement.class);
	}

	/**
	 * Convert the tag to a graph element.
	 * 
	 * @param tag
	 * @return
	 */
	public static Tag toGraph(HibTag tag) {
		return checkAndCast(tag, Tag.class);
	}

	/**
	 * Convert the tag to a graph element.
	 * 
	 * @param tag
	 * @return
	 */
	public static Language toGraph(HibLanguage tag) {
		return checkAndCast(tag, Language.class);
	}

	/**
	 * Convert the tagfamily to a graph element.
	 * 
	 * @param tagFamily
	 * @return
	 */
	public static TagFamily toGraph(HibTagFamily tagFamily) {
		return checkAndCast(tagFamily, TagFamily.class);
	}

	/**
	 * Convert the auth user to a graph element.
	 * 
	 * @param user
	 * @return
	 */
	public static User toGraph(MeshAuthUser user) {
		if (user == null) {
			return null;
		}
		return toGraph(user.getDelegate());
	}

	/**
	 * Convert the user to a graph element.
	 * 
	 * @param user
	 * @return
	 */
	public static User toGraph(HibUser user) {
		if (user instanceof MeshAuthUser) {
			return toGraph((MeshAuthUser) user);
		}
		return checkAndCast(user, User.class);
	}

	/**
	 * Convert the branch assignment to a graph element.
	 * 
	 * @param assignment
	 * @return
	 */
	public static BranchVersionEdge toGraph(HibBranchVersionAssignment assignment) {
		return checkAndCast(assignment, BranchVersionEdge.class);
	}

	/**
	 * Convert the field schema element to a graph element.
	 * 
	 * @param <SC>
	 * @param <SCV>
	 * @param <R>
	 * @param <RM>
	 * @param element
	 * @return
	 */
	public static <SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>, R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion> GraphFieldSchemaContainer<R, ?, SC, SCV> toGraphContainer(
		HibFieldSchemaElement<R, RM, SC, SCV> element) {
		return checkAndCast(element, GraphFieldSchemaContainer.class);
	}

	/**
	 * Convert the field schema version element to a graph element.
	 * 
	 * @param <RM>
	 * @param <SC>
	 * @param <SCV>
	 * @param <R>
	 * @param element
	 * @return
	 */
	public static <RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>, R extends FieldSchemaContainer> GraphFieldSchemaContainerVersion<R, RM, ?, SCV, ?> toGraphVersion(
		HibFieldSchemaVersionElement<R, RM, SC, SCV> element) {
		return checkAndCast(element, GraphFieldSchemaContainerVersion.class);
	}

	/**
	 * Convert the group to a graph element.
	 * 
	 * @param group
	 * @return
	 */
	public static Group toGraph(HibGroup group) {
		return checkAndCast(group, Group.class);
	}

	/**
	 * Convert the branch to a graph element.
	 * 
	 * @param branch
	 * @return
	 */
	public static Branch toGraph(HibBranch branch) {
		return checkAndCast(branch, Branch.class);
	}

	/**
	 * Convert the schema to a graph element.
	 * 
	 * @param schema
	 * @return
	 */
	public static Schema toGraph(HibSchema schema) {
		return checkAndCast(schema, Schema.class);
	}

	/**
	 * Convert the version to a graph element.
	 * 
	 * @param version
	 * @return
	 */
	public static SchemaVersion toGraph(HibSchemaVersion version) {
		return checkAndCast(version, SchemaVersion.class);
	}

	/**
	 * Convert the microschema to a graph element.
	 * 
	 * @param schema
	 * @return
	 */
	public static Microschema toGraph(HibMicroschema schema) {
		return checkAndCast(schema, Microschema.class);
	}

	/**
	 * Convert the microschema version to a graph element.
	 * 
	 * @param version
	 * @return
	 */
	public static MicroschemaVersion toGraph(HibMicroschemaVersion version) {
		return checkAndCast(version, MicroschemaVersion.class);
	}

	/**
	 * Convert the project to a graph element.
	 * 
	 * @param project
	 * @return
	 */
	public static Project toGraph(HibProject project) {
		return checkAndCast(project, Project.class);
	}

	/**
	 * Convert the role to a graph element.
	 * 
	 * @param role
	 * @return
	 */
	public static Role toGraph(HibRole role) {
		return checkAndCast(role, Role.class);
	}

	/**
	 * Convert the binary to a graph element.
	 * 
	 * @param binary
	 * @return
	 */
	public static Binary toGraph(HibBinary binary) {
		return checkAndCast(binary, Binary.class);
	}

	/**
	 * Convert the node to a graph element.
	 * 
	 * @param node
	 * @return
	 */
	public static Node toGraph(HibNode node) {
		return checkAndCast(node, Node.class);
	}

	/**
	 * Convert the content to a graph element.
	 * 
	 * @param content
	 * @return
	 */
	public static NodeGraphFieldContainer toGraph(HibContent content) {
		return checkAndCast(content, NodeGraphFieldContainer.class);
	}

	/**
	 * Convert the job to graph element.
	 * 
	 * @param job
	 * @return
	 */
	public static Job toGraph(HibJob job) {
		return checkAndCast(job, Job.class);
	}

	/**
	 * Convert the change to a graph element.
	 * 
	 * @param change
	 * @return
	 */
	public static SchemaChange<?> toGraph(HibSchemaChange<?> change) {
		return checkAndCast(change, SchemaChange.class);
	}

	/**
	 * Convert the version to a graph element.
	 * 
	 * @param version
	 * @return
	 */
	public static GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> toGraph(HibFieldSchemaVersionElement<?, ?, ?, ?> version) {
		return checkAndCast(version, GraphFieldSchemaContainerVersion.class);
	}

	/**
	 * Apply the cast to the graph element and return it.
	 * 
	 * @param <T>
	 *            Type of the graph element
	 * @param element
	 *            MDM element to be casted
	 * @param clazz
	 *            Element class to validate the cast operation
	 * @return Casted element object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T checkAndCast(HibElement element, Class<? extends ElementFrame> clazz) {
		if (element == null) {
			return null;
		}
		if (clazz.isInstance(element)) {
			return (T) clazz.cast(element);
		} else {
			throw new RuntimeException("The received element was not an OrientDB element. Got: " + element.getClass().getName());
		}
	}

}
