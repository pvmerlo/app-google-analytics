package com.enonic.app.ga.rest.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.sound.midi.SysexMessage;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

//import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
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

import java.net.URLDecoder;
import java.net.URLEncoder;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.model.GaData;


@Path("admin/rest/google-analytics")
@RolesAllowed(RoleKeys.ADMIN_ID)
@Component(immediate = true, configurationPid = "com.enonic.app.ga", property = "group=admin")
public class GoogleAnalyticsAuthenticationService
        implements JaxRsComponent
{
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String APPLICATION_NAME = "Enonic XP Google Analytics App";

    private static final String GA_SERVICE_ACCOUNT_PROPERTY_KEY = "ga.serviceAccount";

    private static final String GA_P12_KEY_PATH = "ga.p12KeyPath";

    private static final String SERVICE_ACCOUNT_P12_KEY_ERROR_MSG = "Service Account and P12 key not found.";

    private static final String SERVICE_ACCOUNT_ERROR_MSG = "Service Account not found.";

    private static final String P12_KEY_ERROR_MSG = "P12 key not found.";

    private static final String TOKEN_RETRIEVAL_ERROR_MSG = "Error while retrieving token: ";

    private static final String PROFILE_ID_RETRIVAL_ERROR_MSG = "Error retrieving Account Profile";

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
        Analytics analytics = null;

        try
        {

            gaP12KeyInputStream = Files.newInputStream( ga12KeyPath );
            final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            final GoogleCredential credential = getGoogleCredential(gaServiceAccount, gaP12KeyInputStream, httpTransport);

            accessToken = retrieveAccessToken( credential );
            analytics = retrieveAnalytics( credential, httpTransport );
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
            System.out.println(e.getMessage());
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

        return success( accessToken, analytics );
    }

    private static GoogleAnalyticsAuthenticationResult success( String accessToken, Analytics analytics )
    {
        return new GoogleAnalyticsAuthenticationResult( accessToken, null, analytics );
    }

    private static GoogleAnalyticsAuthenticationResult error( String errorMessage )
    {
        return new GoogleAnalyticsAuthenticationResult( null, errorMessage, null );
    }

    private String retrieveAccessToken( final GoogleCredential credential)
            throws Exception
    {
        String accessToken = null;

        if ( credential.refreshToken() )
        {
            accessToken = credential.getAccessToken();
        }

        return accessToken;
    }

    private Analytics retrieveAnalytics( final GoogleCredential credential, final HttpTransport httpTransport )
            throws Exception
    {
        return new Analytics.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();
    }

    private GoogleCredential getGoogleCredential( final String serviceAccount, final InputStream p12InputStream, final HttpTransport httpTransport )
            throws Exception
    {
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

        return credential;
    }

    @GET
    @Path("/reports/{startDate}/{endDate}/{metrics}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getResults(@PathParam("startDate") String startDate, @PathParam("endDate") String endDate,
                             @PathParam("metrics") String metrics,
                             @DefaultValue("ga:pageTitle,ga:pagePath") @QueryParam("dimensions") String dimensions,
                             @DefaultValue("-ga:pageviews") @QueryParam("sort") String sort,
                             @DefaultValue("ga:pagePath=~/Nyheter/,ga:pagePath=~/Nyheiter/") @QueryParam("filters") String filters)
            throws Exception {
        final GoogleAnalyticsAuthenticationResult googleAnalyticsAuthenticationResult = doAuthenticate();

        ObjectMapper mapper = new ObjectMapper();

        //Prepares the authentication result
        String profile = googleAnalyticsAuthenticationResult.getAnalyticsProfile();

        dimensions = URLDecoder.decode(dimensions, "UTF-8");
        filters = URLDecoder.decode(filters, "UTF-8");
        sort = URLDecoder.decode(sort, "UTF-8");

        GaData data = googleAnalyticsAuthenticationResult.getAnalytics().data().ga()
                .get("ga:" + profile, startDate, endDate, metrics)
                .setDimensions(dimensions)
                .setSort(sort)
                .setFilters(filters)
                .setMaxResults(100)
                .execute();

        return mapper.writeValueAsString(data);
    }
}
