package com.enonic.app.ga.rest.auth;

public class GoogleAnalyticsAuthenticationResult
{
    private String token;

    private String errorMessage;

    public GoogleAnalyticsAuthenticationResult( final String token, final String errorMessage )
    {
        this.token = token == null ? "" : token;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public String getToken()
    {
        return token;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }
}
