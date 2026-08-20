package com.generic.genericpaymentservice.services;

import com.generic.genericpaymentservice.models.supabase.request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;

@Service
@Slf4j
public class paymentService {

    @Value("${daraja.passkey}")
    private String darajaPasskey;

    @Value("${daraja.url}")
    private String darajaUrl;

    @Value("${daraja.consumer-key}")
    private String consumerKey;

    @Value("${daraja.consumer-secret}")
    private String consumerSecret;

    private String getAccessToken() {
        String credentials = consumerKey + ":" + consumerSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedCredentials);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials",
                HttpMethod.GET,
                entity,
                Map.class
        );
        log.info("get token response {}", response);

        return response.getBody().get("access_token").toString();
    }

    public ResponseEntity<Object> makePayment(request supabaseReq) {
        com.generic.genericpaymentservice.models.daraja.request darajaReq = new com.generic.genericpaymentservice.models.daraja.request();

        darajaReq.setBusinessShortCode(supabaseReq.getBusinessShortCode());

        // Generate timestamp: yyyyMMddHHmmss
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        darajaReq.setTimestamp(timestamp);

        String shortCode = supabaseReq.getBusinessShortCode();

        String password = Base64.getEncoder().encodeToString(
                (shortCode + darajaPasskey + timestamp).getBytes(StandardCharsets.UTF_8)
        );
        darajaReq.setPassword(password);

        darajaReq.setTransactionType("CustomerPayBillOnline");
        darajaReq.setAmount(supabaseReq.getAmount());
        darajaReq.setPartyA(supabaseReq.getPhoneNumber());
        darajaReq.setPartyB(supabaseReq.getBusinessShortCode());
        darajaReq.setPhoneNumber(supabaseReq.getPhoneNumber());
        darajaReq.setCallBackURL(supabaseReq.getCallbackUrl());
        darajaReq.setAccountReference(supabaseReq.getReference());
        darajaReq.setTransactionDesc(supabaseReq.getDescription());

        // Call the external API
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String accessToken;
        try {
            accessToken = getAccessToken();
            // TEMP diagnostic logging — remove once the 404.001.03 issue is confirmed fixed.
            log.info("Using access token: {}", accessToken);
            log.info("Posting STK push to daraja.url: {}", darajaUrl);
            headers.setBearerAuth(accessToken);
        } catch (Exception e) {
            log.error("Failed to generate access token", e);
            return ResponseEntity.internalServerError().body("Error generating access token: " + e.getMessage());
        }

        HttpEntity<com.generic.genericpaymentservice.models.daraja.request> requestEntity = new HttpEntity<>(darajaReq, headers);

        log.info("Outgoing request: {}", darajaReq);

        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(darajaUrl, requestEntity, Object.class);
            return response;
        } catch (Exception e) {
            log.error("Daraja STK push failed", e);
            return ResponseEntity.internalServerError().body("Error processing payment: " + e.getMessage());
        }
    }
}