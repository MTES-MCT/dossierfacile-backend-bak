package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.MessageStatus;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.Role;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.dto.CustomMessage;
import fr.gouv.bo.dto.DisplayableFile;
import fr.gouv.bo.dto.GuarantorItem;
import fr.gouv.bo.dto.ItemDetail;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.dto.MessageItem;
import fr.gouv.bo.dto.PartnerDTO;
import fr.gouv.bo.dto.TenantInfoHeader;
import fr.gouv.bo.service.DocumentDeniedReasonsService;
import fr.gouv.bo.service.DocumentService;
import fr.gouv.bo.service.TenantLogService;
import fr.gouv.bo.service.MessageService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserApiService;
import fr.gouv.bo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/bo/tenant")
@Slf4j
public class BOTenantController {

    private static final String EMAIL = "email";
    private static final String TENANT = "tenant";
    private static final String GUARANTOR = "guarantor";
    private static final String NEW_MESSAGE = "newMessage";
    private static final String HEADER = "header";
    private static final String REDIRECT_BO = "redirect:/bo";
    private static final String CUSTOM_MESSAGE = "customMessage";
    private static final String CLARIFICATION = "clarification";
    private static final String OPERATOR_COMMENT = "operatorComment";
    private static final String REDIRECT_ERROR = "redirect:/error";

    private final TenantService tenantService;
    private final MessageService messageService;
    private final DocumentService documentService;
    private final UserApiService userApiService;
    private final PartnerCallBackService partnerCallBackService;
    private final UserService userService;
    private final TenantLogService logService;
    private final DocumentDeniedReasonsService documentDeniedReasonsService;

    @GetMapping("/{id}")
    public String getTenant(@PathVariable Long id) {
        Tenant tenant = tenantService.findTenantById(id);
        if (tenant != null) {
            return redirectToTenantPage(tenant);
        }
        throw new NotFoundException("Tenant is not found. Still exists?");
    }

    @GetMapping("/setAsTenantCreate/{id}")
    public String setAsTenantCreate(@PathVariable Long id) {
        Tenant tenant = userService.setAsTenantCreate(tenantService.findTenantById(id));
        return redirectToTenantPage(tenant);
    }

    @GetMapping("/deleteCoTenant/{id}")
    public String deleteCoTenant(@PathVariable Long id, Principal principal) {
        Tenant tenant = tenantService.findTenantById(id);
        if (tenant.getTenantType() == TenantType.CREATE) {
            throw new IllegalArgumentException("Delete main tenant is not allowed - set another user as main tenant before OR delete the entire apartmentSharing");
        }
        BOUser operator = userService.findUserByEmail(principal.getName());
        userService.deleteCoTenant(tenant, operator);
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId();
    }

    @GetMapping("/deleteApartmentSharing/{id}")
    public String deleteApartmentSharing(@PathVariable("id") Long id, Principal principal) {
        Tenant create = tenantService.findTenantById(id);
        BOUser operator = userService.findUserByEmail(principal.getName());
        userService.deleteApartmentSharing(create, operator);
        return REDIRECT_BO;
    }

