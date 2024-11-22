package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import jakarta.annotation.Nullable;

import java.util.List;

public interface PartnerCallBackService {

    void registerTenant(String internalPartnerId, Tenant tenant, UserApi userApi);
    void sendCallBack(Tenant tenant, PartnerCallBackType partnerCallBackType);
    void sendCallBack(List<Tenant> tenantList, PartnerCallBackType partnerCallBackType);
    void sendCallBack(Tenant tenant, UserApi userApi, ApplicationModel applicationModel);
    void sendRevokedAccessCallback(Tenant tenant, UserApi userApi);

    @Nullable
    ApplicationModel getWebhookDTO(Tenant tenant, UserApi userApi, PartnerCallBackType partnerCallBackType);
}
