package com.gentics.mesh.core.data.dao;

import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobWarningList;

/**
 * A persisting extension to {@link JobDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingJobDao extends JobDao, PersistingDaoGlobal<HibJob> {

	@Override
	default JobResponse transformToRestSync(HibJob job, InternalActionContext ac, int level, String... languageTags) {
		JobResponse response = new JobResponse();
		response.setUuid(job.getUuid());

		HibUser creator = job.getCreator();
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

		JobWarningList warnings = job.getWarnings();
		if (warnings != null) {
			response.setWarnings(warnings.getData());
		}

		Map<String, String> props = response.getProperties();
		HibBranch branch = job.getBranch();
		if (branch != null) {
			props.put("branchName", branch.getName());
			props.put("branchUuid", branch.getUuid());
		} else {
			log.debug("No referenced branch found.");
		}

		HibSchemaVersion toSchema = job.getToSchemaVersion();
		if (toSchema != null) {
			HibSchema container = toSchema.getSchemaContainer();
			props.put("schemaName", container.getName());
			props.put("schemaUuid", container.getUuid());
			props.put("fromVersion", job.getFromSchemaVersion().getVersion());
			props.put("toVersion", toSchema.getVersion());
		}

		HibMicroschemaVersion toMicroschema = job.getToMicroschemaVersion();
		if (toMicroschema != null) {
			HibMicroschema container = toMicroschema.getSchemaContainer();
			props.put("microschemaName", container.getName());
			props.put("microschemaUuid", container.getUuid());
			props.put("fromVersion", job.getFromMicroschemaVersion().getVersion());
			props.put("toVersion", toMicroschema.getVersion());
		}
		return response;
	}
}