    @PostMapping("/partner/{id}")
    public String sendCallbackToPartner(@PathVariable("id") Long id, PartnerDTO partnerDTO) {
        Tenant tenant = tenantService.find(id);

        UserApi userApi = userApiService.findById(partnerDTO.getPartner());
        PartnerCallBackType partnerCallBackType = tenant.getStatus() == TenantFileStatus.VALIDATED ?
                PartnerCallBackType.VERIFIED_ACCOUNT :
                PartnerCallBackType.CREATED_ACCOUNT;
        ApplicationModel webhookDTO = partnerCallBackService.getWebhookDTO(tenant, userApi, partnerCallBackType);
        partnerCallBackService.sendCallBack(tenant, userApi, webhookDTO);

        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId();
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<Void> validateTenantFile(@PathVariable("id") Long tenantId, Principal principal) {
        tenantService.validateTenantFile(principal, tenantId);
        return ok().build();
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> declineTenantFile(@PathVariable("id") Long tenantId, Principal principal) {
        tenantService.declineTenant(principal, tenantId);
        return ok().build();
    }

    @PostMapping("/{id}/customMessage")
    public String customEmail(@PathVariable("id") Long tenantId, CustomMessage customMessage, Principal principal) {
        return tenantService.customMessage(principal, tenantId, customMessage);
    }

    @GetMapping("/delete/document/{id}")
    public String deleteDocument(@PathVariable("id") Long id, Principal principal) {
        User operator = userService.findUserByEmail(principal.getName());
        Tenant tenant = tenantService.deleteDocument(id, operator);
        return redirectToTenantPage(tenant);
    }

    @GetMapping("/status/{id}")
    public String changeStatusOfDocument(@PathVariable("id") Long id, MessageDTO messageDTO, Principal principal) {
        User operator = userService.findUserByEmail(principal.getName());
        Tenant tenant = tenantService.changeDocumentStatus(id,messageDTO, operator);

        return redirectToTenantPage(tenant);
    }
    private void checkPartnerRights(Tenant tenant, Principal principal){
        BOUser operator = userService.findUserByEmail(principal.getName());
        if (operator.getUserRoles().stream().anyMatch( r -> r.getRole() == Role.ROLE_PARTNER)
                && tenant.getTenantsUserApi().stream().noneMatch( apiUser -> apiUser.getUserApi().getId().equals(operator.getExclusivePartnerId()))){
            log.error("Access Denied");
            throw new AccessDeniedException("Access denied");
        }
    }
    @GetMapping("/{id}/processFile")
    public String processFileForm(Model model, @PathVariable("id") Long id, Principal principal) throws IOException {
        Tenant tenant = tenantService.find(id);

        checkPartnerRights(tenant, principal);
        if (tenant == null) {
            log.error("BOTenantController processFile not found tenant with id : {}", id);
            return REDIRECT_ERROR;
        }
        TenantInfoHeader header = TenantInfoHeader.build(tenant,
                userApiService.findPartnersLinkedToTenant(id),
                logService.getLogByTenantId(id));
        model.addAttribute(HEADER, header);
        model.addAttribute(NEW_MESSAGE, findNewMessageFromTenant(id));
        model.addAttribute(TENANT, tenant);
        model.addAttribute(CUSTOM_MESSAGE, getCustomMessage(tenant));
        model.addAttribute(CLARIFICATION, splitInParagraphs(tenant.getClarification()));
        model.addAttribute(OPERATOR_COMMENT, splitInParagraphs(tenant.getOperatorComment()));
        return "bo/process-file";
    }

    @GetMapping("/delete/guarantor/{guarantorId}")
    public String deleteGuarantor(@PathVariable("guarantorId") Long guarantorId, Principal principal) {
        User operator = userService.findUserByEmail(principal.getName());
        Tenant tenant = tenantService.deleteGuarantor(guarantorId, operator);
        return redirectToTenantPage(tenant);
    }

    private Boolean findNewMessageFromTenant(Long id) {

        User tenant1 = tenantService.getUserById(id);
        List<Message> messages = messageService.findTenantMessages(tenant1);
        for (Message message : messages) {
            if (message.getMessageStatus().equals(MessageStatus.UNREAD) && message.getFromUser() != null)
                return true;
        }
        return false;
    }

    @PostMapping("/{id}/processFile")
    public String processFile(@PathVariable("id") Long id, CustomMessage customMessage, Principal principal) {
        Tenant tenant = tenantService.find(id);
        checkPartnerRights(tenant, principal);
        tenantService.processFile(id, customMessage, principal);
        return tenantService.redirectToApplication(principal, null);
    }

    @PostMapping("/{id}/comment")
    public String addOperatorComment(@PathVariable("id") Long tenantId, @RequestParam String comment) {
        Tenant tenant = tenantService.addOperatorComment(tenantId, comment);
        return redirectToTenantPage(tenant);
    }

    private static String redirectToTenantPage(Tenant tenant) {
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
    }

    private List<ItemDetail> getItemDetailForSubcategoryOfDocument(DocumentSubCategory documentSubCategory, String tenantOrGuarantor) {

        List<ItemDetail> itemDetails = new ArrayList<>();
        for (DocumentDeniedOptions documentDeniedOptions : documentService.findDocumentDeniedOptionsByDocumentSubCategoryAndDocumentUserType(documentSubCategory, tenantOrGuarantor)) {
            ItemDetail itemDetail1 = ItemDetail.builder()
                    .check(false)
                    .message(documentDeniedOptions.getMessageValue())
                    .idOptionMessage(documentDeniedOptions.getId())
                    .build();
            itemDetails.add(itemDetail1);
        }
        return itemDetails;
    }

    private CustomMessage getCustomMessage(Tenant tenant) {

        CustomMessage customMessage = new CustomMessage();

        List<Document> documents = tenant.getDocuments();
        documents.sort(Comparator.comparing(Document::getDocumentCategory));
        for (Document document : documents) {
            if (document.getDocumentStatus().equals(DocumentStatus.TO_PROCESS)) {
                customMessage.getMessageItems().add(MessageItem.builder()
                        .monthlySum(document.getMonthlySum())
                        .customTex(document.getCustomText())
                        .avisDetected(document.getAvisDetected())
                        .documentCategory(document.getDocumentCategory())
                        .documentSubCategory(document.getDocumentSubCategory())
                        .itemDetailList(getItemDetailForSubcategoryOfDocument(document.getDocumentSubCategory(), TENANT))
                        .documentId(document.getId())
                        .documentName(document.getName())
                        .analyzedFiles(DisplayableFile.onlyAnalyzedFilesOf(document))
                        .previousDeniedReasons(documentDeniedReasonsService.getLastDeniedReason(document, tenant).orElse(null))
                        .documentAnalysisReport(document.getDocumentAnalysisReport())
                        .analysisReportComment(document.getDocumentAnalysisReport() != null && (DocumentAnalysisStatus.DENIED == document.getDocumentAnalysisReport().getAnalysisStatus())  ? document.getDocumentAnalysisReport().getComment() : null)
                        .build());
            }
        }

        for (Guarantor guarantor : tenant.getGuarantors()) {
            GuarantorItem guarantorItem = GuarantorItem.builder()
                    .guarantorId(guarantor.getId())
                    .typeGuarantor(guarantor.getTypeGuarantor())
                    .firstName(guarantor.getFirstName())
                    .lastName(guarantor.getLastName())
                    .legalPersonName(guarantor.getLegalPersonName())
                    .build();

            documents = guarantor.getDocuments();
            documents.sort(Comparator.comparing(Document::getDocumentCategory));
            for (Document document : documents) {
                if (document.getDocumentStatus().equals(DocumentStatus.TO_PROCESS)) {
                    guarantorItem.getMessageItems().add(MessageItem.builder()
                            .monthlySum(document.getMonthlySum())
                            .avisDetected(document.getAvisDetected())
                            .customTex(document.getCustomText())
                            .documentCategory(document.getDocumentCategory())
                            .documentSubCategory(document.getDocumentSubCategory())
                            .itemDetailList(getItemDetailForSubcategoryOfDocument(document.getDocumentSubCategory(), GUARANTOR))
                            .documentId(document.getId())
                            .documentName(document.getName())
                            .analyzedFiles(DisplayableFile.onlyAnalyzedFilesOf(document))
                            .documentAnalysisReport(document.getDocumentAnalysisReport())
                            .analysisReportComment(document.getDocumentAnalysisReport() != null && (DocumentAnalysisStatus.DENIED == document.getDocumentAnalysisReport().getAnalysisStatus())  ? document.getDocumentAnalysisReport().getComment() : null)
                            .previousDeniedReasons(documentDeniedReasonsService.getLastDeniedReason(document, tenant).orElse(null))
                            .build());
                }
            }
            customMessage.getGuarantorItems().add(guarantorItem);
        }
        return customMessage;
    }

    private static List<String> splitInParagraphs(String string) {
        if (string == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(string.split("\n"))
                .filter(paragraph -> !paragraph.isBlank())
                .toList();
    }

}
