package com.enonic.app.ga.rest.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.common.base.Strings;

import com.enonic.xp.jaxrs.JaxRsComponent;
import com.enonic.xp.security.RoleKeys;

@Path("admin/rest/google-analytics")
@RolesAllowed(RoleKeys.ADMIN_ID)
@Component(immediate = true, configurationPid = "com.enonic.app.ga")
public class GoogleAnalyticsAuthenticationService
    implements JaxRsComponent
{
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String GA_SERVICE_ACCOUNT_PROPERTY_KEY = "ga.serviceAccount";

    private static final String GA_P12_KEY_PATH = "ga.p12KeyPath";

    private static final String SERVICE_ACCOUNT_P12_KEY_ERROR_MSG = "Service Account and P12 key not found.";

    private static final String SERVICE_ACCOUNT_ERROR_MSG = "Service Account not found.";

    private static final String P12_KEY_ERROR_MSG = "P12 key not found.";

    private static final String TOKEN_RETRIEVAL_ERROR_MSG = "Error while retrieving token: ";

    private String gaServiceAccount;

    private String gaP12KeyPathValue;

    @Activate
    public void activate( final Map<String, String> map )
    {
        this.gaServiceAccount = map.get( GA_SERVICE_ACCOUNT_PROPERTY_KEY );
        if ( this.gaServiceAccount != null )
        {
            this.gaServiceAccount = this.gaServiceAccount.trim();
        }
        this.gaP12KeyPathValue = map.get( GA_P12_KEY_PATH );
        if ( this.gaP12KeyPathValue != null )
        {
            this.gaP12KeyPathValue = this.gaP12KeyPathValue.trim();
        }
    }

    @GET
    @Path("authenticate")
    @Produces(MediaType.APPLICATION_JSON)
    public String authenticate()
        throws IOException
    {
        final GoogleAnalyticsAuthenticationResult googleAnalyticsAuthenticationResult = doAuthenticate();

        //Prepares the authentication result
        ObjectMapper mapper = new ObjectMapper();
        String authenticationResult = mapper.writeValueAsString( googleAnalyticsAuthenticationResult );

        return authenticationResult;
    }

    private GoogleAnalyticsAuthenticationResult doAuthenticate()
    {
        //Retrieves the service account and P12 key
        final java.nio.file.Path ga12KeyPath = gaP12KeyPathValue != null ? Paths.get( gaP12KeyPathValue ) : null;

        if ( Strings.isNullOrEmpty( gaServiceAccount ) && ( ga12KeyPath == null || !Files.exists( ga12KeyPath ) ) )
        {
            return error( SERVICE_ACCOUNT_P12_KEY_ERROR_MSG );
        }
        if ( Strings.isNullOrEmpty( gaServiceAccount ) )
        {
            return error( SERVICE_ACCOUNT_ERROR_MSG );
        }
        if ( ( ga12KeyPath == null || !Files.exists( ga12KeyPath ) ) )
        {
            return error( P12_KEY_ERROR_MSG );
        }

        //Retrieves the token
        String accessToken = null;
        InputStream gaP12KeyInputStream = null;
        try
        {
            gaP12KeyInputStream = Files.newInputStream( ga12KeyPath );
            accessToken = retrieveAccessToken( gaServiceAccount, gaP12KeyInputStream );
        }
        catch ( TokenResponseException e )
        {
            final TokenErrorResponse tokenErrorResponse = e.getDetails();
            if ( tokenErrorResponse != null && tokenErrorResponse.getError() != null )
            {
                return error( TOKEN_RETRIEVAL_ERROR_MSG + tokenErrorResponse.getError() );
            }
            else
            {
                return error( TOKEN_RETRIEVAL_ERROR_MSG + e.getMessage() );
            }
        }
        catch ( Exception e )
        {
            return error( TOKEN_RETRIEVAL_ERROR_MSG + e.getMessage() );
        }
        finally
        {
            try
            {
                if ( gaP12KeyInputStream != null )
                {
                    gaP12KeyInputStream.close();
                }
            }
            catch ( IOException e )
            {
                return error( TOKEN_RETRIEVAL_ERROR_MSG + e.getMessage() );
            }
        }

        return success( accessToken );
    }

    private static GoogleAnalyticsAuthenticationResult success( String accessToken )
    {
        return new GoogleAnalyticsAuthenticationResult( accessToken, null );
    }

    private static GoogleAnalyticsAuthenticationResult error( String errorMessage )
    {
        return new GoogleAnalyticsAuthenticationResult( null, errorMessage );
    }

    private String retrieveAccessToken( final String serviceAccount, final InputStream p12InputStream )
        throws Exception
    {
        String accessToken = null;

        final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        final PrivateKey privateKey =
            SecurityUtils.loadPrivateKeyFromKeyStore( SecurityUtils.getPkcs12KeyStore(), p12InputStream, "notasecret", "privatekey",
                                                      "notasecret" );

        final GoogleCredential credential = new GoogleCredential.Builder().
            setTransport( httpTransport ).
            setJsonFactory( JSON_FACTORY ).
            setServiceAccountId( serviceAccount ).
            setServiceAccountPrivateKey( privateKey ).
            setServiceAccountScopes( AnalyticsScopes.all() ).
            build();

        if ( credential.refreshToken() )
        {
            accessToken = credential.getAccessToken();
        }

        return accessToken;
    }

}
