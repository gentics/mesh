package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantResponse;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.ImageManipulationRetrievalParametersImpl;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.buffer.Buffer;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class BinaryFieldVariantsTest extends AbstractMeshTest {

	protected String nodeUuid;
	protected ImageVariantResponse defaultAutoVariant1;
	protected ImageVariantResponse defaultAutoVariant2;
	protected ImageVariantResponse defaultOriginalResponse;

	@Before
	public void beforeTest() throws IOException {
		if (nodeUuid == null) {
			synchronized (this) {
				String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());
				Buffer buffer = getBuffer("/pictures/android-gps.jpg");
				NodeResponse node = createBinaryNode(parentNodeUuid);
				call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
					buffer.length(), "test.jpg", "image/jpeg"));

				NodeUpdateRequest nodeUpdateRequest = node.toRequest();
				NodeResponse node3 = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), nodeUpdateRequest));

				// Upload the image again and check that the metadata will be updated
				call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", node3.getVersion(), "binary",
						new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "test.jpg", "image/jpeg"));

				nodeUuid = node.getUuid();

				defaultOriginalResponse = new ImageVariantResponse();
				defaultOriginalResponse.setHeight(150).setWidth(200).setAuto(false).setOrigin(true);
				defaultAutoVariant1 = new ImageVariantResponse();
				defaultAutoVariant1.setHeight(null).setWidth(8).setAuto(true);
				defaultAutoVariant2 = new ImageVariantResponse();
				defaultAutoVariant2.setHeight(24).setWidth(null).setAuto(true);
			}
		} else {
			call(() -> client().clearNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary"));
		}
	}

	@Test
	public void testCreatingAutoVariantsNoOriginal() throws IOException {
		ImageManipulationRequest request = new ImageManipulationRequest();
		request.setVariants(Arrays.asList(defaultAutoVariant1.toRequest(), defaultAutoVariant2.toRequest()));

		ImageVariantsResponse response = call(() -> client().upsertNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary", request));
		assertEquals("There should be 2 variants in total", 2, response.getVariants().size());

		defaultAutoVariant1.setResizeMode(ResizeMode.SMART).setHeight(6);
		defaultAutoVariant2.setResizeMode(ResizeMode.SMART).setWidth(18);
		
		assertThat(response.getVariants()).containsExactlyInAnyOrder(defaultAutoVariant1, defaultAutoVariant2);

		defaultAutoVariant1.setResizeMode(null).setHeight(null);
		defaultAutoVariant2.setResizeMode(null).setWidth(null);
	}

	@Test
	public void testUpdateVariantsViaWebroot() throws IOException {
		testCreatingAutoVariantsNoOriginal();

		String path = tx(tx -> {
			return tx.nodeDao().getPath(tx.nodeDao().findByUuid(tx.projectDao().findByName(PROJECT_NAME), nodeUuid), mockActionContext(), project().getLatestBranch().getUuid(), ContainerType.DRAFT, english());
		});

		ImageManipulationRequest request = new ImageManipulationRequest();
		ImageVariantResponse variant1 = new ImageVariantResponse();
		variant1.setHeight(8).setWidth(8).setAuto(false);
		ImageVariantResponse variant2 = new ImageVariantResponse();
		variant2.setHeight(24).setWidth(24).setAuto(false);
		request.setVariants(Arrays.asList(variant1.toRequest(), variant2.toRequest()));

		call(() -> client().webrootUpdate(PROJECT_NAME, path, new NodeUpdateRequest().setManipulation(request)));

		ImageVariantsResponse response = call(() -> client().getNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary", new ImageManipulationRetrievalParametersImpl().setRetrieveFilesize(true).setRetrieveOriginal(true)));
		assertEquals("There should be 2 new variants + 2 existing ones + 1 original = 5 items in total", 5, response.getVariants().size());

		variant1.setResizeMode(ResizeMode.SMART);
		variant2.setResizeMode(ResizeMode.SMART);
		defaultAutoVariant1.setResizeMode(ResizeMode.SMART).setHeight(6);
		defaultAutoVariant2.setResizeMode(ResizeMode.SMART).setWidth(18);

		assertThat(response.getVariants()).containsExactlyInAnyOrder(defaultAutoVariant1, defaultAutoVariant2, defaultOriginalResponse, variant1, variant2);

		defaultAutoVariant1.setResizeMode(null).setHeight(null);
		defaultAutoVariant2.setResizeMode(null).setWidth(null);
	}

	@Test
	public void testUpdateVariantsViaWebrootField() throws IOException {
		testCreatingAutoVariantsNoOriginal();

		String path = tx(tx -> {
			return tx.nodeDao().getPath(tx.nodeDao().findByUuid(tx.projectDao().findByName(PROJECT_NAME), nodeUuid), mockActionContext(), project().getLatestBranch().getUuid(), ContainerType.DRAFT, english());
		});

		ImageManipulationRequest request = new ImageManipulationRequest();
		ImageVariantResponse variant1 = new ImageVariantResponse();
		variant1.setHeight(8).setWidth(8).setAuto(false);
		ImageVariantResponse variant2 = new ImageVariantResponse();
		variant2.setHeight(24).setWidth(24).setAuto(false);
		request.setVariants(Arrays.asList(variant1.toRequest(), variant2.toRequest()));

		ImageVariantsResponse response = call(() -> client().upsertWebrootFieldImageVariants(PROJECT_NAME, "binary", path, request, new ImageManipulationRetrievalParametersImpl().setRetrieveFilesize(true).setRetrieveOriginal(true)));

		assertEquals("There should be 2 new variants + 2 existing ones + 1 original = 5 items in total", 5, response.getVariants().size());

		variant1.setResizeMode(ResizeMode.SMART);
		variant2.setResizeMode(ResizeMode.SMART);
		defaultAutoVariant1.setResizeMode(ResizeMode.SMART).setHeight(6);
		defaultAutoVariant2.setResizeMode(ResizeMode.SMART).setWidth(18);

		assertThat(response.getVariants()).containsExactlyInAnyOrder(defaultAutoVariant1, defaultAutoVariant2, defaultOriginalResponse, variant1, variant2);

		defaultAutoVariant1.setResizeMode(null).setHeight(null);
		defaultAutoVariant2.setResizeMode(null).setWidth(null);
	}

	@Test
	public void testCreatingVariantsAtop() throws IOException {
		testCreatingAutoVariantsNoOriginal();

		ImageManipulationRequest request = new ImageManipulationRequest();
		ImageVariantResponse variant1 = new ImageVariantResponse();
		variant1.setHeight(8).setWidth(8).setAuto(false);
		ImageVariantResponse variant2 = new ImageVariantResponse();
		variant2.setHeight(24).setWidth(24).setAuto(false);
		request.setVariants(Arrays.asList(variant1.toRequest(), variant2.toRequest()));

		ImageVariantsResponse response = call(() -> client().upsertNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary", request, new ImageManipulationRetrievalParametersImpl().setRetrieveFilesize(true).setRetrieveOriginal(true)));
		assertEquals("There should be 2 new variants + 2 existing ones + 1 original = 5 items in total", 5, response.getVariants().size());

		variant1.setResizeMode(ResizeMode.SMART);
		variant2.setResizeMode(ResizeMode.SMART);
		defaultAutoVariant1.setResizeMode(ResizeMode.SMART).setHeight(6);
		defaultAutoVariant2.setResizeMode(ResizeMode.SMART).setWidth(18);

		assertThat(response.getVariants()).containsExactlyInAnyOrder(defaultAutoVariant1, defaultAutoVariant2, defaultOriginalResponse, variant1, variant2);

		defaultAutoVariant1.setResizeMode(null).setHeight(null);
		defaultAutoVariant2.setResizeMode(null).setWidth(null);
	}

	@Test
	public void testCreatingVariantsDeletingOldOnes() throws IOException {
		testCreatingAutoVariantsNoOriginal();

		ImageManipulationRequest request = new ImageManipulationRequest();
		request.setDeleteOther(true);
		ImageVariantResponse variant1 = new ImageVariantResponse();
		variant1.setHeight(8).setWidth(8).setAuto(false);
		ImageVariantResponse variant2 = new ImageVariantResponse();
		variant2.setHeight(24).setWidth(24).setAuto(false);
		request.setVariants(Arrays.asList(variant1.toRequest(), variant2.toRequest()));

		ImageVariantsResponse response = call(() -> client().upsertNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary", request, new ImageManipulationRetrievalParametersImpl().setRetrieveFilesize(true).setRetrieveOriginal(true)));
		assertEquals("There should be 2 new variants + 1 original = 3 items in total", 3, response.getVariants().size());

		variant1.setResizeMode(ResizeMode.SMART);
		variant2.setResizeMode(ResizeMode.SMART);

		assertThat(response.getVariants()).containsExactlyInAnyOrder(defaultOriginalResponse, variant1, variant2);
	}

	@Test
	public void testClearVariants() throws IOException {
		testCreatingAutoVariantsNoOriginal();
	
		call(() -> client().clearNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary"));
	
		ImageVariantsResponse response = call(() -> client().getNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary"));	
		assertEquals("No variants expected", 0, response.getVariants().size());
	
		response = call(() -> client().getNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary", new ImageManipulationRetrievalParametersImpl().setRetrieveOriginal(true)));	
		assertEquals("Only original expected", 1, response.getVariants().size());
		assertThat(response.getVariants()).containsExactly(defaultOriginalResponse);
	}

	@Test
	public void testGetVariants() throws IOException {
		testCreatingAutoVariantsNoOriginal();
	
		ImageVariantsResponse response = call(() -> client().getNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary"));	
		assertEquals("There should be 2 variants in total", 2, response.getVariants().size());

		defaultAutoVariant1.setResizeMode(ResizeMode.SMART).setHeight(6);
		defaultAutoVariant2.setResizeMode(ResizeMode.SMART).setWidth(18);
		
		assertThat(response.getVariants()).containsExactlyInAnyOrder(defaultAutoVariant1, defaultAutoVariant2);

		response = call(() -> client().getNodeBinaryFieldImageVariants(PROJECT_NAME, nodeUuid, "binary", new ImageManipulationRetrievalParametersImpl().setRetrieveOriginal(true)));	
		assertEquals("There should be 2 new variants + 1 original = 3 items in total", 3, response.getVariants().size());
		assertThat(response.getVariants()).containsExactlyInAnyOrder(defaultAutoVariant1, defaultAutoVariant2, defaultOriginalResponse);

		defaultAutoVariant1.setResizeMode(null).setHeight(null);
		defaultAutoVariant2.setResizeMode(null).setWidth(null);
	}
}
