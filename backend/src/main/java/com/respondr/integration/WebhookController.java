package com.respondr.integration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookIngestionService webhookIngestionService;

    public WebhookController(WebhookIngestionService webhookIngestionService) {
        this.webhookIngestionService = webhookIngestionService;
    }

    @PostMapping("/{integrationKey}")
    public ResponseEntity<WebhookIngestionService.WebhookResponse> receiveWebhook(
            @PathVariable String integrationKey,
            @RequestHeader(value = "X-Signature", required = false) String sig1,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String sig2,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String sig3,
            @RequestBody String payload) {

        String signature = sig1 != null ? sig1 : (sig2 != null ? sig2 : sig3);
        WebhookIngestionService.WebhookResponse response =
                webhookIngestionService.processWebhook(integrationKey, signature, payload);

        return ResponseEntity.ok(response);
    }
}
