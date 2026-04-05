package com._6.group4.smartcart.instacart;

import com._6.group4.smartcart.auth.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/instacart")
public class InstacartShoppingListController {

    private static final ResponseEntity<?> UNAUTHORIZED =
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));

    private final InstacartShoppingListService instacartShoppingListService;

    public InstacartShoppingListController(InstacartShoppingListService instacartShoppingListService) {
        this.instacartShoppingListService = instacartShoppingListService;
    }

    @PostMapping("/shopping-list")
    public ResponseEntity<?> createShoppingList(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return UNAUTHORIZED;
        }

        try {
            InstacartShoppingListService.ShoppingListLinkResult result =
                    instacartShoppingListService.createShoppingListLinkForUser(userId);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("productsLinkUrl", result.productsLinkUrl());
            response.put("itemCount", result.itemCount());
            response.put("title", result.title());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", ex.getMessage()));
        } catch (InstacartGatewayException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    private Long getCurrentUserId(HttpSession session) {
        Object id = session != null ? session.getAttribute(SessionKeys.USER_ID) : null;
        if (id instanceof Long value) {
            return value;
        }
        if (id instanceof Number value) {
            return value.longValue();
        }
        return null;
    }
}
