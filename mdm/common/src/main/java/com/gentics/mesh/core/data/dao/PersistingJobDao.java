package com.gentics.mesh.core.data.dao;

import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobWarningListModel;
import com.gentics.mesh.event.EventQueueBatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persisting extension to {@link JobDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingJobDao extends JobDao, PersistingDaoGlobal<Job> {
	static final Logger log = LoggerFactory.getLogger(JobDao.class);

	@Override
	default JobResponse transformToRestSync(Job job, InternalActionContext ac, int level, String... languageTags) {
		JobResponse response = new JobResponse();
		response.setUuid(job.getUuid());

		User creator = job.getCreator();
		if (creator != null) {
			response.setCreator(creator.transformToReference());
		} else {
			//log.error("The object {" + getClass().getSimpleName() + "} with uuid {" + getUuid() + "} has no creator. Omitting creator field");
		}

		String date = job.getCreationDate();
		response.setCreated(date);
		response.setErrorMessage(job.getErrorMessage());
		response.setErrorDetail(job.getErrorDetail());
		response.setType(job.getType());
		response.setStatus(job.getStatus());
		response.setStopDate(job.getStopDate());
		response.setStartDate(job.getStartDate());
		response.setCompletionCount(job.getCompletionCount());
		response.setNodeName(job.getNodeName());

		JobWarningListModel warnings = job.getWarnings();
		if (warnings != null) {
			response.setWarnings(warnings.getData());
		}

		Map<String, String> props = response.getProperties();
		Branch branch = job.getBranch();
		if (branch != null) {
			props.put("branchName", branch.getName());
			props.put("branchUuid", branch.getUuid());
		} else {
			log.debug("No referenced branch found.");
		}

		SchemaVersion toSchema = job.getToSchemaVersion();
		if (toSchema != null) {
			Schema container = toSchema.getSchemaContainer();
			props.put("schemaName", container.getName());
			props.put("schemaUuid", container.getUuid());
			props.put("fromVersion", job.getFromSchemaVersion().getVersion());
			props.put("toVersion", toSchema.getVersion());
		}

		MicroschemaVersion toMicroschema = job.getToMicroschemaVersion();
		if (toMicroschema != null) {
			Microschema container = toMicroschema.getSchemaContainer();
			props.put("microschemaName", container.getName());
			props.put("microschemaUuid", container.getUuid());
			props.put("fromVersion", job.getFromMicroschemaVersion().getVersion());
			props.put("toVersion", toMicroschema.getVersion());
		}
		return response;
	}

	@Override
	default Job create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new NotImplementedException("Jobs cannot be created using REST");
	}

	@Override
	default boolean update(Job job, InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Jobs can't be updated");
	}
}
