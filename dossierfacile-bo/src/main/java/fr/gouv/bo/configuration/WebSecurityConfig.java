package fr.gouv.bo.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.dossierfacile.common.utils.LocalDateTimeTypeAdapter;
import fr.gouv.bo.security.BOQuotaAuthorizationManager;
import lombok.AllArgsConstructor;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAnyRole;
import static org.springframework.security.authorization.AuthorizationManagers.allOf;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableAsync
@EnableMethodSecurity(securedEnabled = true)
@AllArgsConstructor
public class WebSecurityConfig {

    private final BOQuotaAuthorizationManager quotaAuthorizationManager;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(new BOConnectionContextFilter(), FilterSecurityInterceptor.class)
                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()))
                .headers(headers -> headers
                        .contentTypeOptions(withDefaults())
                        .xssProtection(withDefaults())
                        .cacheControl(withDefaults())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors *.dossierfacile.fr *.dossierfacile.logement.gouv.fr; frame-src *.dossierfacile.fr *.dossierfacile.logement.gouv.fr; child-src 'none'; upgrade-insecure-requests; default-src 'none'; script-src 'self' 'unsafe-eval' 'unsafe-inline'; style-src 'self' fonts.googleapis.com 'unsafe-inline'; object-src *.dossierfacile.fr *.dossierfacile.logement.gouv.fr; img-src 'self' *.dossierfacile.fr *.dossierfacile.logement.gouv.fr data:; font-src 'self' fonts.gstatic.com; connect-src *.dossierfacile.fr *.dossierfacile.logement.gouv.fr; base-uri 'self'; media-src 'none'; worker-src *.dossierfacile.fr *.dossierfacile.logement.gouv.fr; manifest-src 'none';"))
                        .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable) // Scalingo force https and add this policy
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/login/auth/**", "/login/oauth2/**", "/actuator/health", "/assets/public/**")
                        .permitAll()
                        .requestMatchers("/bo/userApi", "/bo/userApi/**", "/bo/admin", "/bo/admin/**", "/bo/statistic/admin", "/bo/timeServeTenant", "/bo/users", "/bo/users/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/bo/tenant/{tenantId}/processFile")
                        .hasAnyRole("ADMIN", "OPERATOR", "PARTNER")
                        .requestMatchers("/bo/**", "/bo", "/bo/dashboard")
                        .hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/documents/**")
                        .access(allOf(hasAnyRole("ADMIN", "OPERATOR"), quotaAuthorizationManager))
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(new BOAccessDeniedHandler()))
                .oauth2Login(login -> login
                        .loginPage("/login")
                        .successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "JWT", "_csrf")
                );

        return http.build();
    }

    private CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setSessionAttributeName("_csrf");
        return repository;
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Mailer-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter());
        return builder.create();
    }

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
