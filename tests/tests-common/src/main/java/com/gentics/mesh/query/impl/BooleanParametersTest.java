package com.gentics.mesh.query.impl;

import static com.gentics.mesh.parameter.DeleteParameters.RECURSIVE_PARAMETER_KEY;
import static com.gentics.mesh.parameter.SchemaUpdateParameters.UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;

/**
 * Tests the parsing of boolean query parameter values.
 */
public class BooleanParametersTest {

	@Test
	public void testFlagWithDefaultFalse() {
		DeleteParametersImpl params = new DeleteParametersImpl();
		assertFalse("Unset flag must use the default", params.isRecursive());

		params.setParameter(RECURSIVE_PARAMETER_KEY, "true");
		assertTrue(params.isRecursive());
		params.setParameter(RECURSIVE_PARAMETER_KEY, "false");
		assertFalse(params.isRecursive());
		params.setParameter(RECURSIVE_PARAMETER_KEY, "1");
		assertTrue("1 must be parsed as true", params.isRecursive());
		params.setParameter(RECURSIVE_PARAMETER_KEY, "0");
		assertFalse("0 must be parsed as false", params.isRecursive());
		params.setParameter(RECURSIVE_PARAMETER_KEY, "invalid");
		assertFalse("Invalid value must use the default", params.isRecursive());
	}

	@Test
	public void testFlagWithDefaultTrue() {
		SchemaUpdateParametersImpl params = new SchemaUpdateParametersImpl();
		assertTrue("Unset flag must use the default", params.getUpdateAssignedBranches());

		params.setParameter(UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY, "false");
		assertFalse(params.getUpdateAssignedBranches());
		params.setParameter(UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY, "true");
		assertTrue(params.getUpdateAssignedBranches());
		params.setParameter(UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY, "0");
		assertFalse("0 must be parsed as false", params.getUpdateAssignedBranches());
		params.setParameter(UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY, "1");
		assertTrue("1 must be parsed as true", params.getUpdateAssignedBranches());
		params.setParameter(UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY, "invalid");
		assertTrue("Invalid value must use the default", params.getUpdateAssignedBranches());
	}
}
