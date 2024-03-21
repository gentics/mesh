package com.gentics.mesh.core.data.page.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.query.MeshGraphEdgeQuery;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.StreamUtil;
import com.syncleus.ferma.FramedGraph;

/**
 * This page implementation will handle paging internally and on-demand. The internal paging will only iterate over as many items as the needed operation
 * requires. Loading the first page will thus only iterate over the elements of the first page. Loading the total count on the other hand requires the
 * implementation to iterate over all edges.
 *
 * @param <T>
 */
public class DynamicTransformablePageImpl<T extends TransformableElement<? extends RestModel>> extends AbstractDynamicPage<T> {

	private HibUser requestUser;

	private Predicate<T> extraFilter;

	private boolean frameExplicitly;

	private DynamicTransformablePageImpl(HibUser requestUser, PagingParameters pagingInfo, Predicate<T> extraFilter, boolean frameExplicitly) {
		super(pagingInfo);
		this.extraFilter = extraFilter;
		this.requestUser = requestUser;
		this.frameExplicitly = frameExplicitly;
	}

	/**
	 * Create new dynamic page.
	 *
	 * @param requestUser
	 *            User which is used to check permissions
	 * @param root
	 *            Root vertex which provides the elements which can be paged
	 * @param pagingInfo
	 *            Paging information which contains the perPage and page information
	 */
	public DynamicTransformablePageImpl(HibUser requestUser, RootVertex<? extends T> root, PagingParameters pagingInfo) {
		this(requestUser, root, pagingInfo, READ_PERM, null, true);
	}

	/**
	 * Create a new dynamic page.
	 *
	 * @param requestUser
	 *            User which is used to check permissions
	 * @param root
	 *            Root vertex which provides the elements which can be paged
	 * @param pagingInfo
	 *            Paging information which contains the perPage and page information
	 * @param perm
	 *            Permission used to filter elements by
	 * @param extraFilter
	 *            Optional extra filter to filter by
	 * @param frameExplicitly
	 *            Whether to frame the found value explicitily
	 *
	 */
	public DynamicTransformablePageImpl(HibUser requestUser, RootVertex<? extends T> root, PagingParameters pagingInfo, InternalPermission perm,
		Predicate<T> extraFilter, boolean frameExplicitly) {
		this(requestUser, pagingInfo, extraFilter, frameExplicitly);
		init(root.getPersistanceClass(), root.getRootLabel(), root.id(), Direction.IN, root.getGraph(), perm, root.getPersistenceClassVariations());
	}

	/**
	 * Create a new dynamic page.
	 *
	 * @param requestUser
	 *            User which is used to check permissions
	 * @param rootLabel
	 * 			  Root vertex label     
	 * @param indexKey
	 *            Key to be used for the index lookup
	 * @param dir
	 *            The direction to be resolved for each resulting edge in order to get to the target element
	 * @param clazz
	 *            Class of the element to be returned
	 * @param pagingInfo
	 *            Paging parameters
	 * @param perm
	 *            Permission to check against
	 * @param extraFilter
	 *            Optional extra filter to filter by
	 * @param frameExplicitly
	 *            Whether to frame the found value explicitily
	 */
	public DynamicTransformablePageImpl(HibUser requestUser, String rootLabel, Object indexKey, Direction dir, Class<T> clazz, PagingParameters pagingInfo,
		InternalPermission perm, Predicate<T> extraFilter, boolean frameExplicitly) {
		this(requestUser, pagingInfo, extraFilter, frameExplicitly);
		init(clazz, rootLabel, indexKey, dir, GraphDBTx.getGraphTx().getGraph(), perm, Optional.empty());
	}

	/**
	 * Create a new dynamic page.
	 *
	 * @param user
	 *            User to check permissions against
	 * @param traversal
	 *            Traversal which yields the items
	 * @param pagingInfo
	 *            Paging settings
	 * @param perm
	 *            Permission to check against
	 * @param clazz
	 *            Element class used to reframe the found elements
	 */
	public DynamicTransformablePageImpl(HibUser user, VertexTraversal<?, ?> traversal, PagingParameters pagingInfo, InternalPermission perm,
		Class<? extends T> clazz) {
		this(user, pagingInfo, null, true);
		init(clazz, traversal, perm);
	}

