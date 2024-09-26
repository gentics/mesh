package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibBinaryDataElement;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.util.UniquenessUtil;

/**
 * A generic binary data field.
 * 
 * @author plyhun
 *
 */
public interface HibBinaryDataField extends HibField {

	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	String getFileName();

	/**
	 * Set the binary filename.
	 * 
	 * @param fileName
	 * @return Fluent API
	 */
	HibBinaryDataField setFileName(String fileName);

	/**
	 * Return the binary mime type of the node.
	 * 
	 * @return
	 */
	String getMimeType();

	/**
	 * Set the binary mime type of the node.
	 * 
	 * @param mimeType
	 * @return Fluent API
	 */
	HibBinaryDataField setMimeType(String mimeType);
	
	/**
	 * Return the referenced binary.
	 * 
	 * @return
	 */
	HibBinaryDataElement getBinary();

	/**
	 * Increment any found postfix number in the filename.
	 * 
	 * e.g:
	 * <ul>
	 * <li>test.txt -> test_1.txt</li>
	 * <li>test -> test_1</li>
	 * <li>test.blub.txt -> test.blub_1.txt</li>
	 * <ul>
	 * 
	 */
	default void postfixFileName() {
		String oldName = getFileName();
		if (oldName != null && !oldName.isEmpty()) {
			setFileName(UniquenessUtil.suggestNewName(oldName));
		}
	}
}
