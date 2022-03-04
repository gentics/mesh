package com.gentics.mesh.cli;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionsTypeUnawareContext;

import io.vertx.core.Vertx;

public class MeshImplTest implements MeshOptionsTypeUnawareContext {

	@Test
	public void testHostname() throws Exception {
		MeshComponent.Builder b = Mockito.mock(MeshComponent.Builder.class);
		MeshImpl mesh = new MeshImpl(options(), b);
		assertNotNull(mesh.getHostname());
	}

	@Test
	public void testUpdateCheck() throws Exception {
		MeshComponent.Builder b = Mockito.mock(MeshComponent.Builder.class);
		MeshImpl mesh = new MeshImpl(options(), b);
		MeshComponent i = Mockito.mock(MeshComponent.class);
		when(i.vertx()).thenReturn(Vertx.vertx());
		mesh.setMeshInternal(i);

		mesh.invokeUpdateCheck();
	}

	public MeshOptions options() {
		MeshOptions opts = getOptions();
		opts.getSearchOptions().setStartEmbedded(false);
		return opts;
	}
}
