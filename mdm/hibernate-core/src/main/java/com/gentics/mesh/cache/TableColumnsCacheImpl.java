package com.gentics.mesh.cache;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.hibernate.data.domain.AbstractHibDatabaseElement;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import jakarta.persistence.Entity;

@Singleton
public class TableColumnsCacheImpl extends AbstractMeshCache<Class<?>, Set<String>> implements TableColumnsCache {

	protected static final Logger log = LoggerFactory.getLogger(TableColumnsCacheImpl.class);

	@Inject
	public TableColumnsCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super(createCache(factory), registry, 40);
	}

	/**
	 * Create the cache. 
	 * @param factory cache factory
	 * @return cache instance
	 */
	private static EventAwareCache<Class<?>, Set<String>> createCache(EventAwareCacheFactory factory) {
		// The value is taken from the commented code below
		long size = 46;		
//		try {
//			size = ClassPath.from(AbstractHibDatabaseElement.class.getClassLoader()).getAllClasses()
//					.stream()
//					.filter(cls -> "com.gentics.mesh.hibernate.data.domain".equals(cls.getPackageName()))
//					.map(ClassInfo::load)
//					.filter(cls -> cls.getDeclaredAnnotation(Entity.class) != null)
//					.count();
//		} catch (IOException e) {
//		}
		return factory.<Class<?>, Set<String>>builder()
				.name("tablecolumns")
				.events()
				.maxSize(size)
				.build();
	}
}
