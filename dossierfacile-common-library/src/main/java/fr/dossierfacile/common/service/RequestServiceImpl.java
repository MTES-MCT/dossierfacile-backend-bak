package fr.dossierfacile.common.service;

import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.service.interfaces.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private static final String X_API_KEY = "x-api-key";
    private static final String CALL_BACK_RESPONSE = "CallBack ResponseStatus: {}";
    private final RestTemplate restTemplate;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void send(ApplicationModel applicationModel, String urlCallback, String partnerApiKeyCallback) {
        HttpHeaders headers = new HttpHeaders();
        if (partnerApiKeyCallback != null && !partnerApiKeyCallback.isEmpty()) {
            headers.set(X_API_KEY, partnerApiKeyCallback);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        sendRequest(new HttpEntity<>(applicationModel, headers), urlCallback);
    }

    private <T> void sendRequest(HttpEntity<T> request, String urlCallback) {
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.exchange(urlCallback, HttpMethod.POST, request, String.class);
            log.info(CALL_BACK_RESPONSE, response.getStatusCode());
        } catch (RestClientException e) {
            log.error("Error occurs during the call to :" + urlCallback, e);
            if (e instanceof ResourceAccessException) {
                try {
                    log.warn("Trying to send the request again without SSL Verification, to the urlCallBack: " + urlCallback);
                    addExceptionOfSecurity();
                    response = restTemplate.exchange(urlCallback, HttpMethod.POST, request, String.class);
                    log.info(CALL_BACK_RESPONSE, response.getStatusCode());
                } catch (GeneralSecurityException | ResourceAccessException e1) {
                    log.error("Error to sendRequest", e1);
                }
            }
        }
        if (response != null
                && HttpStatus.OK != response.getStatusCode()
                && HttpStatus.ACCEPTED != response.getStatusCode()
                && HttpStatus.NO_CONTENT != response.getStatusCode()) {
            log.error("Failure on partner callback url:" + urlCallback + "- Status:" + response.getStatusCode());
        }
    }

    private void addExceptionOfSecurity() throws GeneralSecurityException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(csf)
                        .build())
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        restTemplate.setRequestFactory(requestFactory);
    }
}
