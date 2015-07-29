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

    private ContentService contentService;

    @GET
    @Path("authenticate")
    @Produces(MediaType.APPLICATION_JSON)
    public String test( @Context HttpServletRequest httpServletRequest )
        throws IOException
    {
        String accessToken = null;
        String errorMessage = null;

        //Retrieves the service account and P12 key
        String serviceAccount = null;
        String p12Key = null;
        final PortalRequest portalRequest = PortalRequestAccessor.get( httpServletRequest );
        final Site site = portalRequest.getSite();
        if ( site != null )
        {
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
        }

        //Retrieves the linked P12 file
        InputStream p12InputStream = null;
        if ( serviceAccount == null )
        {
            errorMessage = "Service Account not found";
        }
        else if ( p12Key == null )
        {
            errorMessage = "P12 key not found";
        }
        else
        {
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
        }

        //Retrieves the token
        if ( serviceAccount != null && p12InputStream != null )
        {
            try
            {
                accessToken = retrieveAccessToken( serviceAccount, p12InputStream );
            }
            catch ( Exception e )
            {
                errorMessage = "Error while retrieving token: " + e.getMessage();
            }
            finally
            {
                try
                {
                    p12InputStream.close();
                }
                catch ( IOException e )
                {
                    errorMessage = "Error while closing P12 key file: " + e.getMessage();
                }
            }
        }

        //Prepares the authentication result
        ObjectMapper mapper = new ObjectMapper();
        String authenticationResult = mapper.writeValueAsString( new GoogleAnalyticsAuthenticationResult( accessToken, errorMessage ) );

        return authenticationResult;

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
