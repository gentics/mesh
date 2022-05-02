package com.gentics.mesh.core.data.job;

/**
 * Extension of {@link HibJob} for mail sending jobs.
 */
public interface HibMailJob extends HibJob {

	/**
	 * Get the mail messages.
	 * @return
	 */
	String getMail();

	/**
	 * Set the Mail Message for the job.
	 *
	 * @param mail
	 */
	void setMail(String mail);

}