	private void init(Class<? extends T> clazz, VertexTraversal<?, ?> traversal, InternalPermission perm) {
		// Iterate over all vertices that are managed by this root vertex
		Stream<Vertex> stream = StreamUtil.toStream(traversal.iterator());
		applyPagingAndPermChecks(stream, clazz, perm);
	}

	/**
	 * Modify the given stream and add further filters and mapping functions to it in order to be able to track operations and element handling.
	 *
	 * @param stream
	 * @param clazz
	 * @param perm
	 */
	private void applyPagingAndPermChecks(Stream<Vertex> stream, Class<? extends T> clazz, InternalPermission perm) {
		AtomicLong pageCounter = new AtomicLong();
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();

		UserDao userDao = GraphDBTx.getGraphTx().userDao();

		// Only handle elements which are visible to the user
		if (perm != null) {
			stream = stream.filter(item -> userDao.hasPermissionForId(requestUser, item.id(), perm));
		}

		Stream<T> framedStream;
		if (extraFilter == null) {
			// We can skip a lot of framing if we don't use a filter
			framedStream = stream.map(item -> {
				if (pageFull.get()) {
					return null;
				} else {
					return frameExplicitly
						? graph.frameElementExplicit(item, clazz)
						: graph.frameElement(item, clazz);
				}
			});
		} else {
			framedStream = stream.map(item -> frameExplicitly
				? graph.frameElementExplicit(item, clazz)
				: graph.frameElement(item, clazz)
			).filter(extraFilter);
		}

		framedStream = framedStream
			.peek(item -> totalCounter.incrementAndGet());

		if (lowerBound != null) {
			framedStream = framedStream.skip(lowerBound);
		}

		framedStream = framedStream
			.peek(element -> {
			// Only add elements to the list if those elements are part of selected the page
			long elementsInPage = pageCounter.get();
			if (perPage == null || elementsInPage < perPage) {
				elementsOfPage.add(element);
				pageCounter.incrementAndGet();
			} else {
				pageFull.set(true);
				hasNextPage.set(true);
			}
		});

		visibleItems = framedStream.iterator();

	}

	/**
	 * Initialize the dynamic iterator which is bound to the most getters of this class. A stream is setup which is used to filter out the unwanted data. Paging
	 * is also handled via the stream. At the end only a iterator is provided for the other methods. The iterator next method is invoked until the needed data
	 * is provided or the iterator has no more items.
	 *
	 * @param clazz
	 *            Class used to frame the found elements.
	 * @param rootLabel
	 * 			  Root vertex label           
	 * @param indexName
	 *            Name of the graph index to use
	 * @param indexKey
	 *            Key object used for the lookup
	 * @param vertexDirection
	 *            The direction to be resolved for each resulting edge in order to get to the target element.
	 * @param graph
	 *            Framed graph used to re-frame the resulting elements
	 * @param perm
	 *            Graph permission to filter by
	 * @param maybeVariations 
	 *            Variations of an entity type to fetch. Currently subclasses of {@link HibJob} are used here.
	 */
	private void init(Class<? extends T> clazz, String rootLabel, Object indexKey, Direction vertexDirection, FramedGraph graph,
		InternalPermission perm, Optional<? extends Collection<? extends Class<?>>> maybeVariations) {

		MeshGraphEdgeQuery query = GraphDBTx.getGraphTx().edgeQuery(clazz, rootLabel);
		query.relationDirection(vertexDirection);
		query.directionPointsTo(vertexDirection.opposite(), indexKey);
		List<String> sortParams = sort.entrySet().stream().map(e -> e.getKey() + " " + e.getValue().getValue()).collect(Collectors.toUnmodifiableList());
		query.setOrderPropsAndDirs(sortParams.toArray(new String[sortParams.size()]));
		Stream<? extends Edge> itemEdges = StreamUtil.toStream(query.fetch(maybeVariations));

		applyPagingAndPermChecks(itemEdges
				// Get the vertex from the edge
				.map(itemEdge -> {
					switch(vertexDirection) {
					case IN:
						return itemEdge.inVertex();
					case OUT:
						return itemEdge.outVertex();
					default:
						throw new IllegalStateException("Unsupported direction: " + vertexDirection);
					}
				}), clazz, perm);
	}
}
