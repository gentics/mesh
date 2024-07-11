package com.gentics.mesh.core.data.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.core.rest.node.field.MicronodeFieldModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.util.CompareUtils;
import com.gentics.mesh.util.ETag;

public interface Micronode extends FieldContainer, BaseElement, TransformableElement<MicronodeResponse> {

	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	NodeFieldContainer getContainer();

	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	default Result<? extends NodeFieldContainer> getContainers() {
		return getContainers(true, true);
	}

	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	Result<? extends NodeFieldContainer> getContainers(boolean lookupInFields, boolean lookupInLists);

	/**
	 * Compare the micronode and return a list of changes which identify the changes.
	 * 
	 * @param micronodeB
	 *            Micronode to compare with
	 * @return
	 */
	default List<FieldContainerChange> compareTo(Micronode micronode) {
		List<FieldContainerChange> changes = new ArrayList<>();
		for (FieldSchema fieldSchema : getSchemaContainerVersion().getSchema().getFields()) {
			Field fieldA = getField(fieldSchema);
			Field fieldB = micronode.getField(fieldSchema);
			if (!CompareUtils.equals(fieldA, fieldB)) {
				changes.add(new FieldContainerChange(fieldSchema.getName(), FieldChangeTypes.UPDATED));
			}
		}
		return changes;
	}

	/**
	 * Make this micronode a clone of the given micronode. Property Vertices are reused
	 *
	 * @param micronode
	 *            micronode
	 */
	default void clone(Micronode micronode) {
		List<Field> otherFields = micronode.getFields();

		for (Field graphField : otherFields) {
			graphField.cloneTo(this);
		}
	}

	/**
	 * Micronodes don't provide a dedicated API path since those can't be directly accessed via REST URI.
	 * 
	 * @param ac
	 */
	@Override
	default String getAPIPath(InternalActionContext ac) {
		// Micronodes have no public location
		return null;
	}

	@Override
	default ReferenceType getReferenceType() {
		return ReferenceType.MICRONODE;
	}

	@Override
	MicroschemaVersion getSchemaContainerVersion();

	/**
	 * General implementation for the micronode transformation into the REST representation
	 * 
	 * @param ac
	 * @param level
	 * @param languageTags
	 * @return
	 */
	default MicronodeResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		NodeParametersImpl parameters = new NodeParametersImpl(ac);
		MicronodeResponse restMicronode = new MicronodeResponse();
		MicroschemaVersion microschemaContainer = getSchemaContainerVersion();
		if (microschemaContainer == null) {
			throw error(BAD_REQUEST, "The microschema container for micronode {" + getUuid() + "} could not be found.");
		}

		MicroschemaModel microschemaModel = microschemaContainer.getSchema();
		if (microschemaModel == null) {
			throw error(BAD_REQUEST, "The microschema for micronode {" + getUuid() + "} could not be found.");
		}

		restMicronode.setMicroschema(microschemaContainer.transformToReference());
		restMicronode.setUuid(getUuid());

		List<String> requestedLanguageTags = new ArrayList<>();
		if (languageTags.length == 0) {
			requestedLanguageTags.addAll(parameters.getLanguageList(Tx.get().data().options()));
		} else {
			requestedLanguageTags.addAll(Arrays.asList(languageTags));
		}

		// Fields
		for (FieldSchema fieldEntry : microschemaModel.getFields()) {
			FieldModel restField = getRestField(ac, fieldEntry.getName(), fieldEntry, requestedLanguageTags, level);
			if (restField != null) {
				restMicronode.getFields().put(fieldEntry.getName(), restField);
			}
		}

		return restMicronode;
	}

	/**
	 * Fetch the node that this micronode is currently attached to.
	 * 
	 * @return
	 */
	default Node getNode() {
		ContentDao contentDao = Tx.get().contentDao();
		NodeFieldContainer container = getContainer();
		while (container.getPreviousVersion() != null) {
			container = container.getPreviousVersion();
		}
		return contentDao.getNode(container);
	}

	@Override
	default String getLanguageTag() {
		return getContainer().getLanguageTag();
	}

	@Override
	default Stream<? extends NodeFieldContainer> getContents(boolean lookupInFields, boolean lookupInLists) {
		return getContainers(lookupInFields, lookupInLists).stream();
	}

	@Override
	default String getETag(InternalActionContext ac) {
		// TODO check whether the uuid remains static for micronode updates
		return ETag.hash(getUuid());
	}

	/**
	 * A default method cannot override a method from java.lang.Object. 
	 * This is the common equality check implementation, that has to be reused by the HibMicronode implementors.
	 * 
	 * @param obj
	 * @return
	 */
	default boolean micronodeEquals(Object obj) {
		if (obj instanceof Micronode) {
			Micronode micronode = getClass().cast(obj);
			List<Field> fieldsA = getFields();
			List<Field> fieldsB = micronode.getFields();
			return CompareUtils.equals(fieldsA, fieldsB);
		}
		if (obj instanceof MicronodeFieldModel) {
			MicronodeFieldModel restMicronode = (MicronodeFieldModel) obj;
			MicroschemaModel schema = getSchemaContainerVersion().getSchema();
			// Iterate over all field schemas and compare rest and graph with eachother
			for (FieldSchema fieldSchema : schema.getFields()) {
				Field graphField = getField(fieldSchema);
				FieldModel restField = restMicronode.getFields().getField(fieldSchema.getName(), fieldSchema);
				if (!CompareUtils.equals(graphField, restField)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
