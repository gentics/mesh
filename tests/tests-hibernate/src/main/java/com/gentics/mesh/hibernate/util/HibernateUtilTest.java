package com.gentics.mesh.hibernate.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.database.HibTxData;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public class HibernateUtilTest {

	protected HibernateMeshOptions options;

	@Before
	public void setup() {
		options = new HibernateMeshOptions();
		options.getStorageOptions().setSqlParametersLimit(Integer.toString(10));

		HibTxData data = mock(HibTxData.class);
		when(data.options()).thenReturn(options);
		HibernateTx tx = mock(HibernateTx.class);
		when(tx.data()).thenReturn(data);
		Tx.setActive(tx);
	}

	@Test
	public void testInQueriesLimitForSplittingLessThanLimit() {
		int limit = HibernateUtil.inQueriesLimitForSplitting(1);
		assertThat(limit).as("Calculated limit")
			.isGreaterThan(0)
			.isEqualTo(Integer.parseInt(options.getStorageOptions().getSqlParametersLimit()) - 5 - 1);
	}

	@Test
	public void testInQueriesLimitForSplittingMoreThanLimit() {
		int limit = HibernateUtil.inQueriesLimitForSplitting(12);
		assertThat(limit).as("Calculated limit")
			.isGreaterThan(0)
			.isEqualTo(Integer.parseInt(options.getStorageOptions().getSqlParametersLimit()) - 5);
	}

	@Test
	public void testInQueriesLimitForSplittingZero() {
		int limit = HibernateUtil.inQueriesLimitForSplitting(0);
		assertThat(limit).as("Calculated limit")
			.isGreaterThan(0)
			.isEqualTo(Integer.parseInt(options.getStorageOptions().getSqlParametersLimit()) - 5);
	}


	@Test
	public void testInQueriesLimitForSplittingLimit() {
		int limit = HibernateUtil.inQueriesLimitForSplitting(Integer.parseInt(options.getStorageOptions().getSqlParametersLimit()));
		assertThat(limit).as("Calculated limit")
			.isGreaterThan(0)
			.isEqualTo(Integer.parseInt(options.getStorageOptions().getSqlParametersLimit()) - 5);
	}
}
