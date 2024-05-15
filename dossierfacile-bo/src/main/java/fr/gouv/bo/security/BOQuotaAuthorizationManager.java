package fr.gouv.bo.security;

import fr.gouv.bo.service.QuotaService;
import lombok.AllArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@AllArgsConstructor
public class BOQuotaAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final QuotaService quotaService;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        String endpointPath = object.getRequest().getRequestURI();
        boolean accessGranted = true ; // quotaService.checkQuota(authentication.get().getName(), endpointPath);
        return new AuthorizationDecision(accessGranted);
    }

}
