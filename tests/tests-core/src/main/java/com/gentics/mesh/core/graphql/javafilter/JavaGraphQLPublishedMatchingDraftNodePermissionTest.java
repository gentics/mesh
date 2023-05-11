package com.gentics.mesh.core.graphql.javafilter;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLPublishedMatchingDraftNodePermissionTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

/**
 * Tests for node queries when having a node with published version equals to draft version.
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
public class JavaGraphQLPublishedMatchingDraftNodePermissionTest extends GraphQLPublishedMatchingDraftNodePermissionTest {

    public JavaGraphQLPublishedMatchingDraftNodePermissionTest(String query, String version) {
        super(query, version);
    }
}