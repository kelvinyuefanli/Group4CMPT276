package com._6.group4.smartcart.instacart;

public class InstacartGatewayException extends RuntimeException {

    public InstacartGatewayException(String message) {
        super(message);
    }

    public InstacartGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
