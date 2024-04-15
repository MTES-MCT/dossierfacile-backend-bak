package fr.dossierfacile.api.front.register.tenant;

import com.google.common.annotations.VisibleForTesting;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PasswordRecoveryTokenService;
import fr.dossierfacile.api.front.service.interfaces.UserRoleService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class Application implements SaveStep<ApplicationFormV2> {

    private final TenantCommonRepository tenantRepository;
    private final UserRoleService userRoleService;
    private final TenantMapper tenantMapper;
    private final MailService mailService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final LogService logService;
    private final KeycloakService keycloakService;
    private final ApartmentSharingService apartmentSharingService;
    private final PartnerCallBackService partnerCallBackService;
    private final UserService userService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, ApplicationFormV2 applicationForm) {
        List<Tenant> oldCoTenant = tenant.getApartmentSharing().getTenants()
                .stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .collect(Collectors.toList());

        List<Tenant> tenantToDelete = oldCoTenant.stream()
                .filter(t -> applicationForm.getCoTenants().parallelStream()
                        .noneMatch(currentTenant -> t.getFirstName().equals(currentTenant.getFirstName())
                                && t.getLastName().equals(currentTenant.getLastName())
                                && (StringUtils.isBlank(t.getEmail()) || t.getEmail().equals(currentTenant.getEmail()))))
                .collect(Collectors.toList());

        List<CoTenantForm> tenantToCreate = applicationForm.getCoTenants().stream()
                .filter(currentTenant -> oldCoTenant.parallelStream()
                        .noneMatch(oldTenant -> oldTenant.getFirstName().equals(currentTenant.getFirstName())
                                && oldTenant.getLastName().equals(currentTenant.getLastName())
                                && (StringUtils.isBlank(oldTenant.getEmail()) || oldTenant.getEmail().equals(currentTenant.getEmail()))))
                .collect(Collectors.toList());

        List<Pair<Tenant, String>> tenantWitNewEmailToUpdate = applicationForm.getCoTenants().stream()
                .map(currentTenant -> {
                            Optional<Tenant> updatedTenant = oldCoTenant.parallelStream()
                                    .filter(oldTenant ->
                                            oldTenant.getFirstName().equals(currentTenant.getFirstName())
                                                    && oldTenant.getLastName().equals(currentTenant.getLastName())
                                                    && !StringUtils.equals(oldTenant.getEmail(), currentTenant.getEmail())
                                                    && oldTenant.getEmail() == null
                                    ).findFirst();
                            if (updatedTenant.isEmpty()) {
                                return null;
                            }
                            return new ImmutablePair<>(updatedTenant.get(), currentTenant.getEmail());
                        }
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        linkEmailToTenants(tenant, tenantWitNewEmailToUpdate);

        return saveStep(tenant, applicationForm.getApplicationType(), tenantToDelete, tenantToCreate);
    }

    @VisibleForTesting
    protected void linkEmailToTenants(Tenant tenantCreate, List<Pair<Tenant, String>> tenantWitNewEmailToUpdate) {
        if (tenantWitNewEmailToUpdate != null) {
            //TODO remove this block for allowing existing account invitation
            List<String> emailsExistTenants = tenantWitNewEmailToUpdate.stream()
                    .map(Pair::getRight)
                    .filter(tenantRepository::existsByEmail)
                    .collect(Collectors.toList());

            if (!emailsExistTenants.isEmpty())
                throw new IllegalArgumentException("Cannot update a tenant with an existing email: " + String.join(",", emailsExistTenants));
            //TODO allow tenant

            //tenantWitNewEmailToUpdate
            for (Pair<Tenant, String> pair : tenantWitNewEmailToUpdate) {
                Tenant t = pair.getLeft();
                String newEmail = pair.getRight();

                if (StringUtils.isNotBlank(newEmail)) {
                    // TODO do something if tenant exists
                    String keycloakId = keycloakService.createKeycloakUser(newEmail);
                    Tenant existingTenant = tenantRepository.findByKeycloakId(keycloakId);
                    if (existingTenant != null) {
                        // A tenant already exists, should never happen here because we have already checked existing email
                        String msg = "Cannot update a tenant with an existing email: " + String.join(",", emailsExistTenants);
                        log.error(msg + Sentry.captureMessage(msg));
                        throw new IllegalArgumentException(msg);
                    }

                    t.setKeycloakId(keycloakId);
                    t.setEmail(newEmail);
                    userRoleService.createRole(t);
                    tenantRepository.save(t);

                    sendInvitationToJoin(tenantCreate, t, tenantCreate.getApartmentSharing().getApplicationType());
                }
            }
        }
    }
    private void sendInvitationToJoin(Tenant tenantCreate, Tenant tenantToJoin, ApplicationType applicationType){
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(tenantToJoin);
        mailService.sendEmailForFlatmates(tenantCreate, tenantToJoin, passwordRecoveryToken, applicationType);
    }

    TenantModel saveStep(Tenant tenant, ApplicationType newApplicationType, List<Tenant> tenantToDelete, List<CoTenantForm> tenantToCreate) {

        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        ApplicationType currentApplicationType = apartmentSharing.getApplicationType();
        apartmentSharing.setApplicationType(newApplicationType);
        apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);

        deleteCoTenants(tenantToDelete);
        logService.saveApplicationTypeChangedLog(apartmentSharing.getTenants(), currentApplicationType, newApplicationType);
        createCoTenants(tenant, tenantToCreate, apartmentSharing);

        LocalDateTime now = LocalDateTime.now();
        tenant.lastUpdateDateProfile(now, null);
        tenantRepository.save(tenant);
        return tenantMapper.toTenantModel(tenant);
    }

    private void createCoTenants(Tenant tenantCreate, List<CoTenantForm> tenants, ApartmentSharing apartmentSharing) {

        // TODO if tenant exist you have to send an invitation - on doit faire un nouveau flow avec
        // TODO ALONE avec des invitations en pending -> quand les invitations sont acceptées, l'utilisateur passe en couple ou en colocation
        // TODO pour le couple c'est peut etre différent car il a le droit d'administrer complement le dossier
        // TODO ca risque donc de poser problème par exemple s'ils ont deja completement rempli leur dossier chacun dans leur coin que se passe-t'il
        // TODO il faudra bloquer la honor declaration tant qu'il y a des invitation en pending? même s'ils ont deja tout les 2 validés...
        // TODO Que se passe-t'il si l'invité n'accepte jamais l'invitation et qu'elle expire? Le tenant invitant redevient seul?

        // check if email account exist
        // Currently we cannot add existing user
        List<String> emailsExistTenants = tenants.stream()
                .map(CoTenantForm::getEmail)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .filter(StringUtils::isNotBlank)
                .filter(tenantRepository::existsByEmail)
                .collect(Collectors.toList());

        if (!emailsExistTenants.isEmpty()) {
            // on adopte le principe de la substitution - on va donc toujours avoir un utilisateur fictif (il convient de l'identifié pour pouvoir le supprimer)

            throw new IllegalArgumentException("Cannot add tenant with existing emails " + String.join(",", emailsExistTenants));


        }
        Set<Tenant> joinTenants = tenants.stream().map(
                tenant -> {
                    Tenant joinTenant = Tenant.builder()
                            .tenantType(TenantType.JOIN)
                            .firstName(tenant.getFirstName())
                            .lastName(tenant.getLastName())
                            .preferredName(tenant.getPreferredName())
                            .email(StringUtils.isBlank(tenant.getEmail()) ? null : tenant.getEmail())
                            .apartmentSharing(apartmentSharing)
                            .build();
                    if (apartmentSharing.getApplicationType() == ApplicationType.COUPLE) {
                        joinTenant.setHonorDeclaration(tenantCreate.getHonorDeclaration());
                    }
                    tenantRepository.save(joinTenant);

                    if (apartmentSharing.getApplicationType() == ApplicationType.COUPLE) {
                        if (!CollectionUtils.isEmpty(tenantCreate.getTenantsUserApi())) {
                            tenantCreate.getTenantsUserApi().stream()
                                    .forEach(tenantUserApi -> {
                                        partnerCallBackService.registerTenant(null, joinTenant, tenantUserApi.getUserApi());
                                    });
                        }
                    }

                    if (StringUtils.isNotBlank(tenant.getEmail())) {
                        // create keycloak user
                        String newKeycloakId = keycloakService.createKeycloakUser(tenant.getEmail());
                        Tenant existingTenant = tenantRepository.findByKeycloakId(newKeycloakId);
                        if (existingTenant != null) {
                            // A tenant already exists, should never happen here because we cannot add existing user
                            tenantRepository.delete(joinTenant);
                            String msg = "Cannot add a cotenant with an existing account: " + String.join(",", emailsExistTenants);
                            log.error(msg + Sentry.captureMessage(msg));
                            throw new IllegalArgumentException(msg);
                        }
                        joinTenant.setKeycloakId(newKeycloakId);
                        userRoleService.createRole(joinTenant);
                        tenantRepository.save(joinTenant);

                        sendInvitationToJoin(tenantCreate, joinTenant, tenantCreate.getApartmentSharing().getApplicationType());
                    }

                    // Relating all the clients related to tenant CREATE to tenant JOIN
                    Optional.ofNullable(tenantCreate.getTenantsUserApi())
                            .orElse(new ArrayList<>())
                            .forEach(tenantUserApi -> partnerCallBackService.registerTenant(null, joinTenant, tenantUserApi.getUserApi()));

                    logService.saveLog(LogType.ACCOUNT_CREATED, joinTenant.getId());
                    return joinTenant;
                }
        ).collect(Collectors.toSet());

        apartmentSharing.getTenants().addAll(joinTenants);
        apartmentSharingRepository.save(apartmentSharing);
    }

    private void deleteCoTenants(List<Tenant> tenantToDelete) {
        tenantToDelete.forEach(userService::deleteAccount);
    }

}
