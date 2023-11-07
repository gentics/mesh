package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.binary.BinaryGraphFieldVariant;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.node.field.list.HibListField;

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

	/**
	 * Detach the variant from the container with the binary field of a given key.
	 * 
	 * @param key
	 * @param variant
	 */
	void detachImageVariant(String key, HibImageVariant variant);

	/**
	 * Find a variant edge for the given image variant for the binary field of a given key.
	 * 
	 * @param key
	 * @param variant
	 * @return
	 */
	BinaryGraphFieldVariant findImageVariant(String key, HibImageVariant variant);

	/**
	 * Find all variant edges for the binary field of a given key.
	 * 
	 * @param key
	 * @return
	 */
	Iterable<? extends BinaryGraphFieldVariant> findImageVariants(String key);
}
