package com.gentics.mesh.plugin;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.RestAPIVersion;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;

public class InitMutatingPlugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(InitMutatingPlugin.class);
	
	public InitMutatingPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return Completable.defer(() -> {
			
			MeshRestClient client = this.environment().createAdminClient(RestAPIVersion.V2);
			
			log.info("Mutate 'em all!");
			
			return Completable.merge(
					IntStream.range(0, 100).mapToObj(
							i -> {
								String projectName = "prj" + System.currentTimeMillis();
								log.info("Mutating " + projectName);
								try {
									Thread.sleep(2);
								} catch (InterruptedException e) {
									return Completable.error(e);
								}
								return client.createProject(new ProjectCreateRequest().setName(projectName).setSchemaRef("folder")).getResponse().toCompletable();
							}
						).collect(Collectors.toList()));
		});
	}
}
