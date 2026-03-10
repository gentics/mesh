package com.gentics.mesh.core;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;

public abstract class AbstractHibernateOptionsProvider implements HibernateMeshOptionsProvider {

	protected static final Logger log = LoggerFactory.getLogger(AbstractHibernateOptionsProvider.class);

	protected final HibernateMeshOptions meshOptions;
	
	@SuppressWarnings("unchecked")
	public AbstractHibernateOptionsProvider() {
		Class<HibernateMeshOptions> optionsClass = Optional.ofNullable(System.getProperty(ENV_OPTIONS_CLASS, StringUtils.EMPTY)).filter(StringUtils::isNotBlank).map(cls -> {
			try {
				return (Class<HibernateMeshOptions>) Class.forName(cls);
			} catch (ClassNotFoundException e) {
				log.error("Could not instantiate custom options class: " + cls, e);
				return HibernateMeshOptions.class;
			}
		}).orElse(HibernateMeshOptions.class);
		meshOptions = OptionsLoader.createOrloadOptions(optionsClass, OptionsLoader.generateDefaultConfig(optionsClass, null));
	}

	@Override
	public MeshOptions getOptions() {
		fillMeshOptions(meshOptions);
		return meshOptions;
	}

	@Override
	public MeshOptions getClone() throws Exception {
		ObjectMapper mapper = OptionsLoader.getYAMLMapper();
		String optionsAsString = mapper.writeValueAsString(meshOptions);

		HibernateMeshOptions clonedMeshOptions = mapper.readValue(optionsAsString, meshOptions.getClass());

		// by actually sharing the storage options, we make sure that if the database connection settings are changed in the original mesh options,
		// all instances will get the updated settings
		clonedMeshOptions.setStorageOptions(meshOptions.getStorageOptions());

		return clonedMeshOptions;
	}
}

