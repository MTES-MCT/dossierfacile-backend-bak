package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.config.ApiVersion;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.CallbackLogRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.RequestService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerCallBackServiceImpl implements PartnerCallBackService {
    private final TenantCommonRepository tenantRepository;
    private final TenantUserApiRepository tenantUserApiRepository;
    private final ApplicationFullMapper applicationFullMapper;
    private final RequestService requestService;
    private final CallbackLogRepository callbackLogRepository;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final ObjectMapper objectMapper;

    public void registerTenant(String internalPartnerId, Tenant tenant, UserApi userApi) {
        Optional<TenantUserApi> optionalTenantUserApi = tenantUserApiRepository.findFirstByTenantAndUserApi(tenant, userApi);
        if (optionalTenantUserApi.isEmpty()) {
            TenantUserApi tenantUserApi = TenantUserApi.builder()
                    .id(new TenantUserApiKey(tenant.getId(), userApi.getId()))
                    .tenant(tenant)
                    .userApi(userApi)
                    .build();
            if (ApiVersion.V3.is(userApi.getVersion())) {
                if (internalPartnerId != null && !internalPartnerId.isEmpty()) {
                    if (tenantUserApi.getAllInternalPartnerId() == null) {
                        tenantUserApi.setAllInternalPartnerId(Collections.singletonList(internalPartnerId));
                    } else if (!tenantUserApi.getAllInternalPartnerId().contains(internalPartnerId)) {
                        tenantUserApi.getAllInternalPartnerId().add(internalPartnerId);
                    }
                }
            }
            tenantUserApiRepository.save(tenantUserApi);

            if (userApi.getVersion() != null && userApi.getUrlCallback() != null && (
                    tenant.getStatus() == TenantFileStatus.VALIDATED
                            || tenant.getStatus() == TenantFileStatus.TO_PROCESS)) {
                PartnerCallBackType partnerCallBackType = tenant.getStatus() == TenantFileStatus.VALIDATED ?
                        PartnerCallBackType.VERIFIED_ACCOUNT :
                        PartnerCallBackType.CREATED_ACCOUNT;
                ApplicationModel webhookDTO = getWebhookDTO(tenant, userApi, partnerCallBackType);
                sendCallBack(tenant, userApi, webhookDTO);
            }
        }
    }

    private List<UserApi> findAllUserApi(ApartmentSharing as) {
        return as.getTenants() == null ? Collections.emptyList() :
                as.getTenants().stream()
                        .flatMap(t -> t.getTenantsUserApi() == null ? Stream.empty() : t.getTenantsUserApi().stream())
                        .map(TenantUserApi::getUserApi)
                        .distinct()
                        .collect(Collectors.toList());
    }

    // TODO send callback should be transactionnal or have DTO
    @Transactional
    public void sendCallBack(Tenant tenant, PartnerCallBackType partnerCallBackType) {
        Optional<ApartmentSharing> apartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId());
        if (apartmentSharing.isEmpty()) {
            return;
        }
        List<ApplicationModel> applicationModelList = findAllUserApi(tenant.getApartmentSharing()).stream()
                .map(userApi -> getWebhookDTO(tenant, userApi, partnerCallBackType)).toList();

        TransactionalUtil.afterCommit(() -> {
            try {
                applicationModelList.forEach(model -> sendCallBack(tenant, model.getUserApi(), model));
            } catch (Exception e) {
                log.error("CAUTION Unable to send notification to partner", e);
            }
        });

    }

    @Override
    @Transactional
    public void sendCallBack(List<Tenant> tenantList, PartnerCallBackType partnerCallBackType) {
        if (tenantList != null && !tenantList.isEmpty()) {
            tenantList.forEach(t -> sendCallBack(t, partnerCallBackType));
        }
    }

    @SneakyThrows
    private void createCallbackLogForPartnerModel(Long tenantId, Long partnerId, TenantFileStatus tenantFileStatus, ApplicationModel applicationModel) {
        String jsonContent = objectMapper.writeValueAsString(applicationModel);
        callbackLogRepository.save(new CallbackLog(tenantId, partnerId, tenantFileStatus, jsonContent));
    }

    public void sendCallBack(Tenant tenant, UserApi userApi, ApplicationModel applicationModel) {
        if (userApi == null) {
            log.error("userApi should not be null");
            return;
        }
        log.info("Send Callback for " + tenant.getId() + " to" + userApi.getName());
        if (userApi.isDisabled() || StringUtils.isBlank(userApi.getUrlCallback())) {
            log.warn("UserApi call has not effect for " + userApi.getName());
            return;
        }
        requestService.send(applicationModel, userApi.getUrlCallback(), userApi.getPartnerApiKeyCallback());
        createCallbackLogForPartnerModel(tenant.getId(), userApi.getId(), tenant.getStatus(), applicationModel);
    }

    @Override
    public void sendRevokedAccessCallback(Tenant tenant, UserApi userApi) {
        ApplicationModel applicationModel = new ApplicationModel();
        applicationModel.setOnTenantId(tenant.getId());
        applicationModel.setPartnerCallBackType(PartnerCallBackType.ACCESS_REVOKED);
        applicationModel.setUserApi(userApi);

        sendCallBack(tenant, userApi, applicationModel);
    }

    @NotNull
    public ApplicationModel getWebhookDTO(Tenant tenant, UserApi userApi, PartnerCallBackType partnerCallBackType) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        ApplicationModel applicationModel = applicationFullMapper.toApplicationModel(apartmentSharing, userApi);
        applicationModel.setUserApi(userApi);

        List<Tenant> tenantList = tenantRepository.findAllByApartmentSharing(apartmentSharing);
        for (Tenant t : tenantList) {
            tenantUserApiRepository.findFirstByTenantAndUserApi(t, userApi).ifPresent(tenantUserApi -> {
                if (ApiVersion.V3.is(userApi.getVersion())) {
                    if (tenantUserApi.getAllInternalPartnerId() != null && !tenantUserApi.getAllInternalPartnerId().isEmpty()) {
                        applicationModel.getTenants().stream()
                                .filter(tenantObject -> Objects.equals(tenantObject.getId(), t.getId()))
                                .findFirst()
                                .ifPresent(tenantModel -> tenantModel.setAllInternalPartnerId(tenantUserApi.getAllInternalPartnerId()));
                    }
                }
            });
        }

        applicationModel.setPartnerCallBackType(partnerCallBackType);
        applicationModel.setOnTenantId(tenant.getId());
        return applicationModel;
    }
}
