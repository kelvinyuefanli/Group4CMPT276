package com._6.group4.smartcart.grocery;

public record PantryItemUpdateRequest(
        String name,
        String canonicalName,
        Double quantity,
        String unit,
        Boolean covered
) {
}
