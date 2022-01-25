package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.util.CompareUtils;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface HibMicronodeFieldList extends HibMicroschemaListableField, HibReferencingListField<HibMicronodeField, MicronodeFieldList, HibMicronode> {

	String TYPE = "micronode";

	/**
	 * Create a new empty micronode and add it to the list.
	 * 
	 * @return
	 */
	default HibMicronode createMicronode(HibMicroschemaVersion microschemaContainerVersion) {
		return createMicronodeAt(Optional.of((int)getSize()), microschemaContainerVersion);
	}

	/**
	 * Create a new empty micronode and, if an index is given, insert it into the list.
	 * 
	 * @param maybeIndex
	 * @param microschemaContainerVersion 
	 * @return
	 */
	HibMicronode createMicronodeAt(Optional<Integer> maybeIndex, HibMicroschemaVersion microschemaContainerVersion);

	@Override
	default MicronodeFieldList transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {

		MicronodeFieldList restModel = new MicronodeFieldListImpl();

		for (HibMicronodeField item : getList()) {
			restModel.getItems().add(item.getMicronode().transformToRestSync(ac, level));
		}
		return restModel;
	}

	/**
	 * Update the micronode list using the rest model list as a source.
	 * 
	 * @param ac
	 * @param list
	 * @return
	 */
	default Single<Boolean> update(InternalActionContext ac, MicronodeFieldList list) {
		// Transform the list of micronodes into a hashmap. This way we can lookup micronode fields faster
		Map<String, HibMicronode> existing = getList().stream().collect(Collectors.toMap(field -> {
			return field.getMicronode().getUuid();
		}, field -> {
			return field.getMicronode();
		}, (a, b) -> {
			return a;
		}));

		return Observable.<Boolean>create(subscriber -> {

			Iterator<MicronodeField> it = list.getItems().stream().map(item -> {
				if (item == null) {
					throw error(BAD_REQUEST, "field_list_error_null_not_allowed", getFieldKey());
				}
				return item;
			}).iterator();
			Observable.fromIterable(() -> it).flatMap(item -> {

				// Resolve the microschema reference from the rest model
				MicroschemaReference microschemaReference = item.getMicroschema();
				if (microschemaReference == null) {
					// TODO i18n
					return Observable.error(error(INTERNAL_SERVER_ERROR, "Found micronode without microschema reference"));
				}

				Tx tx = Tx.get();
				MicroschemaDao microschemaDao = tx.microschemaDao();
				HibMicroschemaVersion container = microschemaDao.fromReference(tx.getProject(ac), microschemaReference, tx.getBranch(ac));
				return Observable.just(container);
				// TODO add onError in order to return nice exceptions if the schema / version could not be found
			}, (node, microschemaContainerVersion) -> {
				// Load the micronode for the current field
				HibMicronode micronode = existing.get(node.getUuid());
				// Create a new micronode if none could be found
				if (micronode == null) {
					micronode = createMicronodeAt(Optional.empty(), microschemaContainerVersion);
				} else {
					// Avoid microschema container changes for micronode updates
					if (!equalsIgnoreCase(micronode.getSchemaContainerVersion().getUuid(), microschemaContainerVersion.getUuid())) {
						HibMicroschemaVersion usedContainerVersion = micronode.getSchemaContainerVersion();
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
				int counter = 0;
				for (HibMicronode micronode : micronodeList) {
					existing.remove(micronode.getUuid());
					insertReferenced(counter++, micronode);
				}
				// Delete remaining items in order to prevent dangling micronodes
				existing.values().stream().forEach(micronode -> {
					deleteReferenced(micronode);
				});
				subscriber.onNext(true);
				subscriber.onComplete();
			}, e -> {
				subscriber.onError(e);
			});
		}).singleOrError();
	}

	@Override
	default List<HibMicronode> getValues() {
		return getList().stream().map(HibMicronodeField::getMicronode).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof HibMicronodeFieldList) {
			HibMicronodeFieldList graphList = ((HibMicronodeFieldList) obj);
			return CompareUtils.equals(getList(), graphList.getList());
		}
		if (obj instanceof MicronodeFieldListImpl) {
			MicronodeFieldListImpl restField = (MicronodeFieldListImpl) obj;
			List<MicronodeField> restList = restField.getItems();

			List<? extends HibMicronodeField> graphList = getList();
			List<HibMicronode> graphMicronodeList = graphList.stream().map(e -> e.getMicronode()).collect(Collectors.toList());
			return CompareUtils.equals(graphMicronodeList, restList);
		}
		return HibReferencingListField.super.listEquals(obj);
	}
}
