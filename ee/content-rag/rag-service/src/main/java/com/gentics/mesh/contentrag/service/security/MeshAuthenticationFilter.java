package com.gentics.mesh.contentrag.service.security;

import com.gentics.mesh.contentrag.service.config.RagProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Setzt vor jedem API-Aufruf den {@link MeshPrincipal} aus dem mitgelieferten Mesh-Token.
 *
 * <p>Token-Quellen, in dieser Reihenfolge: {@code Authorization: Bearer <token>} und das
 * Mesh-Cookie {@code mesh.token}. Fehlt beides, laeuft die Anfrage als anonymer Mesh-Benutzer
 * — sofern das konfiguriert erlaubt ist.
 *
 * <p>Der Filter setzt bewusst nur ein Request-Attribut, statt Spring Security einzuziehen: es
 * gibt genau eine Autorisierungsentscheidung im System, und die trifft Elasticsearch anhand der
 * Rollen. Eine Rollen- oder URL-Matrix waere ein zweiter Ort, an dem dieselbe Frage beantwortet
 * wird, und damit ein zweiter Ort, an dem sie falsch beantwortet werden kann.
 */
@Component
public class MeshAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MeshAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";
    private static final String MESH_COOKIE = "mesh.token";

    private final PrincipalResolver resolver;
    private final RagProperties.Auth config;

    /**
     * @param resolver   loest das Token gegen Mesh auf
     * @param properties RAG-Konfiguration
     */
    public MeshAuthenticationFilter(PrincipalResolver resolver, RagProperties properties) {
        this.resolver = resolver;
        this.config = properties.getAuth();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health, Metriken und Info bleiben frei – sie liefern keine Inhalte aus.
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null && !config.isAllowAnonymous()) {
            deny(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Anmeldung erforderlich: Mesh-Token als Bearer-Token oder mesh.token-Cookie.");
            return;
        }

        try {
            MeshPrincipal principal = resolver.resolve(token);
            request.setAttribute(MeshPrincipal.ATTRIBUTE, principal);
        } catch (InvalidTokenException e) {
            log.debug("Token abgelehnt: {}", e.getMessage());
            deny(response, HttpServletResponse.SC_UNAUTHORIZED, "Ungueltiges Mesh-Token.");
            return;
        } catch (MeshUnavailableException e) {
            log.warn("Rollenaufloesung fehlgeschlagen: {}", e.getMessage());
            deny(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Berechtigungen koennen derzeit nicht geprueft werden.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (MESH_COOKIE.equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private static void deny(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
