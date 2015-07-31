package com.enonic.app.ga.rest.auth;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.analytics.AnalyticsScopes;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.attachment.Attachment;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.Media;
import com.enonic.xp.data.Property;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.rest.PortalRestService;
import com.enonic.xp.site.Site;

@Component(immediate = true)
public class GoogleAnalyticsAuthenticationService
    implements PortalRestService
{
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String SITE_ERROR_MSG = "Site not found.";

    private static final String SERVICE_ACCOUNT_P12_KEY_ERROR_MSG = "Service Account and P12 key not found.";

    private static final String SERVICE_ACCOUNT_ERROR_MSG = "Service Account not found.";

    private static final String P12_KEY_ERROR_MSG = "P12 key not found.";

    private static final String TOKEN_RETRIEVAL_ERROR_MSG = "Error while retrieving token: ";

    private ContentService contentService;

    @GET
    @Path("authenticate")
    @Produces(MediaType.APPLICATION_JSON)
    public String authenticate( @Context final HttpServletRequest httpServletRequest )
        throws IOException
    {
        final GoogleAnalyticsAuthenticationResult googleAnalyticsAuthenticationResult = doAuthenticate( httpServletRequest );

        //Prepares the authentication result
        ObjectMapper mapper = new ObjectMapper();
        String authenticationResult = mapper.writeValueAsString( googleAnalyticsAuthenticationResult );

        return authenticationResult;
    }

    private GoogleAnalyticsAuthenticationResult doAuthenticate( final HttpServletRequest httpServletRequest )
    {
        //Retrieves the service account and P12 key
        String serviceAccount = null;
        String p12Key = null;
        final PortalRequest portalRequest = PortalRequestAccessor.get( httpServletRequest );
        final Site site = portalRequest.getSite();
        if ( site == null )
        {
            return error( SITE_ERROR_MSG );
        }
        final PropertyTree siteConfig = site.getSiteConfig( ApplicationKey.from( "com.enonic.app.ga" ) );
        if ( siteConfig != null )
        {
            final Property serviceAccountProperty = siteConfig.getProperty( "serviceAccount" );
            if ( serviceAccountProperty != null )
            {
                serviceAccount = serviceAccountProperty.getString();
            }
            final Property p12KeyProperty = siteConfig.getProperty( "p12Key" );
            if ( p12KeyProperty != null )
            {
                p12Key = p12KeyProperty.getString();
            }
        }

        //Retrieves the linked P12 file
        InputStream p12InputStream = null;
        if ( serviceAccount == null && p12Key == null )
        {
            return error( SERVICE_ACCOUNT_P12_KEY_ERROR_MSG );
        }
        if ( serviceAccount == null )
        {
            return error( SERVICE_ACCOUNT_ERROR_MSG );
        }
        if ( p12Key == null )
        {
            return error( P12_KEY_ERROR_MSG );
        }
        final ContentId p12ContentId = ContentId.from( p12Key );
        final Content p12Content = contentService.getById( p12ContentId );
        if ( p12Content instanceof Media )
        {
            final Attachment sourceAttachment = ( (Media) p12Content ).getSourceAttachment();
            if ( sourceAttachment != null )
            {
                p12InputStream = contentService.getBinaryInputStream( p12ContentId, sourceAttachment.getBinaryReference() );
            }
        }

        //Retrieves the token
        String accessToken = null;
        if ( p12InputStream != null )
        {
            try
            {
                accessToken = retrieveAccessToken( serviceAccount, p12InputStream );
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
                    p12InputStream.close();
                }
                catch ( IOException e )
                {
                    return error( TOKEN_RETRIEVAL_ERROR_MSG + e.getMessage() );
                }
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

    @Reference
    public void setContentService( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    public String getName()
    {
        return "google-analytics";
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
