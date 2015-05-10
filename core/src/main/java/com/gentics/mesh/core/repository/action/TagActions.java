package com.gentics.mesh.core.repository.action;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.Content;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.paging.PagingInfo;

public interface TagActions {

	/**
	 * Find all visible tags within the given project. /tags
	 * 
	 * @param userUuid
	 * @param projectName
	 * @param languageTags
	 * @param pageingInfo
	 * @return
	 */
	public Page<Tag> findProjectTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pageingInfo);

	/**
	 * Find all visible tags that are tagging the given tag.
	 * @param userUuid
	 * @param projectName
	 * @param languageTags
	 * @param pageingInfo
	 * @return
	 */
	public Page<Tag> findTaggingTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pageingInfo);

	/**
	 * Find all visible tags that are tagged using the given tag.
	 * @param userUuid
	 * @param projectName
	 * @param languageTags
	 * @param pageingInfo
	 * @return
	 */
	public Page<Tag> findTaggedTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pageingInfo);

	/**
	 * Find all visible child tags for the given project and roottag. /tags/:uuid/childTags
	 * 
	 * @param userUuid
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pageingInfo
	 * @return
	 */
	public Page<Tag> findChildTags(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pageingInfo);

	/**
	 * Find all visible child contents for a given root tag and project. /tags/:uuid/childContents
	 * 
	 * @param userUuid
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pageingInfo
	 * @return
	 */
	public Page<Content> findChildContents(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pageingInfo);

	/**
	 * Find all visible tags that use this tag for tagging. /tags/:uuid/tags
	 * 
	 * @param userUuid
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pageingInfo
	 * @return
	 */
	public Page<Content> findTaggedContents(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pageingInfo);

}
