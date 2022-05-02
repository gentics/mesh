package com.gentics.mesh.context;

import java.math.BigDecimal;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.MailSendingCause;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;

/**
 * Context of a micronode migration.
 */
public interface MailSendingContext extends InternalActionContext {

	HibBranch getBranch();

	HibNode getHibNode();

	HibBinary getHibBinary();

	BinaryDao getBinaryDao();

	String getBinaryName();

	String getBinaryMimeType();

	/**
	 * Return the cause info of the mail sending.
	 * 
	 * @return
	 */
	MailSendingCause getCause();

	/**
	 * Validate that all needed information are present in the context.
	 */
	void validate();
}
