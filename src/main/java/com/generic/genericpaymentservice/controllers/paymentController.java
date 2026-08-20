package com.generic.genericpaymentservice.controllers;

import com.generic.genericpaymentservice.models.supabase.request;
import com.generic.genericpaymentservice.services.paymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1")
@Slf4j
public class paymentController {

    private final paymentService paymentService;

    @Autowired
    public paymentController(paymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(value = "/initiate-payment", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Object> initiatePayment(@RequestBody request req) {

        log.info("Incoming request: {}", req);
        return paymentService.makePayment(req);
    }
}
