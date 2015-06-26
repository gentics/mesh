package com.gentics.mesh.core.data.service.transformation.tag;

import static com.gentics.mesh.core.data.service.I18NService.getI18n;
import static com.gentics.mesh.core.data.service.LanguageService.getLanguageService;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.model.root.TagFamily;
import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshAuthUser;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.tag.response.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.util.BlueprintTransaction;

public class TagTransformationTask extends RecursiveTask<Void> {

	private static final long serialVersionUID = -5106639943022399262L;

	private static final Logger log = LoggerFactory.getLogger(TagTransformationTask.class);

	private Tag tag;
	private TransformationInfo info;
	private TagResponse restTag;
	private int currentDepth;

	public TagTransformationTask(Tag tag, TransformationInfo info, TagResponse restTag, int depth) {
		this.tag = tag;
		this.info = info;
		this.restTag = restTag;
		this.currentDepth = depth;
	}

	public TagTransformationTask(Tag tag, TransformationInfo info, TagResponse restTag) {
		this(tag, info, restTag, 0);
	}

	@Override
	protected Void compute() {

		Set<ForkJoinTask<Void>> tasks = new HashSet<>();
		String uuid = tag.getUuid();
		MeshAuthUser requestUser = info.getRequestUser();
		// Check whether the tag has already been transformed by another task
		TagResponse foundTag = (TagResponse) info.getObjectReferences().get(uuid);
		if (foundTag == null) {

			try (BlueprintTransaction tx = new BlueprintTransaction(MeshSpringConfiguration.getMeshSpringConfiguration()
					.getFramedThreadedTransactionalGraph())) {
				restTag.setPermissions(requestUser.getPermissionNames(tag));
				restTag.setUuid(tag.getUuid());

				TagFamily tagFamilyRoot = tag.getTagFamilyRoot();

				if (tagFamilyRoot != null) {
					TagFamilyReference tagFamilyReference = new TagFamilyReference();
					tagFamilyReference.setName(tagFamilyRoot.getName());
					tagFamilyReference.setUuid(tagFamilyRoot.getUuid());
					restTag.setTagFamilyReference(tagFamilyReference);
				}

				MeshUser creator = tag.getCreator();
				if (creator != null) {
					restTag.setCreator(creator.transformToRest());
				}

				restTag.setName(tag.getName());
				tx.success();
			}

			//			info.addObject(uuid, restTag);
		} else {
			restTag = foundTag;
		}

		//		if (currentDepth < info.getMaxDepth()) {
		//		}
		//		if (info.isIncludeTags()) {
		//			TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, currentDepth, restTag, tasks);
		//			tag.getTags().parallelStream().forEachOrdered(tagConsumer);
		//		} else {
		//			restTag.setTags(null);
		//		}
		//
		//		if (info.isIncludeContents()) {
		//			ContentTraversalConsumer contentConsumer = new ContentTraversalConsumer(info, currentDepth, restTag, tasks);
		//			tag.getContents().parallelStream().forEachOrdered(contentConsumer);
		//		} else {
		//			restTag.setContents(null);
		//		}
		//
		//		if (info.isIncludeChildTags()) {
		//			TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, currentDepth, restTag, tasks);
		//			tag.getChildTags().parallelStream().forEachOrdered(tagConsumer);
		//		} else {
		//			restTag.setChildTags(null);
		//		}

		// Wait for our forked tasks
		tasks.forEach(action -> action.join());
		return null;
	}
}
