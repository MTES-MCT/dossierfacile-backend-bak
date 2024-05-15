package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.service.DsnService;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
public class Names implements SaveStep<NamesForm> {

    private final TenantCommonRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentService documentService;
    private final TenantStatusService tenantStatusService;
    private final DsnService dnsService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, NamesForm namesForm) {
        // any change of first, last and preferred names triggers a document status update from validated -> to_process
        if (!StringUtils.equals(tenant.getFirstName(), namesForm.getFirstName())
                || !StringUtils.equals(tenant.getLastName(), namesForm.getLastName())
                || !StringUtils.equals(tenant.getPreferredName(), namesForm.getPreferredName())) {
            List<Document> documentsToCheck = new ArrayList<>(tenant.getDocuments());
            if (!CollectionUtils.isEmpty(tenant.getGuarantors())
                    && (tenant.getGuarantors().getFirst().getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON
                    || tenant.getGuarantors().getFirst().getTypeGuarantor() == TypeGuarantor.ORGANISM)) {
                documentsToCheck.addAll(tenant.getGuarantors().getFirst().getDocuments());
            }
            documentService.resetValidatedOrInProgressDocumentsAccordingCategories(documentsToCheck, Arrays.asList(DocumentCategory.values()));
        }
        if (!tenant.getFranceConnect()) {
            tenant.setFirstName(namesForm.getFirstName());
            tenant.setLastName(namesForm.getLastName());
        }
        tenant.setPreferredName(namesForm.getPreferredName());
        tenant.setZipCode(namesForm.getZipCode());
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenant = tenantStatusService.updateTenantStatus(tenant);

        // ici on va lancer la recuperation des informations de la DSN
        // Finallement dans le mappeur

        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
