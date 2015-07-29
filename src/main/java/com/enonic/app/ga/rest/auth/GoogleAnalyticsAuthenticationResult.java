package com.enonic.app.ga.rest.auth;

public class GoogleAnalyticsAuthenticationResult
{
    private String token;

    private String errorMesage;

    public GoogleAnalyticsAuthenticationResult( final String token, final String errorMesage )
    {
        this.token = token == null ? "" : token;
        this.errorMesage = errorMesage == null ? "" : errorMesage;
    }

    public String getToken()
    {
        return token;
    }

    public String getErrorMesage()
    {
        return errorMesage;
    }
}
