package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.form.ContactForm;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;

public interface MailService {
    void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken);

    void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken);

    void sendEmailForFlatmates(User flatmate, User guest, PasswordRecoveryToken passwordRecoveryToken, ApplicationType applicationType);

    void sendEmailInvitationForExistingFlatmates(User flatmate, User guest, InvitationToken invitationToken, ApplicationType applicationType);

    void sendEmailAccountDeleted(User user);

    void sendEmailAccountCompleted(Tenant tenant);

    void sendEmailWhenEmailAccountNotYetValidated(User user, ConfirmationToken confirmationToken);

    void sendEmailWhenAccountNotYetCompleted(User user);

    void sendEmailWhenAccountIsStillDeclined(User user);

    void sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(User user);

    void sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(User user);

    void sendEmailToSupport(ContactForm form);

    void sendEmailWelcomeForPartnerUser(User user, UserApi userApi);

    void sendFileByMail(String url, String email, String tenantName, String fullName, String tenantEmail);

    void sendEmailPartnerAccessRevoked(Tenant receiver, UserApi userApi, Tenant revocationRequester);

    void sendDefaultEmailExpiredToken(String email, OperationAccessToken token);

    void sendEmailExpiredInvitation(String email, Tenant mainTenant);
}
