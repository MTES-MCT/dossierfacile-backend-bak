package fr.dossierfacile.api.front.service;

import com.google.common.base.Strings;
import fr.dossierfacile.api.front.form.ContactForm;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.utils.OptionalString;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailReplyTo;
import sibModel.SendSmtpEmailTo;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.dossierfacile.common.enums.ApplicationType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
    private final TransactionalEmailsApi apiInstance;
    @Value("${sendinblue.url.domain}")
    private String sendinBlueUrlDomain;
    @Value("${email.support}")
    private String emailSupport;
    @Value("${sendinblue.template.id.welcome}")
    private Long templateIDWelcome;
    @Value("${sendinblue.template.id.welcome.partner}")
    private Long templateIDWelcomePartner;
    @Value("${sendinblue.template.id.new.password}")
    private Long templateIdNewPassword;
    @Value("${sendinblue.template.id.invitation.couple}")
    private Long templateIdCoupleApplication;
    @Value("${sendinblue.template.id.invitation.group}")
    private Long templateIdGroupApplication;
    @Value("${sendinblue.template.id.account.deleted}")
    private Long templateIdAccountDeleted;
    @Value("${sendinblue.template.id.account.completed}")
    private Long templateIdAccountCompleted;
    @Value("${sendinblue.template.id.account.completed.with.partner}")
    private Long templateIdAccountCompletedWithPartner;
    @Value("${sendinblue.template.id.account.email.validation.reminder}")
    private Long templateEmailWhenEmailAccountNotYetValidated;
    @Value("${sendinblue.template.id.account.incomplete.reminder}")
    private Long templateEmailWhenAccountNotYetCompleted;
    @Value("${sendinblue.template.id.account.declined.reminder}")
    private Long templateEmailWhenAccountIsStillDeclined;
    @Value("${sendinblue.template.id.account.satisf.not.assoc.to.partners}")
    private Long templateEmailWhenTenantNOTAssociatedToPartnersAndValidated;
    @Value("${sendinblue.template.id.account.satisf.yes.assoc.to.partners}")
    private Long templateEmailWhenTenantYESAssociatedToPartnersAndValidated;
    @Value("${sendinblue.template.id.contact.support}")
    private Long templateIdContactSupport;
    @Value("${sendinblue.template.id.token.expiration}")
    private Long templateIdTokenExpiration;
    @Value("${sendinblue.template.id.share.file}")
    private Long templateIdShareFile;
    @Value("${sendinblue.template.id.partner.access.revoked}")
    private Long templateIDPartnerAccessRevoked;

    @Value("${sendinblue.template.id.application.couple.invitation.existing}")
    private Long templateIdCoupleApplicationInvitationForExistingTenant;
    @Value("${sendinblue.template.id.application.group.invitation.existing}")
    private Long templateIdGroupApplicationInvitationForExistingTenant;
    @Value("${sendinblue.template.id.application.invitation.expiration}")
    private Long templateIdApplicationInvitationExpired;
    @Value("${link.after.completed.default}")
    private String defaultCompletedUrl;
    @Value("${link.after.created.default}")
    private String defaultCreatedUrl;
    @Value("${link.accept.invitation.existing.tenant}")
    private String acceptInvitationUrlForExistingTenant;

    @Value("${sendinblue.domains.blacklist:example.com}")
    private List<String> blackListedDomains;

    private void sendEmailToTenant(String email,String tenantFullName, Map<String, String> params, Long templateId) {
        if (email == null) {
            return;
        }
        boolean blackListed = blackListedDomains.stream().anyMatch(domain -> email.endsWith(domain));
        if (blackListed) {
            return;
        }
        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(email);
        if (!Strings.isNullOrEmpty(tenantFullName)) {
            sendSmtpEmailTo.setName(tenantFullName);
        }
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateId);
        sendSmtpEmail.params(params);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email Api Exception" + Sentry.captureException(e), e);
        }
    }
    private void sendEmailToTenant(User tenant, Map<String, String> params, Long templateId) {
        sendEmailToTenant(tenant.getEmail(),tenant.getFullName(), params, templateId);
    }

    @Override
    public void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        sendEmailToTenant(user, variables, templateIDWelcome);
    }

    @Override
    public void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("newPasswordToken", passwordRecoveryToken.getToken());
        variables.put("PRENOM", user.getFirstName());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        sendEmailToTenant(user, variables, templateIdNewPassword);
    }

    @Override
    public void sendEmailForFlatmates(User flatmate, User guest, PasswordRecoveryToken passwordRecoveryToken, ApplicationType applicationType) {
        Map<String, String> variables = new HashMap<>();

        Long templateId = templateIdCoupleApplication;
        variables.put("PRENOM", flatmate.getFirstName());
        variables.put("confirmToken", passwordRecoveryToken.getToken());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);
        if (applicationType == GROUP) {
            variables.put("NOM", Strings.isNullOrEmpty(flatmate.getPreferredName()) ? flatmate.getLastName() : flatmate.getPreferredName());
            templateId = templateIdGroupApplication;
        }

        sendEmailToTenant(guest, variables, templateId);
    }

    @Override
    public void sendEmailInvitationForExistingFlatmates(User flatmate, User guest, InvitationToken invitationToken, ApplicationType applicationType) {
        Map<String, String> variables = new HashMap<>();

        Long templateId = switch (applicationType) {
            case COUPLE -> templateIdCoupleApplicationInvitationForExistingTenant;
            case GROUP -> templateIdGroupApplicationInvitationForExistingTenant;
            default -> throw new RuntimeException("Something wrong happened - cannot send invitation");
        };
        variables.put("tenantFirstName", flatmate.getFirstName());
        variables.put("tenantFullName", flatmate.getFullName());
        variables.put("tenantEmail", flatmate.getFullName());
        variables.put("acceptInvitationUrl", acceptInvitationUrlForExistingTenant + invitationToken.getToken());

        sendEmailToTenant(guest, variables, templateId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    @Override
    public void sendEmailAccountDeleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());

        sendEmailToTenant(user, variables, templateIdAccountDeleted);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    @Override
    public void sendEmailAccountCompleted(Tenant tenant) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", tenant.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(tenant.getPreferredName()) ? tenant.getLastName() : tenant.getPreferredName());

        if (tenant.isBelongToPartner()) {
            UserApi userApi = tenant.getTenantsUserApi().get(0).getUserApi();
            variables.put("partnerName", userApi.getName2());
            variables.put("logoUrl", userApi.getLogoUrl());
            variables.put("callToActionUrl", OptionalString.of(userApi.getCompletedUrl()).orElse(defaultCompletedUrl));

            sendEmailToTenant(tenant, variables, templateIdAccountCompletedWithPartner);
        } else {
            variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);
            sendEmailToTenant(tenant, variables, templateIdAccountCompleted);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    @Override
    public void sendEmailWhenEmailAccountNotYetValidated(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        sendEmailToTenant(user, variables, templateEmailWhenEmailAccountNotYetValidated);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    @Override
    public void sendEmailWhenAccountNotYetCompleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        sendEmailToTenant(user, variables, templateEmailWhenAccountNotYetCompleted);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    @Override
    public void sendEmailWhenAccountIsStillDeclined(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        sendEmailToTenant(user, variables, templateEmailWhenAccountIsStillDeclined);
    }

    @Override
    public void sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        sendEmailToTenant(user, variables, templateEmailWhenTenantNOTAssociatedToPartnersAndValidated);
    }

    @Override
    public void sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        sendEmailToTenant(user, variables, templateEmailWhenTenantYESAssociatedToPartnersAndValidated);
    }

    @Override
    public void sendEmailToSupport(ContactForm form) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstname", form.getFirstname());
        variables.put("lastname", form.getLastname());
        variables.put("email", form.getEmail());
        variables.put("profile", form.getProfile());
        variables.put("subject", form.getSubject());
        variables.put("message", form.getMessage());

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(emailSupport);
        sendSmtpEmailTo.setName("Support depuis formulaire");

        SendSmtpEmailReplyTo sendSmtpEmailReplyTo = new SendSmtpEmailReplyTo();
        sendSmtpEmailReplyTo.setEmail(form.getEmail());

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdContactSupport);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));
        sendSmtpEmail.replyTo(sendSmtpEmailReplyTo);

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    @Override
    public void sendEmailWelcomeForPartnerUser(User user, UserApi userApi) {
        Map<String, String> variables = new HashMap<>();
        variables.put("partnerName", userApi.getName2());
        variables.put("logoUrl", userApi.getLogoUrl());
        variables.put("callToActionUrl", OptionalString.of(userApi.getWelcomeUrl()).orElse(defaultCreatedUrl));

        sendEmailToTenant(user, variables, templateIDWelcomePartner);
    }

    @Override
    public void sendFileByMail(String url, String email, String tenantName, String fullName, String tenantEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("url", sendinBlueUrlDomain + url);
        variables.put("tenantName", tenantName);
        variables.put("tenantEmail", tenantEmail);
        variables.put("fullName", fullName);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(email);
        SendSmtpEmailReplyTo sendSmtpEmailReplyTo = new SendSmtpEmailReplyTo();
        sendSmtpEmailReplyTo.setEmail(tenantEmail);

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdShareFile);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.replyTo(sendSmtpEmailReplyTo);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email Api Exception" + Sentry.captureException(e), e);
        }
    }

    @Override
    public void sendEmailPartnerAccessRevoked(Tenant receiver, UserApi userApi, Tenant revocationRequester) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", receiver.getFirstName());
        variables.put("partnerName", userApi.getName2());
        variables.put("requestOrigin", new RevocationRequest(revocationRequester, receiver).getOrigin());

        sendEmailToTenant(receiver, variables, templateIDPartnerAccessRevoked);
    }

    @Override
    public void sendDefaultEmailExpiredToken(String email, OperationAccessToken token) {
        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(email);

        Map<String, String> params = new HashMap<>();
        params.put("operation", token.getOperationAccessType().name());
        params.put("createdDate", token.getCreatedDate().format(DateTimeFormatter.ISO_DATE));

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdTokenExpiration);
        sendSmtpEmail.params(params);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email Api Exception" + Sentry.captureException(e), e);
        }
    }

    private record RevocationRequest(Tenant requester, Tenant emailReceiver) {

        String getOrigin() {
            if (requester.getId().equals(emailReceiver.getId())) {
                return "votre demande";
            }
            ApplicationType applicationType = requester.getApartmentSharing().getApplicationType();
            String requesterType = applicationType == COUPLE ? "conjoint(e)" : "colocataire";
            return String.format("la demande de votre %s %s", requesterType, requester.getFirstName());
        }

    }

}
