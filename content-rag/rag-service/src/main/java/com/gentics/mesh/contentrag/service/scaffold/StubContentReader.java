package com.gentics.mesh.contentrag.service.scaffold;

import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.SourceContent;
import com.gentics.mesh.contentrag.common.spi.ContentReader;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * SCAFFOLD-Platzhalter fuer den Mesh-Content-Reader (Konzept 5.1/5.3).
 * Spaeter: GraphQL-Read mit gezielter Feldauswahl, HTML-&gt;Markdown, Referenzaufloesung (V7/V8).
 */
@Component
public class StubContentReader implements ContentReader {

    @Override
    public Optional<SourceContent> read(DocumentGroupId groupId) {
        throw new UnsupportedOperationException(
                "SCAFFOLD: ContentReader (Mesh-GraphQL + Normalisierung) noch nicht implementiert.");
    }
}
