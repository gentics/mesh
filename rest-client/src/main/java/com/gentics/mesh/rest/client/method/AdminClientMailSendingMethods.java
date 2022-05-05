package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.admin.mail.MailSendingResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshRequest;

public interface AdminClientMailSendingMethods {
	/**
	 * Create the job to send the email and trigger the job.
	 * @return
	 */
	MeshRequest<GenericMessageResponse> sendEmail(MailSendingResponse res);
}
