package com.gentics.mesh.core.data.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.util.CompareUtils;
import com.gentics.mesh.util.ETag;

public interface HibMicronode extends HibFieldContainer, HibBaseElement, HibTransformableElement<MicronodeResponse> {

	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	HibNodeFieldContainer getContainer();

	/**
	 * Get the container of this micronode which can either be referenced via a micronode list or a directly to the container.
	 *
	 * @return container
	 */
	Result<? extends HibNodeFieldContainer> getContainers();

	/**
	 * Compare the micronode and return a list of changes which identify the changes.
	 * 
	 * @param micronodeB
	 *            Micronode to compare with
	 * @return
	 */
	default List<FieldContainerChange> compareTo(HibMicronode micronode) {
		List<FieldContainerChange> changes = new ArrayList<>();
		for (FieldSchema fieldSchema : getSchemaContainerVersion().getSchema().getFields()) {
			HibField fieldA = getField(fieldSchema);
			HibField fieldB = micronode.getField(fieldSchema);
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
	default void clone(HibMicronode micronode) {
		List<HibField> otherFields = micronode.getFields();

		for (HibField graphField : otherFields) {
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
	HibMicroschemaVersion getSchemaContainerVersion();

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
		HibMicroschemaVersion microschemaContainer = getSchemaContainerVersion();
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
			Field restField = getRestField(ac, fieldEntry.getName(), fieldEntry, requestedLanguageTags, level);
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
	default HibNode getNode() {
		ContentDao contentDao = Tx.get().contentDao();
		HibNodeFieldContainer container = getContainer();
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
	default Stream<? extends HibNodeFieldContainer> getContents() {
		return getContainers().stream();
	}

	@Override
	default String getETag(InternalActionContext ac) {
		// TODO check whether the uuid remains static for micronode updates
		return ETag.hash(getUuid());
	}

	default boolean micronodeEquals(Object obj) {
		if (obj instanceof HibMicronode) {
			HibMicronode micronode = getClass().cast(obj);
			List<HibField> fieldsA = getFields();
			List<HibField> fieldsB = micronode.getFields();
			return CompareUtils.equals(fieldsA, fieldsB);
		}
		if (obj instanceof MicronodeField) {
			MicronodeField restMicronode = (MicronodeField) obj;
			MicroschemaModel schema = getSchemaContainerVersion().getSchema();
			// Iterate over all field schemas and compare rest and graph with eachother
			for (FieldSchema fieldSchema : schema.getFields()) {
				HibField graphField = getField(fieldSchema);
				Field restField = restMicronode.getFields().getField(fieldSchema.getName(), fieldSchema);
				if (!CompareUtils.equals(graphField, restField)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
