package com.gentics.mesh.graphql.dataloader;

import org.dataloader.DataLoader;

import com.gentics.mesh.core.data.node.NodeContent;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.DataFetcherResult;
import graphql.execution.DataFetcherResult.Builder;
import graphql.schema.DataFetchingEnvironment;

/**
 * Container class for an instance of {@link NodeContent} and an optional {@link RuntimeException}.
 * Instances of this class can be returned from {@link DataLoader} implementations (like the e.g. {@link NodeDataLoader#PARENT_LOADER}).
 */
public class NodeContentWithOptionalRuntimeException {
	/**
	 * NodeContent to be returned (may be null)
	 */
	private final NodeContent nodeContent;

	/**
	 * RuntimeException, which was thrown while trying to fetch the nodeContent (may be null)
	 */
	private final RuntimeException runtimeException;

	/**
	 * Empty instance (nodeContent and runtimeException are both null). Can be used to return null but without error
	 */
	public final static NodeContentWithOptionalRuntimeException EMPTY = new NodeContentWithOptionalRuntimeException();

	/**
	 * Constructor for empty instance
	 */
	private NodeContentWithOptionalRuntimeException() {
		this(null, null);
	}

	/**
	 * Create instance returning the given nodeContent
	 * @param nodeContent returned value
	 */
	public NodeContentWithOptionalRuntimeException(NodeContent nodeContent) {
		this(nodeContent, null);
	}

	/**
	 * Create instance returning null and adding an error
	 * @param runtimeException exception
	 */
	public NodeContentWithOptionalRuntimeException(RuntimeException runtimeException) {
		this(null, runtimeException);
	}

	/**
	 * Create instance returning a nodeContent (may be null) and optionally adding an error
	 * @param nodeContent returned value
	 * @param runtimeException optional exception
	 */
	public NodeContentWithOptionalRuntimeException(NodeContent nodeContent, RuntimeException runtimeException) {
		this.nodeContent = nodeContent;
		this.runtimeException = runtimeException;
	}

	/**
	 * Transform the value into an instance of {@link DataFetcherResult} for the given environment.
	 * If the nodeContent is null, but a runtimeException has been set, the runtimeException will be thrown
	 * @param env data fetching environment
	 * @return data fetcher result
	 */
	public DataFetcherResult<NodeContent> getResult(DataFetchingEnvironment env) {
		if (nodeContent == null && runtimeException != null) {
			throw runtimeException;
		}
		Builder<NodeContent> builder = DataFetcherResult.<NodeContent>newResult().data(nodeContent);
		if (runtimeException != null) {
			builder.error(new ExceptionWhileDataFetching(env.getExecutionStepInfo().getPath(), runtimeException, env.getField().getSourceLocation()));
		}
		return builder.build();
	}
}
