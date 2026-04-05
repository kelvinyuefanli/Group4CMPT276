package com._6.group4.smartcart.instacart;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateInstacartGateway implements InstacartGateway {

    private static final String PRODUCTS_LINK_PATH = "/idp/v1/products/products_link";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;
    private final String baseUrl;

    public RestTemplateInstacartGateway(@Value("${instacart.api.key:}") String apiKey,
                                        @Value("${instacart.api.base-url:https://connect.dev.instacart.tools}") String baseUrl) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    @Override
    public InstacartShoppingListService.ProductsLinkResponse createProductsLink(
            InstacartShoppingListService.ProductsLinkRequest request
    ) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Instacart integration is not configured. Set INSTACART_API_KEY to enable shopping-list handoff.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<?> entity = new HttpEntity<>(request.toApiBody(), headers);

        try {
            ResponseEntity<InstacartShoppingListService.ProductsLinkResponse> response = restTemplate.postForEntity(
                    baseUrl + PRODUCTS_LINK_PATH,
                    entity,
                    InstacartShoppingListService.ProductsLinkResponse.class
            );
            return response.getBody();
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            int statusCode = ex.getStatusCode().value();
            if (body != null && !body.isBlank()) {
                throw new InstacartGatewayException(
                        "Instacart API returned " + statusCode + ": " + body,
                        ex
                );
            }
            throw new InstacartGatewayException(
                    "Instacart API returned " + statusCode + ".",
                    ex
            );
        } catch (ResourceAccessException ex) {
            throw new InstacartGatewayException("Could not reach Instacart. Please try again shortly.", ex);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl != null ? baseUrl.trim() : "";
        if (value.isBlank()) {
            return "https://connect.dev.instacart.tools";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
