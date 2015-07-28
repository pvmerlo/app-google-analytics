package com.enonic.app.ga.rest.auth;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Component;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.data.Property;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.rest.PortalRestService;
import com.enonic.xp.site.Site;

@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
public class GoogleAnalyticsAuthenticationService
    implements PortalRestService
{
    @GET
    @Path("authenticate")
    public String test( @Context HttpServletRequest httpServletRequest )
    {
        //Retrieves the client id
        String clientId = "Undefined";
        final PortalRequest portalRequest = PortalRequestAccessor.get( httpServletRequest );
        final Site site = portalRequest.getSite();
        if ( site != null )
        {
            final PropertyTree siteConfig = site.getSiteConfig( ApplicationKey.from( "com.enonic.app.ga" ) );
            if ( siteConfig != null )
            {
                final Property clientIdProperty = siteConfig.getProperty( "clientId" );
                if ( clientIdProperty != null )
                {
                    clientId = clientIdProperty.getString();
                }
            }
        }

        return "Client id: " + clientId;
    }

    @Override
    public String getName()
    {
        return "google-analytics";
    }
}
