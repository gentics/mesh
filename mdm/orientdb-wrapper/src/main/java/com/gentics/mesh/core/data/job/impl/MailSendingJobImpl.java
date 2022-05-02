package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.HibMailJob;
import com.gentics.mesh.core.data.job.JobCore;

/**
 * Job implementation to be used for persisting and invoking mail sending.
 */
	public class MailSendingJobImpl extends JobImpl implements JobCore, HibMailJob {
	private static final String MAIL = "mail";

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MailSendingJobImpl.class, MeshVertexImpl.class);
	}

	@Override
	public String getMail() {
		return getProperty(MAIL);
	}

	@Override
	public void setMail(String mail) {
		if(mail != null) {
			this.setProperty(MAIL, mail);
		}

	}
}
