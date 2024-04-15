package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.InvitationTokenService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.ScheduledTasksService;
import fr.dossierfacile.api.front.service.interfaces.StatsService;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.ConfirmationTokenRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.OperationAccessTokenService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

//
// TODO Attention cela n'est pas compatible avec plusieurs instance !!!
//
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasksServiceImpl implements ScheduledTasksService {
    private final TenantCommonRepository tenantRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final OperationAccessTokenService operationAccessTokenService;
    private final MailService mailService;
    private final StatsService statsService;
    private final InvitationTokenService invitationTokenService;
    private final Object sendEmailLock = new Object();

    @Value("${days_for_email_account_validation_reminder}")
    private Long daysForEmailAccountValidationReminder;
    @Value("${days_for_account_completion_reminder}")
    private Long daysForAccountCompletionReminder;
    @Value("${days_for_account_declination_reminder}")
    private Long daysForAccountDeclinationReminder;
    @Value("${days_for_satisfaction_email}")
    private Long daysForSatisfactionEmail;

    @Scheduled(cron = "0 10 0 * * ?")
    @Override
    public void emailAccountValidationReminder() {
        synchronized (sendEmailLock) {
            try {
                LocalDateTime startDate = LocalDateTime.now().minusDays(daysForEmailAccountValidationReminder).with(LocalTime.MIN);
                LocalDateTime endDate = LocalDateTime.now().minusDays(daysForEmailAccountValidationReminder).with(LocalTime.MAX);

                List<Tenant> tenantsToNotificate = tenantRepository.findAllByEnabledIsFalseAndCreationDateTimeIsBetween(startDate, endDate);
                if (tenantsToNotificate != null && tenantsToNotificate.size() > 0) {
                    log.info(tenantsToNotificate.size() + " tenants found, to be notified because account email not yet validated after " + daysForEmailAccountValidationReminder + " days of account registration");
                    for (Tenant tenant : tenantsToNotificate) {
                        Optional<ConfirmationToken> optional = confirmationTokenRepository.findByUser(tenant);
                        if (optional.isPresent()) {
                            mailService.sendEmailWhenEmailAccountNotYetValidated(tenant, optional.get());
                            Thread.sleep(1000);// avoid to spam - softBounce
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage() + Sentry.captureException(e), e);
            }
        }
    }

    @Scheduled(cron = "0 40 0 * * ?")
    @Override
    public void accountCompletionReminder() {
        synchronized (sendEmailLock) {
            try {
                LocalDateTime startDate = LocalDateTime.now().minusDays(daysForAccountCompletionReminder).with(LocalTime.MIN);
                LocalDateTime endDate = LocalDateTime.now().minusDays(daysForAccountCompletionReminder).with(LocalTime.MAX);

                List<Tenant> tenantsToNotificate = tenantRepository.findAllByHonorDeclarationIsFalseAndCompletionDateTimeIsBetween(startDate, endDate);
                if (tenantsToNotificate != null && tenantsToNotificate.size() > 0) {
                    log.info(tenantsToNotificate.size() + " tenants found, to be notified because account not yet completed after " + daysForAccountCompletionReminder + " days of account email validation");
                    for (Tenant tenant : tenantsToNotificate) {
                        mailService.sendEmailWhenAccountNotYetCompleted(tenant);
                        Thread.sleep(1000);// avoid to spam - softBounce
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage() + Sentry.captureException(e), e);
            }
        }
    }

    @Scheduled(cron = "0 10 1 * * ?")
    @Override
    public void accountDeclinationReminder() {
        synchronized (sendEmailLock) {
            try {
                LocalDateTime startDate = LocalDateTime.now().minusDays(daysForAccountDeclinationReminder).with(LocalTime.MIN);
                LocalDateTime endDate = LocalDateTime.now().minusDays(daysForAccountDeclinationReminder).with(LocalTime.MAX);

                List<Tenant> tenantsToNotificate = tenantRepository.findAllDeclinedSinceXDaysAgo(startDate, endDate);
                if (tenantsToNotificate != null && tenantsToNotificate.size() > 0) {
                    log.info(tenantsToNotificate.size() + " tenants found, to be notified because account is still declination after " + daysForAccountCompletionReminder + " days of account declined");
                    for (Tenant tenant : tenantsToNotificate) {
                        if (StringUtils.isNotBlank(tenant.getEmail())) {
                            mailService.sendEmailWhenAccountIsStillDeclined(tenant);
                            Thread.sleep(1000);// avoid to spam - softBounce
                        }
                        if (tenant.getApartmentSharing().getApplicationType() == ApplicationType.COUPLE) {
                            tenant.getApartmentSharing().getTenants().stream().filter(user -> !Objects.equals(user.getId(), tenant.getId()) && StringUtils.isNotBlank(user.getEmail()) && user.getStatus() == TenantFileStatus.VALIDATED).forEach(mailService::sendEmailWhenAccountIsStillDeclined);
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage() + Sentry.captureException(e), e);
            }
        }
    }

    @Scheduled(cron = "0 40 1 * * ?")
    @Override
    public void satisfactionEmails() {
        synchronized (sendEmailLock) {
            try {
                LocalDateTime startDate = LocalDateTime.now().minusDays(daysForSatisfactionEmail).with(LocalTime.MIN);
                LocalDateTime endDate = LocalDateTime.now().minusDays(daysForSatisfactionEmail).with(LocalTime.MAX);

                List<Tenant> tenants = tenantRepository.findAllTenantsValidatedSinceXDaysAgo(startDate, endDate);
                log.info(tenants.size() + " tenants found, to be notified after " + daysForSatisfactionEmail + " days of account validation");
                for (Tenant tenant : tenants) {
                    if (tenant.getTenantsUserApi().size() > 0) {
                        mailService.sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(tenant);
                    } else {
                        mailService.sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(tenant);
                    }
                    Thread.sleep(1000);// avoid to spam - softBounce
                }
            } catch (Exception e) {
                log.error(e.getMessage() + Sentry.captureException(e), e);
            }
        }
    }

    @Scheduled(cron = "0 0 5 * * *")
    public void updateStats() {
        statsService.updateStats();
    }

    @Scheduled(cron = "0 0 * * * *")
    @Override
    public void updateOperationAccessTokenStatus() {
        synchronized (sendEmailLock) {
            List<OperationAccessToken> tokens = operationAccessTokenService.findExpiredToken();
            log.info(tokens.size() + " expired tokens found - delete the tokens and send notification");
            for (OperationAccessToken token : tokens) {
                if (StringUtils.isNotBlank(token.getEmail())) {
                    switch(token.getOperationAccessType()){
                        case INVITATION_TO_APARTMENT_SHARING -> {
                            Optional<Tenant> createTenant = token.getApartmentSharing().getTenants().stream().filter( t -> t.getTenantType() == TenantType.CREATE).findFirst();
                            createTenant.ifPresent(tenant -> mailService.sendEmailExpiredInvitation(token.getEmail(), tenant));
                        }
                        case DISPLAY_CLIENT_SECRET -> mailService.sendDefaultEmailExpiredToken(token.getEmail(), token);
                    }
                    ;
                }
                operationAccessTokenService.delete(token);
            }
        }
    }
}
