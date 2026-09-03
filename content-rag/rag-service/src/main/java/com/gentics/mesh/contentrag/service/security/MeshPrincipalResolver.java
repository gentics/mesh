package com.gentics.mesh.contentrag.service.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loest Rollen gegen Mesh auf — Mesh bleibt damit die einzige Autoritaet fuer Berechtigungen,
 * so wie es fuer die Volltextsuche auch schon der Fall ist.
 *
 * <p>Zwei Aufrufe, beide mit dem Token des Anfragenden, nie mit einem Dienst-Token:
 * <ol>
 *   <li>{@code GET /api/v2/auth/me} liefert Benutzer, seine Gruppen und den
 *       {@code rolesHash},</li>
 *   <li>{@code GET /api/v2/groups/<uuid>/roles} je Gruppe die zugeordneten Rollen.</li>
 * </ol>
 *
 * <p>Der zweite Schritt ist der teure – er skaliert mit der Zahl der Gruppen. Mesh liefert
 * dafuer den {@code rolesHash}, ausdruecklich gedacht fuer genau dieses Caching: bleibt er
 * gleich, sind die Rollen unveraendert und die Gruppenaufrufe entfallen. Aendert sich etwas an
 * den Rollen des Benutzers, wechselt der Hash und wir loesen neu auf – ohne auf den Ablauf
 * einer Frist zu warten.
 *
 * <p>Ohne Token wird derselbe Weg ohne Authorization-Header gegangen: Mesh antwortet dann als
 * anonymer Benutzer, und wir erhalten dessen Rollen. Dadurch braucht es keine ausgedachte
 * „public"-Markierung im Index — anonym lesbare Inhalte tragen ohnehin die UUID der anonymen
 * Rolle in {@code _roleUuids}, genau wie in der Mesh-Suche.
 *
 * <p>Zwei Cache-Stufen: innerhalb der konfigurierten Frist wird Mesh gar nicht gefragt,
 * danach nur noch {@code /auth/me} – das validiert zugleich das Token. Der Cache-Schluessel ist
 * ein Hash des Tokens, nicht das Token selbst.
 */
@Component
public class MeshPrincipalResolver implements PrincipalResolver {

    private static final Logger log = LoggerFactory.getLogger(MeshPrincipalResolver.class);
    private static final String ANONYMOUS_KEY = "anonymous";

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String meshUrl;
    private final Duration ttl;
    private final int maxEntries;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * @param mapper     Jackson-ObjectMapper
     * @param properties RAG-Konfiguration (Mesh-URL, Cache-Einstellungen)
     */
    public MeshPrincipalResolver(ObjectMapper mapper, RagProperties properties) {
        this.mapper = mapper;
        this.meshUrl = trimTrailingSlash(properties.getMesh().getUrl());
        this.ttl = Duration.ofSeconds(properties.getAuth().getCacheTtlSeconds());
        this.maxEntries = properties.getAuth().getCacheMaxEntries();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public MeshPrincipal resolve(String token) {
        String key = token == null || token.isBlank() ? ANONYMOUS_KEY : Hashes.sha256(token);
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.principal();
        }

        // Frist abgelaufen: /auth/me validiert das Token und sagt ueber den rolesHash, ob sich
        // an den Rollen ueberhaupt etwas geaendert hat.
        JsonNode me = get("/api/v2/auth/me", token);
        String rolesHash = text(me, "rolesHash");

        MeshPrincipal principal;
        if (cached != null && rolesHash != null && rolesHash.equals(cached.rolesHash())) {
            principal = cached.principal();
        } else {
            principal = load(me, token);
        }

        if (cache.size() >= maxEntries) {
            // Grob, aber ausreichend: der Cache ist eine Latenz-Optimierung, kein Speicher.
            cache.clear();
        }
        cache.put(key, new CacheEntry(principal, rolesHash, Instant.now().plus(ttl)));
        return principal;
    }

    private MeshPrincipal load(JsonNode me, String token) {
        String userUuid = text(me, "uuid");
        String username = text(me, "username");
        boolean anonymous = token == null || token.isBlank();

        Set<String> roleUuids = new LinkedHashSet<>();
        for (String groupUuid : groupUuids(me)) {
            JsonNode roles = get("/api/v2/groups/" + groupUuid + "/roles", token);
            for (JsonNode role : roles.path("data")) {
                String uuid = text(role, "uuid");
                if (uuid != null) {
                    roleUuids.add(uuid);
                }
            }
        }

        if (roleUuids.isEmpty()) {
            // Kein Rollenkontext heisst: der Filter findet nichts. Fail-closed ist richtig,
            // aber es ist fast immer ein Konfigurationsfehler und soll sichtbar sein.
            log.warn("Keine Rollen fuer Benutzer {} ermittelt – es werden keine Inhalte gefunden.",
                    username == null ? userUuid : username);
        }
        return new MeshPrincipal(userUuid, username, List.copyOf(roleUuids), anonymous);
    }

    private List<String> groupUuids(JsonNode me) {
        List<String> uuids = new ArrayList<>();
        for (JsonNode group : me.path("groups")) {
            String uuid = text(group, "uuid");
            if (uuid != null) {
                uuids.add(uuid);
            }
        }
        return uuids;
    }

    private JsonNode get(String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(meshUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new MeshUnavailableException("Mesh nicht erreichbar: " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MeshUnavailableException("Rollenaufloesung unterbrochen: " + path, e);
        }

        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new InvalidTokenException("Mesh hat das Token abgelehnt (HTTP " + status + ")");
        }
        if (status >= 400) {
            throw new MeshUnavailableException(
                    "Mesh antwortete mit HTTP " + status + " auf " + path, null);
        }
        try {
            return mapper.readTree(response.body());
        } catch (IOException e) {
            throw new MeshUnavailableException("Antwort von Mesh nicht lesbar: " + path, e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String trimTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * @param principal aufgeloester Benutzer samt Rollen
     * @param rolesHash Mesh-Hashsumme der Rollen; entscheidet, ob die Gruppenaufrufe noetig sind
     * @param expiresAt bis wann Mesh gar nicht erst gefragt wird
     */
    private record CacheEntry(MeshPrincipal principal, String rolesHash, Instant expiresAt) {
    }
}
