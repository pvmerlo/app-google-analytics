package com.enonic.app.ga.rest.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
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

import com.enonic.xp.admin.AdminResource;
import com.enonic.xp.admin.rest.resource.ResourceConstants;
import com.enonic.xp.home.HomeDir;
import com.enonic.xp.security.RoleKeys;

@Path(ResourceConstants.REST_ROOT + "google-analytics")
@RolesAllowed(RoleKeys.ADMIN_ID)
@Component(immediate = true)
public class GoogleAnalyticsAuthenticationService
    implements AdminResource
{
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String SERVICE_ACCOUNT_P12_KEY_ERROR_MSG = "Service Account and P12 key not found.";

    private static final String SERVICE_ACCOUNT_ERROR_MSG = "Service Account not found.";

    private static final String P12_KEY_ERROR_MSG = "P12 key not found.";

    private static final String TOKEN_RETRIEVAL_ERROR_MSG = "Error while retrieving token: ";

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
        final String homeDirectoryPath = HomeDir.get().toString();
        final java.nio.file.Path serverAccountPath = Paths.get( homeDirectoryPath, "config/ga_account.txt" );
        final String serviceAccount = readFirstLine( serverAccountPath );
        final java.nio.file.Path p12KeyPath = Paths.get( homeDirectoryPath, "config/ga_key.p12" );

        if ( Strings.isNullOrEmpty( serviceAccount ) && !Files.exists( p12KeyPath ) )
        {
            return error( SERVICE_ACCOUNT_P12_KEY_ERROR_MSG );
        }
        if ( Strings.isNullOrEmpty( serviceAccount ) )
        {
            return error( SERVICE_ACCOUNT_ERROR_MSG );
        }
        if ( !Files.exists( p12KeyPath ) )
        {
            return error( P12_KEY_ERROR_MSG );
        }

        //Retrieves the token
        String accessToken = null;
        InputStream p12KeyInputStream = null;
        try
        {
            p12KeyInputStream = Files.newInputStream( p12KeyPath );
            accessToken = retrieveAccessToken( serviceAccount, p12KeyInputStream );
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
                if ( p12KeyInputStream != null )
                {
                    p12KeyInputStream.close();
                }
            }
            catch ( IOException e )
            {
                return error( TOKEN_RETRIEVAL_ERROR_MSG + e.getMessage() );
            }
        }

        return success( accessToken );
    }

    private static String readFirstLine( java.nio.file.Path path )
    {
        if ( Files.isReadable( path ) )
        {
            try
            {
                final BufferedReader bufferedReader = Files.newBufferedReader( path );
                return bufferedReader.readLine();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
        return null;
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
