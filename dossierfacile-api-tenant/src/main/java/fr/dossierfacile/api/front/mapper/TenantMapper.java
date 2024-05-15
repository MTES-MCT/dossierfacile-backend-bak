package fr.dossierfacile.api.front.mapper;

import com.google.errorprone.annotations.concurrent.LazyInit;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.model.tenant.ApartmentSharingModel;
import fr.dossierfacile.api.front.model.tenant.DocumentDeniedReasonsModel;
import fr.dossierfacile.api.front.model.tenant.DocumentModel;
import fr.dossierfacile.api.front.model.tenant.FileModel;
import fr.dossierfacile.api.front.model.tenant.SelectedOption;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.service.DsnService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.CategoriesMapper;
import fr.dossierfacile.common.mapper.MapDocumentCategories;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantMapper {
    private static final String PATH = "api/document/resource";
    private static final String PREVIEW_PATH = "/api/file/preview/";
    private static final String DOSSIER_PDF_PATH = "/api/application/fullPdf/";
    private static final String DOSSIER_PATH = "/file/";

    @Autowired
    @LazyInit
    protected DsnService dnsService;

    @Value("${application.domain}")
    protected String domain;

    @Value("${tenant.domain}")
    protected String tenantDomain;

    protected CategoriesMapper categoriesMapper;

    @Autowired
    public void setCategoriesMapper(CategoriesMapper categoriesMapper) {
        this.categoriesMapper = categoriesMapper;
    }

    @Mapping(target = "passwordEnabled", expression = "java(tenant.getPassword() != null)")
    @Mapping(target = "contracts", expression = "java(dnsService.contracts(tenant.getEmail()))")
    public abstract TenantModel toTenantModel(Tenant tenant);

    @Mapping(target = "name", expression = "java((document.getWatermarkFile() != null )? domain + \"/" + PATH + "/\" + document.getName() : null)")
    @MapDocumentCategories
    public abstract DocumentModel toDocumentModel(Document document);

    @Mapping(target = "name", expression = "java((document.getWatermarkFile() != null )? domain + \"/" + PATH + "/\" + document.getName() : null)")
    @Mapping(target = "documentCategory", expression = "java(categoriesMapper.mapCategory(document.getDocumentCategory()))")
    @Mapping(target = "documentSubCategory", expression = "java(categoriesMapper.mapSubCategory(document.getDocumentSubCategory()))")
    public abstract fr.dossierfacile.api.front.model.dfc.apartment_sharing.DocumentModel documentToDocumentModel(Document document);

    @Mapping(target = "connectedTenantId", source = "id")
    public abstract ConnectedTenantModel toTenantModelDfc(Tenant tenant);

    @Mapping(target = "preview", expression = "java((documentFile.getPreview() != null )? domain + \"" + PREVIEW_PATH + "\" + documentFile.getId() : null)")
    @Mapping(target = "size", source = "documentFile.storageFile.size")
    @Mapping(target = "contentType", source = "documentFile.storageFile.contentType")
    @Mapping(target = "originalName", source = "documentFile.storageFile.name")
    public abstract FileModel toFileModel(File documentFile);

    @AfterMapping
    void modificationsAfterMapping(@MappingTarget TenantModel.TenantModelBuilder tenantModelBuilder) {
        TenantModel tenantModel = tenantModelBuilder.build();
        ApartmentSharingModel apartmentSharingModel = tenantModel.getApartmentSharing();
        if (apartmentSharingModel.getStatus() == TenantFileStatus.VALIDATED) {
            apartmentSharingModel.setDossierPdfUrl(domain + DOSSIER_PDF_PATH + apartmentSharingModel.getToken());
            apartmentSharingModel.setDossierUrl(tenantDomain + DOSSIER_PATH + apartmentSharingModel.getToken());
        } else {
            apartmentSharingModel.setToken(null);
            apartmentSharingModel.setTokenPublic(null);
        }
        var isDossierUser = SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_dossier"));
        var filePath = isDossierUser ? "/api/file/resource/" : "/api-partner/tenant/" + tenantModel.getId() + "/file/resource/";
        setDocumentDeniedReasonsAndDocumentAndFilesRoutes(tenantModel.getDocuments(), filePath, false);

        tenantModel.getApartmentSharing().getTenants().stream().filter(t -> Objects.equals(t.getId(), tenantModel.getId())).forEach(
                t -> {
                    t.setDocuments(null);
                    t.setGuarantors(null);
                }
        );
        Optional.ofNullable(tenantModel.getApartmentSharing().getTenants())
                .ifPresent(coTenantModels -> coTenantModels.forEach(coTenantModel -> {
                    setDocumentDeniedReasonsAndDocumentAndFilesRoutes(coTenantModel.getDocuments(), filePath, true);
                    Optional.ofNullable(coTenantModel.getGuarantors())
                            .ifPresent(guarantorModels -> guarantorModels.forEach(guarantorModel -> setDocumentDeniedReasonsAndDocumentAndFilesRoutes(guarantorModel.getDocuments(), filePath, true)));
                }));

        Optional.ofNullable(tenantModel.getGuarantors())
                .ifPresent(guarantorModels -> guarantorModels.forEach(guarantorModel -> setDocumentDeniedReasonsAndDocumentAndFilesRoutes(guarantorModel.getDocuments(), filePath, false)));

       // tenantModel.setContracts();

    }

    private void setDocumentDeniedReasonsAndDocumentAndFilesRoutes(List<DocumentModel> list, String filePath, boolean previewOnly) {
        Optional.ofNullable(list)
                .ifPresent(documentModels -> documentModels.forEach(documentModel -> {
                    DocumentDeniedReasonsModel documentDeniedReasonsModel = documentModel.getDocumentDeniedReasons();
                    if (documentDeniedReasonsModel != null) {
                        List<SelectedOption> selectedOptionList = new ArrayList<>();
                        if (documentDeniedReasonsModel.isMessageData()) {
                            for (int i = 0; i < documentDeniedReasonsModel.getCheckedOptions().size(); i++) {
                                String checkedOption = documentDeniedReasonsModel.getCheckedOptions().get(i);
                                Integer checkedOptionsId = documentDeniedReasonsModel.getCheckedOptionsId().get(i);
                                selectedOptionList.add(SelectedOption.builder()
                                        .id(checkedOptionsId)
                                        .label(checkedOption).build());
                            }
                        } else {
                            for (int i = 0; i < documentDeniedReasonsModel.getCheckedOptions().size(); i++) {
                                String checkedOption = documentDeniedReasonsModel.getCheckedOptions().get(i);
                                selectedOptionList.add(SelectedOption.builder().id(null).label(checkedOption).build());
                            }
                        }
                        documentDeniedReasonsModel.setSelectedOptions(selectedOptionList);
                        documentDeniedReasonsModel.setCheckedOptions(null);
                        documentDeniedReasonsModel.setCheckedOptionsId(null);
                        documentModel.setDocumentDeniedReasons(documentDeniedReasonsModel);
                    }
                    Optional.ofNullable(documentModel.getFiles())
                            .ifPresent(fileModels -> fileModels.forEach(fileModel -> {
                                if (previewOnly) {
                                    fileModel.setPath("");
                                } else {
                                    fileModel.setPath(domain + filePath + fileModel.getId());
                                }
                            }));
                }));
    }

    @AfterMapping
    void modificationsAfterMapping(@MappingTarget ConnectedTenantModel.ConnectedTenantModelBuilder connectedTenantModelBuilder) {
        ConnectedTenantModel connectedTenantModel = connectedTenantModelBuilder.build();
        fr.dossierfacile.api.front.model.dfc.apartment_sharing.ApartmentSharingModel apartmentSharingModel = connectedTenantModel.getApartmentSharing();
        if (apartmentSharingModel.getStatus() == TenantFileStatus.VALIDATED) {
            apartmentSharingModel.setDossierPdfUrl(domain + DOSSIER_PDF_PATH + apartmentSharingModel.getToken());
            apartmentSharingModel.setDossierUrl(tenantDomain + DOSSIER_PATH + apartmentSharingModel.getToken());
        } else {
            apartmentSharingModel.setToken(null);
            apartmentSharingModel.setTokenPublic(null);
        }
        connectedTenantModel.getApartmentSharing().getTenants().forEach(tenantModel -> setDocumentDeniedReasonsAndDocumentRoutesForDFC(tenantModel.getDocuments()));
        connectedTenantModel.getApartmentSharing().getTenants().forEach(tenantModel ->
                Optional.ofNullable(tenantModel.getGuarantors()).ifPresent(guarantorModels ->
                        guarantorModels.forEach(guarantorModel -> setDocumentDeniedReasonsAndDocumentRoutesForDFC(guarantorModel.getDocuments()))));
    }

    private void setDocumentDeniedReasonsAndDocumentRoutesForDFC(List<fr.dossierfacile.api.front.model.dfc.apartment_sharing.DocumentModel> list) {
        Optional.ofNullable(list)
                .ifPresent(documentModels -> documentModels.forEach(documentModel -> {
                    fr.dossierfacile.api.front.model.dfc.apartment_sharing.DocumentDeniedReasonsModel documentDeniedReasonsModel = documentModel.getDocumentDeniedReasons();
                    if (documentDeniedReasonsModel != null) {
                        List<fr.dossierfacile.api.front.model.dfc.apartment_sharing.SelectedOption> selectedOptionList = new ArrayList<>();
                        if (documentDeniedReasonsModel.isMessageData()) {
                            for (int i = 0; i < documentDeniedReasonsModel.getCheckedOptions().size(); i++) {
                                String checkedOption = documentDeniedReasonsModel.getCheckedOptions().get(i);
                                Integer checkedOptionsId = documentDeniedReasonsModel.getCheckedOptionsId().get(i);
                                selectedOptionList.add(fr.dossierfacile.api.front.model.dfc.apartment_sharing.SelectedOption.builder()
                                        .id(checkedOptionsId)
                                        .label(checkedOption).build());
                            }
                        } else {
                            for (int i = 0; i < documentDeniedReasonsModel.getCheckedOptions().size(); i++) {
                                String checkedOption = documentDeniedReasonsModel.getCheckedOptions().get(i);
                                selectedOptionList.add(fr.dossierfacile.api.front.model.dfc.apartment_sharing.SelectedOption.builder()
                                        .id(null)
                                        .label(checkedOption).build());
                            }
                        }
                        documentDeniedReasonsModel.setSelectedOptions(selectedOptionList);
                        documentDeniedReasonsModel.setCheckedOptions(null);
                        documentDeniedReasonsModel.setCheckedOptionsId(null);
                        documentModel.setDocumentDeniedReasons(documentDeniedReasonsModel);
                    }
                }));
    }
}
