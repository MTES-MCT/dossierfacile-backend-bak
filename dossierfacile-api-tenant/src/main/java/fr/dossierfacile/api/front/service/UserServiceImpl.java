package fr.dossierfacile.api.front.service;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.repository.PasswordRecoveryTokenRepository;
import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.service.interfaces.*;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.utils.TransactionalUtil;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final MailService mailService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final TenantMapper tenantMapper;
    private final TenantCommonRepository tenantRepository;
    private final LogService logService;
    private final KeycloakService keycloakService;
    private final UserApiService userApiService;
    private final PartnerCallBackService partnerCallBackService;
    private final ApartmentSharingService apartmentSharingService;
    private final TenantCommonService tenantCommonService;
    private final TenantMapperForMail tenantMapperForMail;

    @Override
    public TenantModel createPassword(User user, String password) {
        keycloakService.createKeyCloakPassword(user.getKeycloakId(), password);
        return tenantMapper.toTenantModel(tenantRepository.getReferenceById(user.getId()), null);
    }

    @Override
    public TenantModel createPassword(String token, String password) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new PasswordRecoveryTokenNotFoundException(token));

        // check if keycloak is correctly synchronised
        User user = passwordRecoveryToken.getUser();
        var keycloakId = keycloakService.getKeycloakId(user.getEmail());
        if (!StringUtils.equals(keycloakId, user.getKeycloakId())) {
            log.warn("Tenant keycloakId has been synchronized - user_id: " + user.getId());
            user.setKeycloakId(keycloakId);
            userRepository.save(user);
        }

        TenantModel tenantModel = createPassword(passwordRecoveryToken.getUser(), password);

        passwordRecoveryTokenRepository.delete(passwordRecoveryToken);
        return tenantModel;
    }

    @Override
    public void forgotPassword(String email) {
        Tenant tenant = tenantRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));

        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(tenant);
        mailService.sendEmailNewPassword(tenant, passwordRecoveryToken);
    }

    private List<TenantUserApi> groupingAllTenantUserApisInTheApartment(ApartmentSharing as) {
        List<TenantUserApi> tenantUserApis = new ArrayList<>();
        if (as.getTenants() != null && !as.getTenants().isEmpty()) {
            as.getTenants().stream()
                    .filter(t -> t.getTenantsUserApi() != null && !t.getTenantsUserApi().isEmpty())
                    .forEach(t -> tenantUserApis.addAll(t.getTenantsUserApi()));
        }
        return tenantUserApis;
    }

    @Override
    @Transactional
    public void deleteAccount(Tenant tenant) {
        List<ApplicationModel> webhookDTOList = new ArrayList<>();
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        groupingAllTenantUserApisInTheApartment(apartmentSharing).forEach((tenantUserApi) -> {
            UserApi userApi = tenantUserApi.getUserApi();
            webhookDTOList.add(partnerCallBackService.getWebhookDTO(tenant, userApi, PartnerCallBackType.DELETED_ACCOUNT));
        });
        logService.saveLogWithTenantData(LogType.ACCOUNT_DELETE, tenant);
        TenantDto tenantToDeleteDto = tenantMapperForMail.toDto(tenant);
        tenantCommonService.deleteTenantData(tenant);
        String keycloakId = tenant.getKeycloakId();

        if (tenant.getTenantType() == TenantType.CREATE) {
            for (Tenant coTenant : tenant.getApartmentSharing().getTenants().stream().filter(t -> t.getTenantType().equals(TenantType.JOIN)).collect(Collectors.toSet())) {
                deleteAccount(coTenant);
            }
            apartmentSharingService.delete(tenant.getApartmentSharing());
        } else {
            userRepository.delete(tenant);
            apartmentSharingService.removeTenant(tenant.getApartmentSharing(), tenant);
        }
        for (ApplicationModel webhookDTO : webhookDTOList) {
            partnerCallBackService.sendCallBack(tenant, webhookDTO.getUserApi(), webhookDTO);
        }

        TransactionalUtil.afterCommit(() -> {
            mailService.sendEmailAccountDeleted(tenantToDeleteDto);
            if (StringUtils.isNotBlank(keycloakId)) {
                keycloakService.deleteKeycloakUserById(keycloakId);
            }
        });
    }

    @Override
    @Transactional
    public Boolean deleteCoTenant(Tenant tenant, Long coTenantId) {
        if (tenant.getTenantType().equals(TenantType.CREATE)) {
            ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
            Optional<Tenant> coTenant = apartmentSharing.getTenants().stream()
                    .filter(t -> t.getId().equals(coTenantId) && t.getTenantType().equals(TenantType.JOIN)).findFirst();
            if (coTenant.isPresent()) {
                deleteAccount(coTenant.get());
                return true;
            }
        }
        return false;
    }

    @Override
    public void linkTenantToPartner(Tenant tenant, String partner, String internalPartnerId) {
        userApiService.findByName(partner)
                .ifPresent(userApi -> {
                    if (tenant.getApartmentSharing().getApplicationType() == ApplicationType.COUPLE) {
                        tenant.getApartmentSharing().getTenants()
                                .stream()
                                .forEach(t -> partnerCallBackService.registerTenant(
                                        (tenant.getId() == t.getId()) ? internalPartnerId : null, t, userApi));
                    } else {
                        partnerCallBackService.registerTenant(internalPartnerId, tenant, userApi);
                    }
                });
    }

    @Override
    public void logout(String keycloakId) {
        keycloakService.logout(keycloakId);
    }

    @Override
    public void unlinkFranceConnect(Tenant tenant) {
        User user = userRepository.findById(tenant.getId()).orElseThrow(IllegalArgumentException::new);
        user.setFranceConnect(false);
        userRepository.save(tenant);
        keycloakService.unlinkFranceConnect(tenant);
    }

}
