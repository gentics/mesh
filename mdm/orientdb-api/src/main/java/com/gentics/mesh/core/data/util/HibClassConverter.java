package com.gentics.mesh.core.data.util;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibDeletableField;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.branch.BranchVersionEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchVersionAssignment;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphDeletableField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
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
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.syncleus.ferma.ElementFrame;

/**
 * Converter which can transform MDM domain model objects to graph domain element.
 */
public final class HibClassConverter {

	private HibClassConverter() {
	}

	/**
	 * Convert the container field to a graph counterpart.
	 *
	 * @param f
	 * @return
	 */
	public static GraphDeletableField toGraph(HibDeletableField f) {
		return checkAndCast(f, GraphDeletableField.class);
	}

	/**
	 * Convert the container field to a graph counterpart.
	 *
	 * @param f
	 * @return
	 */
	public static GraphField toGraph(HibField f) {
		return checkAndCast(f, GraphField.class);
	}

	/**
	 * Convert the database to a graph counterpart.
	 *
	 * @param tx
	 * @return
	 */
	public static GraphDatabase toGraph(Database tx) {
		return checkAndCast(tx, GraphDatabase.class);
	}

	/**
	 * Convert the transaction to a graph counterpart.
	 *
	 * @param tx
	 * @return
	 */
	public static GraphDBTx toGraph(Tx tx) {
		return checkAndCast(tx, GraphDBTx.class);
	}

	/**
	 * Convert the hib binary field to a graph counterpart.
	 *
	 * @param element
	 * @return
	 */
	public static BinaryGraphField toGraph(HibBinaryField element) {
		return checkAndCast(element, BinaryGraphField.class);
	}

	public static S3BinaryGraphField toGraph(S3HibBinaryField element) {
		return checkAndCast(element, S3BinaryGraphField.class);
	}

	/**
	 * Convert the hib micronode field to a graph counterpart.
	 *
	 * @param element
	 * @return
	 */
	public static MicronodeGraphField toGraph(HibMicronodeField element) {
		return checkAndCast(element, MicronodeGraphField.class);
	}

	/**
	 * Convert the hib node field to a graph counterpart.
	 *
	 * @param element
	 * @return
	 */
	public static NodeGraphFieldContainer toGraph(HibNodeFieldContainer element) {
		return checkAndCast(element, NodeGraphFieldContainer.class);
	}

	/**
	 * Convert the generic hib field container to a graph counterpart.
	 *
	 * @param element
	 * @return
	 */
	public static GraphFieldContainer toGraph(HibFieldContainer element) {
		return checkAndCast(element, GraphFieldContainer.class);
	}

	/**
	 * Convert the hib element to a generic graph element.
	 * 
	 * @param element
	 * @return
	 */
	public static MeshElement toGraph(HibElement element) {
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
	public static <
				RE extends NameUuidReference<RE>,
				SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>,
				SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
				R extends FieldSchemaContainer,
				RM extends FieldSchemaContainerVersion
			> GraphFieldSchemaContainer<R, RM, RE, SC, SCV> toGraphContainer(HibFieldSchemaElement<R, RM, RE, SC, SCV> element) {
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
	public static <
				RE extends NameUuidReference<RE>,
				RM extends FieldSchemaContainerVersion,
				SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>,
				SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
				R extends FieldSchemaContainer
			> GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC> toGraphVersion(
		HibFieldSchemaVersionElement<R, RM, RE, SC, SCV> element) {
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
	 * Convert the image variant to a graph element.
	 * 
	 * @param binary
	 * @return
	 */
	public static ImageVariant toGraph(HibImageVariant binary) {
		return checkAndCast(binary, ImageVariant.class);
	}

	/**
	 * Convert the micronode to a graph element.
	 *
	 * @param node
	 * @return
	 */
	public static Micronode toGraph(HibMicronode node) {
		return checkAndCast(node, Micronode.class);
	}

	/**
	 * Convert the s3binary to a graph element.
	 *
	 * @param s3binary
	 * @return
	 */
	public static S3Binary toGraph(S3HibBinary s3binary) {
		return checkAndCast(s3binary, S3Binary.class);
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
	 * Convert the container edge to a graph element.
	 *
	 * @param change
	 * @return
	 */
	public static GraphFieldContainerEdge toGraph(HibNodeFieldContainerEdge change) {
		return checkAndCast(change, GraphFieldContainerEdge.class);
	}

	/**
	 * Convert the version to a graph element.
	 * 
	 * @param version
	 * @return
	 */
	public static <
				RE extends NameUuidReference<RE>,
				RM extends FieldSchemaContainerVersion,
				SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>,
				SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
				R extends FieldSchemaContainer
			> GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC> toGraph(HibFieldSchemaVersionElement<R, RM, RE, SC, SCV> version) {
		return checkAndCast(version, GraphFieldSchemaContainerVersion.class);
	}

	/**
	 * Apply the cast to the graph element d and return it.
	 *
	 * @param <T>
	 *            Type of the graph field
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
	public static <T extends C, C> T checkAndCast(C element, Class<? extends T> clazz) {
		if (element == null) {
			return null;
		}
		if (clazz.isInstance(element)) {
			return (T) clazz.cast(element);
		} else {
			throw new RuntimeException("The received element does not match the requested class hierarchy. Got '" + element.getClass().getName() + "' vs requested '" + clazz.getName() + "'");
		}
	}
}
