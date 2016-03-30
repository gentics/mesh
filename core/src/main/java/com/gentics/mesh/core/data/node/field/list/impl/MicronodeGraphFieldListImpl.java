package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.RxUtil;

import rx.Observable;

/**
 * @see MicronodeGraphFieldList
 */
public class MicronodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<MicronodeGraphField, MicronodeFieldList, Micronode>
		implements MicronodeGraphFieldList {

	public static void checkIndices(Database database) {
		database.addVertexType(MicronodeGraphFieldListImpl.class);
	}

	@Override
	public Class<? extends MicronodeGraphField> getListType() {
		return MicronodeGraphFieldImpl.class;
	}

	@Override
	public Observable<MicronodeFieldList> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {

		MicronodeFieldList restModel = new MicronodeFieldListImpl();

		List<Observable<MicronodeResponse>> obs = new ArrayList<>();
		for (MicronodeGraphField item : getList()) {
			obs.add(item.getMicronode().transformToRestSync(ac, level));
		}

		return RxUtil.concatList(obs).collect(() -> {
			return restModel.getItems();
		} , (x, y) -> {
			x.add(y);
		}).map(i -> restModel);
	}

	@Override
	public Micronode createMicronode(MicronodeField field) {
		MicronodeImpl micronode = getGraph().addFramedVertex(MicronodeImpl.class);
		addItem(String.valueOf(getSize() + 1), micronode);

		return micronode;
	}

	@Override
	public Observable<Boolean> update(InternalActionContext ac, MicronodeFieldList list) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// Transform the list of micronodes into a hashmap. This way we can lookup micronode fields faster
		Map<String, Micronode> existing = getList().stream().collect(Collectors.toMap(field -> {
			return field.getMicronode().getUuid();
		} , field -> {
			return field.getMicronode();
		} , (a, b) -> {
			return a;
		}));

		return Observable.create(subscriber -> {
			Observable.from(list.getItems()).flatMap(item -> {
				// Resolve the microschema reference from the rest model
				MicroschemaReference microschemaReference = item.getMicroschema();
				if (microschemaReference == null) {
					return Observable.error(error(INTERNAL_SERVER_ERROR, "Found micronode without microschema reference"));
				}

				String microschemaName = microschemaReference.getName();
				String microschemaUuid = microschemaReference.getUuid();
				Integer version = microschemaReference.getVersion();
				Observable<MicroschemaContainer> containerObs = null;
				if (!isEmpty(microschemaName)) {
					containerObs = boot.microschemaContainerRoot().findByName(microschemaName);
				} else {
					containerObs = boot.microschemaContainerRoot().findByUuid(microschemaUuid);
				}
				// Return the specified version or fallback to latest version.
				return containerObs.map(container -> {
					if (version == null) {
						return container.getLatestVersion();
					} else {
						return container.findVersionByRev(version);
					}
				});
				// TODO add onError in order to return nice exceptions if the schema / version could not be found
			} , (node, microschemaContainerVersion) -> {
				// Load the micronode for the current field
				Micronode micronode = existing.get(node.getUuid());

				// Create a new micronode if none could be found
				if (micronode == null) {
					micronode = getGraph().addFramedVertex(MicronodeImpl.class);
					micronode.setMicroschemaContainerVersion(microschemaContainerVersion);
				} else {
					// Avoid microschema container changes for micronode updates
					if (!equalsIgnoreCase(micronode.getMicroschemaContainerVersion().getUuid(), microschemaContainerVersion.getUuid())) {
						MicroschemaContainerVersion usedContainerVersion = micronode.getMicroschemaContainerVersion();
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
					micronode.updateFieldsFromRest(ac, node.getFields(), micronode.getMicroschema());
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
				existing.values().stream().forEach(Micronode::delete);
			} , e -> {
				subscriber.onError(e);
			} , () -> {
				subscriber.onNext(true);
				subscriber.onCompleted();
			});
		});
	}

	@Override
	public void delete() {
		getList().stream().map(MicronodeGraphField::getMicronode).forEach(Micronode::delete);
		getElement().remove();
	}

	@Override
	public List<Micronode> getValues() {
		return getList().stream().map(MicronodeGraphField::getMicronode).collect(Collectors.toList());
	}
}
