package com.gentics.mesh.core.graphql.nativefilter;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLPublishedMatchingDraftNodePermissionTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.NativeGraphQLFilterTests;

/**
 * Tests for node queries when having a node with published version equals to draft version.
 */
@Category({ NativeGraphQLFilterTests.class })
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLPublishedMatchingDraftNodePermissionTest extends GraphQLPublishedMatchingDraftNodePermissionTest {

    public NativeGraphQLPublishedMatchingDraftNodePermissionTest(String query, String version) {
        super(query, version);
    }
}