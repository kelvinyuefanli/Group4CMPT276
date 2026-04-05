package com._6.group4.smartcart.instacart;

public interface InstacartGateway {

    InstacartShoppingListService.ProductsLinkResponse createProductsLink(
            InstacartShoppingListService.ProductsLinkRequest request
    );
}
