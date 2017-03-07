package com.enonic.app.ga.rest.auth;

import com.google.api.services.analytics.Analytics;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.Webproperties;


import java.io.IOException;

public class GoogleAnalyticsAuthenticationResult
{
    private String token;

    private String errorMessage;

    private String analyticsProfile;

    private Analytics analytics;

    public GoogleAnalyticsAuthenticationResult( final String token, final String errorMessage, final Analytics analytics )
    {
        this.token = token == null ? "" : token;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
        this.analytics = analytics == null ? null : analytics;

        if(analytics != null){
            try {
                this.analyticsProfile = getFirstProfileId(analytics);
            } catch (Exception e){
                this.errorMessage = "Error getting profileId";
            }
        }
    }

    public String getToken()
    {
        return token;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public String getAnalyticsProfile(){
        return analyticsProfile;
    }

    @JsonIgnore
    public Analytics getAnalytics(){
        return analytics;
    }

    private String getFirstProfileId(final Analytics analytics) throws IOException {
        // Get the first view (profile) ID for the authorized user.
        String profileId = null;

        // Query for the list of all accounts associated with the service account.
        Accounts accounts = analytics.management().accounts().list().execute();

        if (accounts.getItems().isEmpty()) {
            System.err.println("No accounts found");
        } else {
            String firstAccountId = accounts.getItems().get(0).getId();

            // Query for the list of properties associated with the first account.
            Webproperties properties = analytics.management().webproperties()
                    .list(firstAccountId).execute();

            if (properties.getItems().isEmpty()) {
                System.err.println("No Webproperties found");
            } else {
                String firstWebpropertyId = properties.getItems().get(0).getId();

                // Query for the list views (profiles) associated with the property.
                Profiles profiles = analytics.management().profiles()
                        .list(firstAccountId, firstWebpropertyId).execute();

                if (profiles.getItems().isEmpty()) {
                    System.err.println("No views (profiles) found");
                } else {
                    // Return the first (view) profile associated with the property.
                    profileId = profiles.getItems().get(0).getId();
                }
            }
        }
        return profileId;
    }
}
