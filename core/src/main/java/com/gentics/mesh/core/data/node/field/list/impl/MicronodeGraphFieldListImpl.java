package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.CompareUtils;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * @see MicronodeGraphFieldList
 */
public class MicronodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<MicronodeGraphField, MicronodeFieldList, Micronode>
		implements MicronodeGraphFieldList {

	public static FieldTransformer<MicronodeFieldList> MICRONODE_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
			parentNode) -> {
		MicronodeGraphFieldList graphMicroschemaField = container.getMicronodeList(fieldKey);
		if (graphMicroschemaField == null) {
			return null;
		} else {
			return graphMicroschemaField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	public static FieldUpdater MICRONODE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		MicronodeGraphFieldList micronodeGraphFieldList = container.getMicronodeList(fieldKey);
		MicronodeFieldList micronodeList = fieldMap.getMicronodeFieldList(fieldKey);
		boolean isMicronodeListFieldSetToNull = fieldMap.hasField(fieldKey) && micronodeList == null;
		GraphField.failOnDeletionOfRequiredField(micronodeGraphFieldList, isMicronodeListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = micronodeList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			GraphField.failOnMissingRequiredField(micronodeGraphFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isMicronodeListFieldSetToNull && micronodeGraphFieldList != null) {
			micronodeGraphFieldList.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		micronodeGraphFieldList = container.createMicronodeFieldList(fieldKey);

		// Handle Update
		// TODO instead this method should also return an observable
		micronodeGraphFieldList.update(ac, micronodeList).blockingGet();
	};

	public static FieldGetter MICRONODE_LIST_GETTER = (container, fieldSchema) -> {
		return container.getMicronodeList(fieldSchema.getName());
	};

	public static void init(Database database) {
		database.addVertexType(MicronodeGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Class<? extends MicronodeGraphField> getListType() {
		return MicronodeGraphFieldImpl.class;
	}

	@Override
	public MicronodeFieldList transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {

		MicronodeFieldList restModel = new MicronodeFieldListImpl();

		for (MicronodeGraphField item : getList()) {
			restModel.getItems().add(item.getMicronode().transformToRestSync(ac, level));
		}
		return restModel;
	}

	@Override
	public Micronode createMicronode() {
		MicronodeImpl micronode = getGraph().addFramedVertex(MicronodeImpl.class);
		addItem(String.valueOf(getSize() + 1), micronode);

		return micronode;
	}

	@Override
	public Single<Boolean> update(InternalActionContext ac, MicronodeFieldList list) {
		// Transform the list of micronodes into a hashmap. This way we can lookup micronode fields faster
		Map<String, Micronode> existing = getList().stream().collect(Collectors.toMap(field -> {
			return field.getMicronode().getUuid();
		}, field -> {
			return field.getMicronode();
		}, (a, b) -> {
			return a;
		}));

		return Observable.<Boolean>create(subscriber -> {
			Observable.fromIterable(list.getItems()).flatMap(item -> {
				if (item == null) {
					throw error(BAD_REQUEST, "field_list_error_null_not_allowed", getFieldKey());
				}

				// Resolve the microschema reference from the rest model
				MicroschemaReference microschemaReference = item.getMicroschema();
				if (microschemaReference == null) {
					// TODO i18n
					return Observable.error(error(INTERNAL_SERVER_ERROR, "Found micronode without microschema reference"));
				}

				return Observable.just(ac.getProject().getMicroschemaContainerRoot().fromReference(microschemaReference, ac.getRelease()));
				// TODO add onError in order to return nice exceptions if the schema / version could not be found
			}, (node, microschemaContainerVersion) -> {
				// Load the micronode for the current field
				Micronode micronode = existing.get(node.getUuid());

				// Create a new micronode if none could be found
				if (micronode == null) {
					micronode = getGraph().addFramedVertex(MicronodeImpl.class);
					micronode.setSchemaContainerVersion(microschemaContainerVersion);
				} else {
					// Avoid microschema container changes for micronode updates
					if (!equalsIgnoreCase(micronode.getSchemaContainerVersion().getUuid(), microschemaContainerVersion.getUuid())) {
						MicroschemaContainerVersion usedContainerVersion = micronode.getSchemaContainerVersion();
						String usedSchema = "name:" + usedContainerVersion.getName() + " uuid:" + usedContainerVersion.getSchemaContainer().getUuid()
								+ " version:" + usedContainerVersion.getVersion();
						String referencedSchema = "name:" + microschemaContainerVersion.getName() + " uuid:"
								+ microschemaContainerVersion.getSchemaContainer().getUuid() + " version:" + microschemaContainerVersion.getVersion();
						throw error(BAD_REQUEST, "node_error_micronode_list_update_schema_conflict", micronode.getUuid(), usedSchema,
								referencedSchema);
					}
				}

				// Update the micronode since it could be found
				try {
					micronode.updateFieldsFromRest(ac, node.getFields());
				} catch (GenericRestException e) {
					throw e;
				} catch (Exception e) {
					throw error(INTERNAL_SERVER_ERROR, "Unknown error while updating micronode list.", e);
				}
				return micronode;
			}).toList().subscribe(micronodeList -> {
				// Clear the list and add new items
				removeAll();
				int counter = 1;
				for (Micronode micronode : micronodeList) {
					existing.remove(micronode.getUuid());
					addItem(String.valueOf(counter++), micronode);
				}
				// Delete remaining items in order to prevent dangling micronodes
				existing.values().stream().forEach(micronode -> {
					micronode.delete(null);
				});
				subscriber.onNext(true);
				subscriber.onComplete();
			}, e -> {
				subscriber.onError(e);
			});
		}).singleOrError();
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		getList().stream().map(MicronodeGraphField::getMicronode).forEach(micronode -> {
			micronode.delete(null);
		});
		getElement().remove();
	}

	@Override
	public List<Micronode> getValues() {
		return getList().stream().map(MicronodeGraphField::getMicronode).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MicronodeGraphFieldList) {
			MicronodeGraphFieldList graphList = ((MicronodeGraphFieldList) obj);
			return CompareUtils.equals(getList(), graphList.getList());
		}
		if (obj instanceof MicronodeFieldListImpl) {
			MicronodeFieldListImpl restField = (MicronodeFieldListImpl) obj;
			List<MicronodeField> restList = restField.getItems();

			List<? extends MicronodeGraphField> graphList = getList();
			List<Micronode> graphMicronodeList = graphList.stream().map(e -> e.getMicronode()).collect(Collectors.toList());
			return CompareUtils.equals(graphMicronodeList, restList);
		}
		return super.equals(obj);
	}
}
