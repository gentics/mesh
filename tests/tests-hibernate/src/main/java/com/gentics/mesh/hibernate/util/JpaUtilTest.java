package com.gentics.mesh.hibernate.util;

import com.gentics.mesh.hibernate.util.JpaUtil;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class JpaUtilTest {

	@Test
	public void testToCountQuery() {
		String query = "select n from node n";
		String actual = JpaUtil.toCountHQL(query);

		Assertions.assertThat(actual).isEqualTo("select count( n ) from node n");
	}
}