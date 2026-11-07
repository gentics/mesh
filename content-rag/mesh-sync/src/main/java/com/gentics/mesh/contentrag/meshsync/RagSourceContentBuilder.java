package com.gentics.mesh.contentrag.meshsync;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.PublishStatus;
import com.gentics.mesh.contentrag.common.model.SourceContent;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Baut aus einem Node-Container den {@link SourceContent} fuer den RAG-Index.
 *
 * <p>Die Klasse ist die Mesh-Seite der Feldauswahl: sie holt die Auswahl fuer die Schemaversion,
 * wandelt nur die freigegebenen Felder nach Markdown und liefert {@code null}, wenn nichts uebrig
 * bleibt. {@code null} ist dabei ein normales Ergebnis und bedeutet fuer den Handler „Document-Group
 * abraeumen" – so wirkt die No-RAG-Konvention (K4) auch rueckwirkend.
 *
 * <p>Bewusst kein Rueckgriff auf {@code NodeContainerTransformer}: der baut das Node-Dokument der
 * Volltextsuche (ein Dokument je Container, Felder als JSON-Struktur, HTML als Plaintext). Fuer RAG
 * wird derselbe Container anders gelesen – nur freigegebene Felder, als Markdown, je Feldpfad
 * getrennt. Gemeinsam bleibt die Permission-Logik, damit K3 („dieselbe Filterlogik") stimmt.
 *
 * <p>Die Klasse ist zustandslos; der Selector bringt seinen eigenen Cache mit.
 */
public class RagSourceContentBuilder {

    /** Schutz gegen Zyklen beim Aufstieg zur Basenode. */
    private static final int MAX_PATH_DEPTH = 64;

    private final RagFieldSelector selector;
    private final String sourceUrlTemplate;

    /**
     * @param selector          Feldauswahl
     * @param sourceUrlTemplate Vorlage fuer die Quellen-URL mit den Platzhaltern
     *                          <code>{project}</code>, <code>{branch}</code>,
     *                          <code>{language}</code>, <code>{path}</code> und
     *                          <code>{uuid}</code>; {@code null} laesst die URL leer
     */
    public RagSourceContentBuilder(RagFieldSelector selector, String sourceUrlTemplate) {
        this.selector = selector;
        this.sourceUrlTemplate = sourceUrlTemplate;
    }

    /**
     * Baut den Quellinhalt einer Document-Group.
     *
     * @param container Node-Container (eine Sprachversion)
     * @param project   Projekt
     * @param branch    Branch
     * @param type      Container-Typ; im MVP indexiert der Handler nur {@link ContainerType#PUBLISHED}
     * @return Quellinhalt oder {@code null}, wenn dieser Container keinen RAG-Inhalt liefert
     */
    public SourceContent build(HibNodeFieldContainer container, HibProject project, HibBranch branch,
                               ContainerType type) {
        Tx tx = Tx.get();
        ContentDao contentDao = tx.contentDao();
        NodeDao nodeDao = tx.nodeDao();

        HibNode node = contentDao.getNode(container);
        HibSchemaVersion version = contentDao.getSchemaContainerVersion(container);
        List<FieldSchema> fields = version.getSchema().getFields();

        RagSchemaSelection selection = selector.selectionFor(
                RagSchemaDescriptors.describe(version.getName(), version.getUuid(), version.getSchema()));
        if (selection.isEmpty()) {
            return null;
        }

        RagFieldTexts texts = new RagFieldTexts(selection);
        for (FieldSchema field : fields) {
            if (texts.accepts(field.getName())) {
                collect(container, field, field.getName(), texts);
            }
        }
        if (texts.isEmpty()) {
            return null;
        }

        String language = contentDao.getLanguageTag(container);
        DocumentGroupId group = new DocumentGroupId(node.getUuid(), branch.getName(), language);
        String path = pathOf(contentDao, nodeDao, node, branch, type, language);

        return new SourceContent(
                group,
                project.getUuid(),
                project.getName(),
                branch.getUuid(),
                version.getName(),
                contentDao.getDisplayFieldValue(container),
                path,
                sourceUrl(project, branch, language, path, node),
                texts.toMarkdownByField(),
                meshVersion(container),
                type == PUBLISHED ? PublishStatus.PUBLISHED : PublishStatus.DRAFT,
                null,                                   // validFrom: Gueltigkeitszeitraum ist V1
                null,                                   // validTo
                instant(node.getCreationTimestamp()),
                instant(container.getLastEditedTimestamp()),
                roleUuids(tx.roleDao(), node, type),
                referencedNodes(container, fields),
                editorialTags(tx.tagDao(), node, branch));
    }

    /**
     * Liest ein Feld und uebergibt seinen Text an den Sammler.
     *
     * @param container Container, in dem das Feld liegt (Node-Container oder Micronode)
     * @param field     Feldschema
     * @param path      Feldpfad im Chunk-Dokument
     * @param texts     Sammler
     */
    private void collect(HibFieldContainer container, FieldSchema field, String path, RagFieldTexts texts) {
        FieldTypes type = FieldTypes.valueByName(field.getType());
        if (type == null) {
            return;
        }
        String name = field.getName();
        switch (type) {
            case STRING -> {
                HibStringField value = container.getString(name);
                if (value != null) {
                    texts.add(path, HtmlToMarkdown.plain(value.getString()));
                }
            }
            case HTML -> {
                HibHtmlField value = container.getHtml(name);
                if (value != null) {
                    texts.add(path, HtmlToMarkdown.convert(value.getHTML()));
                }
            }
            case BINARY -> {
                HibBinaryField value = container.getBinary(name);
                if (value != null) {
                    texts.add(path, HtmlToMarkdown.plain(value.getPlainText()));
                }
            }
            case S3BINARY -> {
                S3HibBinaryField value = container.getS3Binary(name);
                if (value != null) {
                    texts.add(path, HtmlToMarkdown.plain(value.getPlainText()));
                }
            }
            case MICRONODE -> {
                HibMicronodeField value = container.getMicronode(name);
                if (value != null) {
                    micronode(value.getMicronode(), path, texts);
                }
            }
            case LIST -> list(container, field, path, texts);
            default -> {
                // number, boolean, date und node tragen keinen Fliesstext.
            }
        }
    }

    private void list(HibFieldContainer container, FieldSchema field, String path, RagFieldTexts texts) {
        if (!(field instanceof ListFieldSchema list)) {
            return;
        }
        String name = field.getName();
        switch (list.getListType()) {
            case "string" -> {
                HibStringFieldList values = container.getStringList(name);
                if (values != null) {
                    int index = 0;
                    for (HibStringField item : values.getList()) {
                        texts.add(RagFieldPaths.append(path, String.valueOf(index++)),
                                HtmlToMarkdown.plain(item.getString()));
                    }
                }
            }
            case "html" -> {
                HibHtmlFieldList values = container.getHTMLList(name);
                if (values != null) {
                    int index = 0;
                    for (HibHtmlField item : values.getList()) {
                        texts.add(RagFieldPaths.append(path, String.valueOf(index++)),
                                HtmlToMarkdown.convert(item.getHTML()));
                    }
                }
            }
            case "micronode" -> {
                HibMicronodeFieldList values = container.getMicronodeList(name);
                if (values != null) {
                    int index = 0;
                    for (HibMicronodeField item : values.getList()) {
                        micronode(item.getMicronode(), RagFieldPaths.append(path, String.valueOf(index++)), texts);
                    }
                }
            }
            default -> {
                // node-, date-, number- und boolean-Listen tragen keinen Fliesstext.
            }
        }
    }

    /**
     * Steigt in einen Micronode ab. Die Freigabe erbt er vom aeusseren Schemafeld – nur dieses steht
     * im Schema. Ein ausdrueckliches {@code rag: false} oder {@code noIndex} am Micro-Feld gilt
     * trotzdem.
     */
    private void micronode(HibMicronode micronode, String path, RagFieldTexts texts) {
        if (micronode == null) {
            return;
        }
        for (FieldSchema field : micronode.getSchemaContainerVersion().getSchema().getFields()) {
            if (RagFieldSelector.isExcluded(RagSchemaDescriptors.describe(field))) {
                continue;
            }
            collect(micronode, field, RagFieldPaths.append(path, field.getName()), texts);
        }
    }

    /**
     * Rollen mit Leserecht, identisch zu {@code _roleUuids} der Mesh-Suche: {@code READ_PERM}, bei
     * veroeffentlichten Containern zusaetzlich {@code READ_PUBLISHED_PERM}. Ein Chunk ohne diese
     * Liste wird auf der Leseseite nie gefunden (fail-closed).
     */
    private static List<String> roleUuids(RoleDao roleDao, HibNode node, ContainerType type) {
        Set<String> uuids = new LinkedHashSet<>(roleDao.getRoleUuidsForPerm(node, READ_PERM));
        if (type == PUBLISHED) {
            uuids.addAll(roleDao.getRoleUuidsForPerm(node, READ_PUBLISHED_PERM));
        }
        return new ArrayList<>(uuids);
    }

    private static List<String> editorialTags(TagDao tagDao, HibNode node, HibBranch branch) {
        List<String> tags = new ArrayList<>();
        for (HibTag tag : tagDao.getTags(node, branch).list()) {
            tags.add(tag.getTagFamily().getName() + ":" + tag.getName());
        }
        return tags;
    }

    /** Sammelt die UUIDs referenzierter Nodes – als Metadatum, ohne den Inhalt aufzuloesen (V7). */
    private static List<String> referencedNodes(HibFieldContainer container, List<FieldSchema> fields) {
        Set<String> uuids = new LinkedHashSet<>();
        for (FieldSchema field : fields) {
            FieldTypes type = FieldTypes.valueByName(field.getType());
            if (type == FieldTypes.NODE) {
                HibNodeField value = container.getNode(field.getName());
                if (value != null && value.getNode() != null) {
                    uuids.add(value.getNode().getUuid());
                }
            } else if (type == FieldTypes.LIST && field instanceof ListFieldSchema list
                    && "node".equals(list.getListType())) {
                HibNodeFieldList values = container.getNodeList(field.getName());
                if (values != null) {
                    for (HibNodeField item : values.getList()) {
                        if (item.getNode() != null) {
                            uuids.add(item.getNode().getUuid());
                        }
                    }
                }
            }
        }
        return new ArrayList<>(uuids);
    }

    /**
     * Baut den Pfad aus den Segmenten bis zur Basenode.
     *
     * <p>{@code NodeDao#getPath(...)} waere die naheliegende Wahl, braucht aber einen
     * {@code ActionContext} – im Sync-Pfad gibt es keinen. {@code ContentDao#getPathSegment(...)}
     * kommt ohne aus; der Pfad-Prefix der Branch bleibt dabei aussen vor.
     */
    private static String pathOf(ContentDao contentDao, NodeDao nodeDao, HibNode node, HibBranch branch,
                                 ContainerType type, String language) {
        Deque<String> segments = new ArrayDeque<>();
        HibNode current = node;
        for (int depth = 0; current != null && depth < MAX_PATH_DEPTH; depth++) {
            String segment = contentDao.getPathSegment(current, branch.getUuid(), type, true, language);
            if (segment == null) {
                return null;
            }
            if (!segment.isEmpty()) {
                segments.addFirst(segment);
            }
            current = nodeDao.getParentNode(current, branch.getUuid());
        }
        return "/" + String.join("/", segments);
    }

    private String sourceUrl(HibProject project, HibBranch branch, String language, String path, HibNode node) {
        if (sourceUrlTemplate == null || sourceUrlTemplate.isBlank()) {
            return null;
        }
        return sourceUrlTemplate
                .replace("{project}", project.getName())
                .replace("{branch}", branch.getName())
                .replace("{language}", language == null ? "" : language)
                .replace("{path}", path == null ? "" : path)
                .replace("{uuid}", node.getUuid());
    }

    private static String meshVersion(HibNodeFieldContainer container) {
        return container.getVersion() == null ? container.getElementVersion() : container.getVersion().toString();
    }

    private static Instant instant(Long timestamp) {
        return timestamp == null ? null : Instant.ofEpochMilli(timestamp);
    }
}
