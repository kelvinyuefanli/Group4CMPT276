package com._6.group4.smartcart.auth;

/**
 * Shared session attribute keys used across controllers.
 * Centralised here to prevent typo bugs from duplicated string constants.
 */
public final class SessionKeys {
    private SessionKeys() {}

    public static final String USER_ID    = "USER_ID";
    public static final String USER_EMAIL = "USER_EMAIL";
    public static final String IS_ADMIN   = "IS_ADMIN";
}
