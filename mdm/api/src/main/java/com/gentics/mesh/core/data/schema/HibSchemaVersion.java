package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.ElementType.SCHEMAVERSION;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.event.branch.AbstractBranchAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.etc.config.ContentConfig;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.EncodeUtil;

/**
 * Each schema update is stored within a dedicated schema container version in order to be able to keep track of changes in between different schema container
 * versions.
 */
public interface HibSchemaVersion extends HibFieldSchemaVersionElement<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion> {

	static final TypeInfo TYPE_INFO = new TypeInfo(SCHEMAVERSION, SCHEMA_CREATED, SCHEMA_UPDATED, SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Get container entity bound to this version.
	 */
	HibSchema getSchemaContainer();

	/**
	 * Bind container entity to this version.
	 */
	void setSchemaContainer(HibSchema container);

	/**
	 * Return jobs which reference the schema version.
	 * 
	 * @return
	 */
	Iterable<? extends HibJob> referencedJobsViaTo();

	/**
	 * Check the autopurge flag of the version.
	 * 
	 * @return
	 */
	default boolean isAutoPurgeEnabled() {
		Boolean schemaAutoPurge = getSchema().getAutoPurge();
		if (schemaAutoPurge == null) {
			if (log.isDebugEnabled()) {
				log.debug("No schema auto purge flag set. Falling back to mesh global setting");
			}
			ContentConfig contentOptions = Tx.get().data().options().getContentOptions();
			if (contentOptions != null) {
				return contentOptions.isAutoPurge();
			} else {
				return true;
			}
		} else {
			return schemaAutoPurge;
		}
	}

	/**
	 * Transform the version to a reference POJO.
	 * 
	 * @return
	 */
	default SchemaReference transformToReference() {
		SchemaReferenceImpl reference = new SchemaReferenceImpl();
		reference.setName(getName());
		reference.setUuid(getSchemaContainer().getUuid());
		reference.setVersion(getVersion());
		reference.setVersionUuid(getUuid());
		return reference;
	}

	@Override
	default SchemaVersionModel getSchema() {
		ServerSchemaStorage serverSchemaStorage = Tx.get().data().serverSchemaStorage();
		SchemaVersionModel schema = serverSchemaStorage.getSchema(getName(), getVersion());
		if (schema == null) {
			schema = JsonUtil.readValue(getJson(), SchemaModelImpl.class);
			serverSchemaStorage.addSchema(schema);
		}
		return schema;
	}

	@Override
	default SchemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		// Load the schema and add/overwrite some properties
		// Use getSchema to utilize the schema storage
		SchemaResponse restSchema = JsonUtil.readValue(getJson(), SchemaResponse.class);
		HibSchema container = getSchemaContainer();
		container.fillCommonRestFields(ac, fields, restSchema);
		restSchema.setRolePerms(Tx.get().roleDao().getRolePermissions(container, ac, ac.getRolePermissionParameters().getRoleUuid()));
		return restSchema;
	}

	@Override
	default void setSchema(SchemaVersionModel schema) {
		ServerSchemaStorage serverSchemaStorage = Tx.get().data().serverSchemaStorage();
		serverSchemaStorage.removeSchema(schema.getName(), schema.getVersion());
		serverSchemaStorage.addSchema(schema);
		// TODO FIXME can we rely on a frontend formatter, to allow de-prettifying the schema JSON?
		String json = schema.toJson(false);
		setJson(json);
		setVersion(schema.getVersion());
	}

	@Override
	default String getSubETag(InternalActionContext ac) {
		return "";
	}

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	default MeshEvent getBranchAssignEvent() {
		return MeshEvent.SCHEMA_BRANCH_ASSIGN;
	}

	@Override
	default MeshEvent getBranchUnassignEvent() {
		return MeshEvent.SCHEMA_BRANCH_UNASSIGN;
	}

	@Override
	default Class<? extends AbstractBranchAssignEventModel<SchemaReference>> getBranchAssignEventModelClass() {
		return BranchSchemaAssignEventModel.class;
	}

	/**
	 * Get the hash over all uuids of microschema versions, which are currently assigned to the branch and which are used in the schema version.
	 * A microschema is "used" by a schema, if the schema contains a field of type "microschema", where the microschema is mentioned in the "allowed" property.
	 * @param branch branch
	 * @return hash
	 */
	default String getMicroschemaVersionHash(HibBranch branch) {
		return getMicroschemaVersionHash(branch, Collections.emptyMap());
	}

