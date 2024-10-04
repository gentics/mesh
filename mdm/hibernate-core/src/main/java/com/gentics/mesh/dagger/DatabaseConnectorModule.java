package com.gentics.mesh.dagger;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.database.connector.service.DatabaseConnectorService;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.handler.RuntimeServiceRegistry;
import com.gentics.mesh.util.StreamUtil;

import dagger.Module;
import dagger.Provides;

/**
 * Database connector provider module
 */
@Module
public abstract class DatabaseConnectorModule {

	@Provides
	@Singleton
	public static DatabaseConnector databaseConnector(HibernateMeshOptions options, RuntimeServiceRegistry runtimeHandler) {
		try {
			Logger log = LoggerFactory.getLogger(DatabaseConnectorModule.class);
			String databaseConnectorClasspath = options.getStorageOptions().getDatabaseConnectorClasspath();
			if (StringUtils.isNotBlank(databaseConnectorClasspath)) {
				Path classPath = Path.of(databaseConnectorClasspath);
				URL[] classpaths;
				if (classPath.toFile().isDirectory()) {
					List<URL> jars = Files.walk(classPath, FileVisitOption.FOLLOW_LINKS)
							.filter(p -> p.toFile().isFile())
							.map(f -> {
								try {
									return f.toUri().toURL();
								} catch (MalformedURLException e) {
									throw new IllegalArgumentException(e);
								}
							}).collect(Collectors.toList());
					classpaths = new URL[jars.size()];
					classpaths = jars.toArray(classpaths);
				} else {
					classpaths = new URL[] {classPath.toUri().toURL()};
				}
				ClassLoader classLoader = new URLClassLoader(classpaths, Thread.currentThread().getContextClassLoader());
				Thread.currentThread().setContextClassLoader(classLoader);
			}
			return StreamUtil.toStream(ServiceLoader.load(DatabaseConnectorService.class))
				.map(dc -> dc.instantiate(options, runtimeHandler))
				.peek(dc -> log.info("Found connector: {}", dc.getConnectorDescription()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Could not find the database connector in either provided path [" + databaseConnectorClasspath + "] or the default classpath!"));
		} catch (Throwable t) {
			throw new RuntimeException("Could not instantiate a database connector", t);
		}
	}
}
