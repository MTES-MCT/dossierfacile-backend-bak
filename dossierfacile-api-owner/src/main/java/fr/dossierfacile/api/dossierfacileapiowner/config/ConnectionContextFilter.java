package fr.dossierfacile.api.dossierfacileapiowner.config;

import fr.dossierfacile.common.config.AbstractConnectionContextFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;

public class ConnectionContextFilter extends AbstractConnectionContextFilter {

    private static final String EMAIL = "email";

    @Override
    public boolean isRequestIgnored(HttpServletRequest request) {
        return false;
    }

    @Override
    public Map<String, String> getAdditionalContextElements() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null || !(authentication.getPrincipal() instanceof Jwt principal)) {
            return Map.of();
        }
        Map<String, String> map = new TreeMap<>();
        map.put(EMAIL, principal.getClaimAsString("email"));
        return map;
    }

}