	/**
	 * Check whether the schema uses the given microschema.
	 * A microschema is "used" by a schema, if the schema contains a field of type "microschema", where the microschema is mentioned in the "allowed" property.
	 * @param microschema microschema in question
	 * @return true, when the schema version uses the microschema, false if not
	 */
	default boolean usesMicroschema(HibMicroschema microschema) {
		return !getFieldsUsingMicroschema(microschema).isEmpty();
	}

	/**
	 * Variant of {@link #getMicroschemaVersionHash(Branch)}, that gets a replacement map (which might be empty, but not null). The replacement map may map microschema names
	 * to microschema version uuids to be used instead of the currently assigned microschema version.
	 * @param branch branch
	 * @param replacementMap replacement map
	 * @return hash
	 */
	default String getMicroschemaVersionHash(HibBranch branch, Map<String, String> replacementMap) {
		Objects.requireNonNull(branch, "The branch must not be null");
		Objects.requireNonNull(replacementMap, "The replacement map must not be null (but may be empty)");
		Set<String> microschemaNames = getSchema().getFields().stream().filter(filterMicronodeField())
				.flatMap(field -> {
					return getAllowedMicroschemas(field).stream();
				}).collect(Collectors.toSet());

		if (microschemaNames.isEmpty()) {
			return null;
		} else {
			Set<String> microschemaVersionUuids = new TreeSet<>();
			for (HibBranchMicroschemaVersion edge : branch.findAllLatestMicroschemaVersionEdges()) {
				HibMicroschemaVersion version = edge.getMicroschemaContainerVersion();
				MicroschemaVersionModel microschema = version.getSchema();
				String microschemaName = microschema.getName();

				// if the microschema is one of the "used" microschemas, we either get the version uuid from the replacement map, or
				// the uuid of the currently assigned version
				if (microschemaNames.contains(microschemaName)) {
					microschemaVersionUuids.add(replacementMap.getOrDefault(microschemaName, version.getUuid()));
				}
			}

			if (microschemaVersionUuids.isEmpty()) {
				return null;
			} else {
				try {
					return EncodeUtil.md5Hex(microschemaVersionUuids.stream().collect(Collectors.joining("|")).getBytes());
				} catch (NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Get the name of fields, using the microschema.
	 * A field uses the microschema, if it is either a of type "microschema" or of type "list of microschemas" and mentions the microschema in the "allowed" property.
	 * @param microschema microschema in question
	 * @return set of field names
	 */
	default Set<String> getFieldsUsingMicroschema(HibMicroschema microschema) {
		return getSchema().getFields().stream().filter(filterMicronodeField())
				.filter(field -> getAllowedMicroschemas(field).contains(microschema.getName()))
				.map(FieldSchema::getName).collect(Collectors.toSet());
	}

	/**
	 * Return a predicate that filters fields that are either of type "micronode", or "list of micronodes"
	 * @return predicate
	 */
	private Predicate<FieldSchema> filterMicronodeField() {
		return field -> {
			if (FieldTypes.valueByName(field.getType()) == FieldTypes.MICRONODE) {
				return true;
			} else if (FieldTypes.valueByName(field.getType()) == FieldTypes.LIST) {
				ListFieldSchema listField = (ListFieldSchema) field;
				return FieldTypes.valueByName(listField.getListType()) == FieldTypes.MICRONODE;
			} else {
				return false;
			}
		};
	}

	/**
	 * Get the allowed microschemas used by the field
	 * @param field field
	 * @return collection of allowed microschema names
	 */
	private Collection<String> getAllowedMicroschemas(FieldSchema field) {
		if (field instanceof MicronodeFieldSchema) {
			MicronodeFieldSchema micronodeField = (MicronodeFieldSchema) field;
			if (micronodeField.getAllowedMicroSchemas() == null) {
				return Collections.emptyList();
			} else {
				return Arrays.asList(micronodeField.getAllowedMicroSchemas());
			}
		} else if (field instanceof ListFieldSchema) {
			ListFieldSchema listField = (ListFieldSchema) field;
			if (listField.getAllowedSchemas() == null) {
				return Collections.emptyList();
			} else {
				return Arrays.asList(listField.getAllowedSchemas());
			}
		} else {
			return Collections.emptyList();
		}
	}
}
