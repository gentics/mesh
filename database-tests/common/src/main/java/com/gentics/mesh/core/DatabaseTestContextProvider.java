package com.gentics.mesh.core;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.output.OutputFrame;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.docker.DatabaseContainer;
import com.gentics.mesh.test.MeshInstanceProvider;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.MeshTestSetting;

/**
 * Common test context provider functionality, that utilizes docker-hosted test databases.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public abstract class DatabaseTestContextProvider<T extends DatabaseContainer<T>> extends HibernateTestContextProvider implements MeshTestContextProvider, MeshInstanceProvider<HibernateMeshOptions> {

	private final T databaseContainer;
	
	public DatabaseTestContextProvider(T databaseContainer) {
		this.databaseContainer = databaseContainer;
	}

	@Override
	public void teardownStorage() {
		stopIfPossible();
	}
	@Override
	public void initPhysicalStorage(MeshTestSetting settings) throws Exception {
		super.initPhysicalStorage(settings);
		startIfRequired();
	}
	@Override
	public void fillMeshOptions(HibernateMeshOptions meshOptions) {
		meshOptions.getStorageOptions().setShowSql(false);
		meshOptions.getStorageOptions().setFormatSql(false);
		startIfRequired();
	}
	public T getDatabaseContainer() {
		return databaseContainer;
	}

	private final void logContainerFrame(OutputFrame frame) {
		OutputFrame.OutputType outputType = frame.getType();

        String utf8String = frame.getUtf8String();
        utf8String = utf8String.replaceAll("((\\r?\\n)|(\\r))$", "");
        if (StringUtils.isBlank(utf8String)) {
        	return;
        }
        switch (outputType) {
            case END:
                break;
            case STDOUT:
            	log.info("{}/{}: {}", databaseContainer.getClass().getSimpleName(), outputType, utf8String);
                break;
            case STDERR:
                log.error("{}/{}: {}", databaseContainer.getClass().getSimpleName(), outputType, utf8String);
                break;
            default:
                throw new IllegalArgumentException("Unexpected outputType " + outputType);
        }
	}

	public void startIfRequired() {
		if (!databaseContainer.isRunning()) {
			databaseContainer.start();
			log.info("Started {} at port {}", getClass().getSimpleName(), databaseContainer.getMappedPort());
			databaseContainer.followOutput(this::logContainerFrame);
		}
	}

	public void stopIfPossible() {
		if (databaseContainer.isRunning()) {
			databaseContainer.stop();
		}
	}
}
