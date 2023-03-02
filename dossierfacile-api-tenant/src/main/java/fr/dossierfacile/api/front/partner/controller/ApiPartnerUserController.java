package fr.dossierfacile.api.front.partner.controller;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.api.front.aop.annotation.MethodLog;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.EmailExistsModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.partner.EmailExistsForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@RestController
@AllArgsConstructor
@RequestMapping("/api-partner")
@Validated
@MethodLog
public class ApiPartnerUserController {

    private final TenantService tenantService;
    private final TenantMapper tenantMapperForPartner;
    private final UserApiService userApiService;
    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;


    @GetMapping(value = "/email/{email}/tenant")
    public ResponseEntity<TenantModel> getTenantByEmail(@JsonDeserialize(using = EmailDeserializer.class) @PathVariable String email) {
        Optional<Tenant> tenant = tenantService.findByEmail(email);
        if (!tenant.isPresent()) {
            return status(HttpStatus.NOT_FOUND).build();
        }
        // access rules
        Optional<UserApi> userApi = this.userApiService.findByName(authenticationFacade.getKeycloakClientId());
        boolean hasReadAccess = userApiService.anyTenantIsAssociated(userApi.get(), Collections.singletonList(tenant.get()));
        if (!hasReadAccess) {
            return status(HttpStatus.UNAUTHORIZED).build();
        }
        return ok(tenantMapperForPartner.toTenantModel(tenant.get()));
    }

    /**
     * @deprecated use /email/{email}/tenant endpoint
     */
    @Deprecated
    @PostMapping(value = "/emailexists", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailExistsModel> emailExists(@Validated @RequestBody EmailExistsForm emailExistsForm) {
        return ok(tenantService.emailExists(emailExistsForm));
    }

    @DeleteMapping("/tenant/{tenantId}/user/deleteAccount")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        userService.deleteAccount(tenant);
        return ok().build();
    }
}
