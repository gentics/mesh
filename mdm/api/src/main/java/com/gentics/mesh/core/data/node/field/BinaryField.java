package com.gentics.mesh.core.data.node.field;

import java.util.Objects;

import com.gentics.mesh.core.data.DeletableField;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.node.field.nesting.ReferenceField;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.BinaryFieldModel;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadataModel;
import com.gentics.mesh.core.rest.node.field.image.FocalPointModel;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.handler.ActionContext;

public interface BinaryField extends ImageDataField, BasicField<BinaryFieldModel>, DeletableField, DisplayField, ReferenceField<Binary> {

	/**
	 * Copy the values of this field to the specified target field.
	 *
	 * @param target
	 * @return Fluent API
	 */
	BinaryField copyTo(BinaryField target);

	/**
	 * Get the parent container.
	 * 
	 * @return
	 */
	NodeFieldContainer getParentContainer();

	/**
	 * Get the image variants, attached to this binary field.
	 * 
	 * @return
	 */
	Result<? extends ImageVariant> getImageVariants();

	@Override
	Binary getBinary();

	@Override
	default Binary getReferencedEntity() {
		return getBinary();
	}

	@Override
	default BinaryFieldModel transformToRest(ActionContext ac) {
		BinaryFieldModel restModel = new BinaryFieldImpl();
		restModel.setFileName(getFileName());
		restModel.setMimeType(getMimeType());

		Binary binary = getBinary();
		if (binary != null) {
			restModel.setBinaryUuid(binary.getUuid());
			restModel.setFileSize(binary.getSize());
			restModel.setSha512sum(binary.getSHA512Sum());
			restModel.setWidth(binary.getImageWidth());
			restModel.setHeight(binary.getImageHeight());
			restModel.setCheckStatus(binary.getCheckStatus());
		}

		restModel.setFocalPoint(getImageFocalPoint());
		restModel.setDominantColor(getImageDominantColor());

		BinaryMetadataModel metaData = getMetadata();
		restModel.setMetadata(metaData);

		restModel.setPlainText(getPlainText());

		return restModel;
	}

	@Override
	default void validate() {

	}

	/**
	 * Common comparison code.
	 *
	 * @param obj
	 * @return
	 */
	default boolean binaryFieldEquals(Object obj) {
		if (getClass().isInstance(obj)) {
			BinaryField binaryField = getClass().cast(obj);
			String filenameA = getFileName();
			String filenameB = binaryField.getFileName();
			boolean filename = Objects.equals(filenameA, filenameB);

			String mimeTypeA = getMimeType();
			String mimeTypeB = binaryField.getMimeType();
			boolean mimetype = Objects.equals(mimeTypeA, mimeTypeB);

			Binary binaryA = getBinary();
			Binary binaryB = binaryField.getBinary();

			String hashSumA = binaryA != null ? binaryA.getSHA512Sum() : null;
			String hashSumB = binaryB != null ? binaryB.getSHA512Sum() : null;
			boolean sha512sum = Objects.equals(hashSumA, hashSumB);

			BinaryCheckStatus statusA = binaryA != null ? binaryA.getCheckStatus() : null;
			BinaryCheckStatus statusB = binaryB != null ? binaryB.getCheckStatus() : null;
			boolean checkStatus = Objects.equals(statusA, statusB);

			return filename && mimetype && sha512sum && checkStatus;
		}
		if (obj instanceof BinaryFieldModel) {
			BinaryFieldModel binaryField = (BinaryFieldModel) obj;

			boolean matchingFilename = true;
			if (binaryField.getFileName() != null) {
				String filenameA = getFileName();
				String filenameB = binaryField.getFileName();
				matchingFilename = Objects.equals(filenameA, filenameB);
			}

			boolean matchingMimetype = true;
			if (binaryField.getMimeType() != null) {
				String mimeTypeA = getMimeType();
				String mimeTypeB = binaryField.getMimeType();
				matchingMimetype = Objects.equals(mimeTypeA, mimeTypeB);
			}

			boolean matchingFocalPoint = true;
			if (binaryField.getFocalPoint() != null) {
				FocalPointModel pointA = getImageFocalPoint();
				FocalPointModel pointB = binaryField.getFocalPoint();
				matchingFocalPoint = Objects.equals(pointA, pointB);
			}

			boolean matchingDominantColor = true;
			if (binaryField.getDominantColor() != null) {
				String colorA = getImageDominantColor();
				String colorB = binaryField.getDominantColor();
				matchingDominantColor = Objects.equals(colorA, colorB);
			}

			boolean matchingSha512sum = true;
			if (binaryField.getSha512sum() != null) {
				String hashSumA = getBinary() != null ? getBinary().getSHA512Sum() : null;
				String hashSumB = binaryField.getSha512sum();
				matchingSha512sum = Objects.equals(hashSumA, hashSumB);
			}

			boolean matchingMetadata = true;
			if (binaryField.getMetadata() != null) {
				BinaryMetadataModel graphMetadata = getMetadata();
				BinaryMetadataModel restMetadata = binaryField.getMetadata();
				matchingMetadata = Objects.equals(graphMetadata, restMetadata);
			}

			boolean matchingCheckStatus = true;

			if (binaryField.getCheckStatus() != null) {
				BinaryCheckStatus statusA = getBinary() != null ? getBinary().getCheckStatus() : null;
				BinaryCheckStatus statusB = binaryField.getCheckStatus();

				matchingCheckStatus = Objects.equals(statusA, statusB);
			}

			return matchingFilename
				&& matchingMimetype
				&& matchingFocalPoint
				&& matchingDominantColor
				&& matchingSha512sum
				&& matchingMetadata
				&& matchingCheckStatus;
		}

		return false;
	}

	@Override
	default String getDisplayName() {
		return getFileName();
	}
}
