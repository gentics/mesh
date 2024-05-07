package com.gentics.mesh.graphql.dataloader;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;

import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;

/**
 * The GraphQL path-aware key for {@link DataLoader}.
 * 
 * @param <T>
 */
public final class DataLoaderKey<T> {

	private final String path;
	private final T value;

	public DataLoaderKey(String path, T value) {
		this.path = path;
		this.value = value;
	}

	public DataLoaderKey(ResultPath path, T value) {
		this(path.toString().replaceAll("\\[[\\d+]\\]", StringUtils.EMPTY), value);
	}

	public DataLoaderKey(DataFetchingEnvironment env, T value) {
		this(env.getExecutionStepInfo().getPath(), value);
	}

	public String getPath() {
		return path;
	}

	public T getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(path, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataLoaderKey<?> other = (DataLoaderKey<?>) obj;
		return Objects.equals(path, other.path) && Objects.equals(value, other.value);
	}
}
