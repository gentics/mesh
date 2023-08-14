package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.binary.BinaryGraphFieldVariant;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.node.field.list.HibListField;
import com.gentics.mesh.core.result.Result;

/**
 * A graph field container (eg. a container for fields of a node) is used to hold i18n specific graph fields.
 */
public interface GraphFieldContainer extends HibFieldContainer, BasicFieldContainer {

	/**
	 * Get the list field of specified type
	 * 
	 * @param classOfT
	 * @param fieldKey
	 * @return
	 */
	<T extends HibListField<?, ?, ?>> T getList(Class<T> classOfT, String fieldKey);

	/**
	 * Attach a variant of a binary image with the given field key.
	 * 
	 * @param key
	 * @param variant
	 */
	void attachImageVariant(String key, HibImageVariant variant);

	void detachImageVariant(String key, HibImageVariant variant);

	BinaryGraphFieldVariant findImageVariant(String key, HibImageVariant variant);

	Iterable<? extends BinaryGraphFieldVariant> findImageVariants(String key);
}
