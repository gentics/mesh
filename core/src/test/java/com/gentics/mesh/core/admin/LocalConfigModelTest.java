package com.gentics.mesh.core.admin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.util.PojoUtil;

public class LocalConfigModelTest {
	@Test
	public void testLocalConfigProperties() throws Exception {
		assertThat(
			PojoUtil.getProperties(LocalConfigModel.class)
				.map(PojoUtil.Property::getName)
		).containsExactlyInAnyOrder("readOnly");
	}
}
