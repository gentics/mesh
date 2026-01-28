package com.gentics.mesh.router;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

/**
 * @see RouterStorageRegistry
 */
@Singleton
public class RouterStorageRegistryImpl implements RouterStorageRegistry {

	private Set<RouterStorage> instances = new ConcurrentHashSet<>();

	@Inject
	public RouterStorageRegistryImpl() {
	}

	/**
	 * Register the eventbus handlers of all stored router storages.
	 */
	public synchronized void registerEventbus() {
		for (RouterStorage rs : instances) {
			rs.registerEventbusHandlers();
		}
	}

	/**
	 * Iterate over all created router storages and assert that no project/api route
	 * causes a conflict with the given name
	 * 
	 * @param name
	 */
	public synchronized void assertProjectName(String name) {
		for (RouterStorage rs : instances) {
			rs.root().apiRouter().projectsRouter().assertProjectNameValid(name);
		}
	}

	@Override
	public synchronized void addProject(String name) throws InvalidNameException {
		for (RouterStorage rs : instances) {
			rs.root().apiRouter().projectsRouter().addProjectRouter(name);
		}
	}

	@Override
	public synchronized boolean hasProject(String projectName) {
		for (RouterStorage rs : instances) {
			if (rs.root().apiRouter().projectsRouter().hasProjectRouter(projectName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<RouterStorage> getInstances() {
		return instances;
	}

	private final class ConcurrentHashSet<E> implements Set<E> {

		private final Map<E, Object> map;

		private static final Object OBJ = new Object();

		public ConcurrentHashSet() {
			map = new ConcurrentHashMap<>();
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public boolean isEmpty() {
			return map.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return map.containsKey(o);
		}

		@Override
		public Iterator<E> iterator() {
			return map.keySet().iterator();
		}

		@Override
		public Object[] toArray() {
			return map.keySet().toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return map.keySet().toArray(a);
		}

		@Override
		public boolean add(E e) {
			return map.put(e, OBJ) == null;
		}

		@Override
		public boolean remove(Object o) {
			return map.remove(o) != null;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return map.keySet().containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			boolean changed = false;
			for (E e : c) {
				if (map.put(e, OBJ) == null) {
					changed = true;
				}
			}
			return changed;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			map.clear();
		}
	}
}
