package com.gentics.mesh.core.rest.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.UUIDUtil;

public class ConflictWarningTest {

	@Test
	public void testModel() {
		ConflictWarning warning = new ConflictWarning();
		warning.setNodeUuid(UUIDUtil.randomUUID());
		System.out.println(warning.toJson());

		JobWarningList warnings = new JobWarningList();
		warnings.add(warning);

		String json = JsonUtil.toJson(warnings);
		System.out.println(json);

		JobWarningList remapped = JsonUtil.readValue(json, JobWarningList.class);
		assertEquals("node-conflict-resolution", remapped.getData().get(0).getType());
	}
}
