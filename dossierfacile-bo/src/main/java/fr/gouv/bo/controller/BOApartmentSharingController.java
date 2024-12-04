package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.bo.dto.DisplayableFile;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.dto.PartnerDTO;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
import fr.gouv.bo.service.ApartmentSharingService;
import fr.gouv.bo.service.TenantLogService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserApiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@AllArgsConstructor
@RequestMapping(value = "/bo/colocation")
@Slf4j
public class BOApartmentSharingController {

    private static final String EMAIL = "email";
    private static final String LOG_SER = "logser";
    private static final String TENANTS = "tenants";
    private static final String TENANT_SERVICE = "tenantService";
    private static final String MESSAGE_DTO = "messageDTO";
    private static final String PARTNERS_LIST = "partnersList";
    private static final String PARTNER_UPDATE = "partnerUpdate";
    private static final String TENANT_PRINCIPAL = "tenantPrincipal";
    private static final String APARTMENT_SHARING = "apartmentSharing";
    private static final String PARTNER_LIST_BY_TENANT = "partnerListByTenant";
    private static final String NOW = "now";
    private static final String FILES_BY_DOCUMENT = "filesByDocument";

    private final TenantService tenantService;
    private final ApartmentSharingService apartmentSharingService;
    private final ApartmentSharingLinkService apartmentSharingLinkService;
    private final UserApiService userApiService;
    private final TenantLogService logService;

    @GetMapping("")
    public String index() {
        return "bo/apartment-sharing";
    }

    @GetMapping("/{id}")
    public String view(Model model, @PathVariable("id") Long id) {

        MessageDTO messageDTO = new MessageDTO();
        PartnerDTO partnerDTO = new PartnerDTO();

        List<Tenant> tenants = tenantService.findAllTenantsByApartmentSharingAndReorderDocumentsByCategory(id);

        model.addAttribute(TENANTS, tenants);
        model.addAttribute(LOG_SER, logService);
        model.addAttribute(MESSAGE_DTO, messageDTO);
        model.addAttribute(PARTNER_UPDATE, partnerDTO);
        model.addAttribute(TENANT_PRINCIPAL, tenants.get(0));
        model.addAttribute(TENANT_SERVICE, tenantService);
        model.addAttribute(PARTNER_LIST_BY_TENANT, userApiService);
        model.addAttribute(PARTNERS_LIST, userApiService.getAllPartners());
        model.addAttribute(APARTMENT_SHARING, tenants.get(0).getApartmentSharing());
        model.addAttribute(NOW, LocalDateTime.now());
        model.addAttribute(FILES_BY_DOCUMENT, getFilesByDocument(tenants));

        return "bo/apartment-sharing-view";
    }

    @DeleteMapping("/{id}/tokens/")
    public String regenerateToken(Model model, @PathVariable("id") Long id) {
        apartmentSharingService.regenerateTokens(id);
        return "redirect:/bo/colocation/" + id ;
    }

    @DeleteMapping("/{id}/apartmentSharingLinks/{link_id}")
    public String deleteToken(Model model, @PathVariable("id") Long id, @PathVariable("link_id") Long linkId) {
        apartmentSharingLinkService.delete(linkId);
        return "redirect:/bo/colocation/" + id ;
    }

    private Map<Long, List<DisplayableFile>> getFilesByDocument(List<Tenant> tenants) {
        return tenants.stream()
                .flatMap(BOApartmentSharingController::getAllDocuments)
                .collect(Collectors.toMap(Document::getId, DisplayableFile::allOf));
    }

    private static Stream<Document> getAllDocuments(Tenant tenant) {
        Stream<Document> tenantDocuments = tenant.getDocuments().stream();
        Stream<Document> guarantorDocuments = tenant.getGuarantors().stream()
                .flatMap(guarantor -> guarantor.getDocuments().stream());
        return Stream.concat(tenantDocuments, guarantorDocuments);
    }

}
