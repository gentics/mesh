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

	protected static final int DEFINED_LIMIT = 10;
	protected static final int CALCULATED_LIMIT = DEFINED_LIMIT - HibernateUtil.NUM_STALE_QUERY_PARAMETERS;

	protected HibernateMeshOptions options;

	@Before
	public void setup() {
		options = new HibernateMeshOptions();
		options.getStorageOptions().setSqlParametersLimit(Integer.toString(DEFINED_LIMIT));

		HibTxData data = mock(HibTxData.class);
		when(data.options()).thenReturn(options);
		HibernateTx tx = mock(HibernateTx.class);
		when(tx.data()).thenReturn(data);
		Tx.setActive(tx);
	}

	@Test
	public void testInQueriesLimitForSplittingLessThanLimit() {
		testInQueriesLimit(3);
	}

	@Test
	public void testInQueriesLimitForSplittingMoreThanLimit() {
		testInQueriesLimit(13);
	}

	@Test
	public void testInQueriesLimitForSplittingZero() {
		testInQueriesLimit(0);
	}

	@Test
	public void testInQueriesLimitForSplittingExactLimit() {
		testInQueriesLimit(Integer.parseInt(options.getStorageOptions().getSqlParametersLimit()));
	}

	protected void testInQueriesLimit(int numParams) {
		int limit = HibernateUtil.inQueriesLimitForSplitting(numParams);
		assertThat(limit).as("Calculated limit")
			.isGreaterThan(0)
			.isEqualTo(CALCULATED_LIMIT - ((numParams < DEFINED_LIMIT) ? numParams : 0));
	}
}
