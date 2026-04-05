package com._6.group4.smartcart.instacart;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstacartShoppingListControllerTest {

    private InstacartShoppingListService service;
    private InstacartShoppingListController controller;

    @BeforeEach
    void setUp() {
        service = mock(InstacartShoppingListService.class);
        controller = new InstacartShoppingListController(service);
    }

    @Test
    void createShoppingList_requiresAuthentication() {
        ResponseEntity<?> response = controller.createShoppingList(new MockHttpSession());

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createShoppingList_returnsProductsLinkUrlAndItemCount() {
        when(service.createShoppingListLinkForUser(42L)).thenReturn(
                new InstacartShoppingListService.ShoppingListLinkResult(
                        "https://example.com/list",
                        3,
                        "SmartCart Grocery List"
                )
        );

        ResponseEntity<?> response = controller.createShoppingList(session(42L));

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("https://example.com/list", body.get("productsLinkUrl"));
        assertEquals(3, body.get("itemCount"));
        assertEquals("SmartCart Grocery List", body.get("title"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createShoppingList_returnsServiceUnavailableWhenInstacartIsNotConfigured() {
        when(service.createShoppingListLinkForUser(42L)).thenThrow(
                new IllegalStateException("Instacart integration is not configured.")
        );

        ResponseEntity<?> response = controller.createShoppingList(session(42L));

        assertEquals(503, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Instacart integration is not configured.", body.get("error"));
    }

    private HttpSession session(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", userId);
        return session;
    }
}
