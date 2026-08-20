package com.generic.genericpaymentservice.models.supabase;

import lombok.Data;

@Data
public class request {
    private String phoneNumber;
    private String businessShortCode;
    private Integer amount;
    private String currency;
    private String reference;
    private String description;
    private String callbackUrl;
}
