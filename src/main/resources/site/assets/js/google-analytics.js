gapi.analytics.ready(function() {
    var serviceUrl = document.getElementById('ga-service');

    console.log(serviceUrl);
    /**
     * Authorize the user immediately if the user has already granted access.
     * If no access has been created, render an authorize button inside the
     * element with the ID "embed-api-auth-container".
     */
    gapi.analytics.auth.authorize({
        container: 'embed-api-auth-container',
        clientid: 'REPLACE WITH YOUR CLIENT ID'
    });


    /**
     * Create a new ViewSelector instance to be rendered inside of an
     * element with the id "view-selector-container".
     */
    var viewSelector = new gapi.analytics.ViewSelector({
        container: 'view-selector-container'
    });

    // Render the view selector to the page.
    viewSelector.execute();


    /**
     * Create a new DataChart instance with the given query parameters
     * and Google chart options. It will be rendered inside an element
     * with the id "chart-container".
     */
    var dataChart = new gapi.analytics.googleCharts.DataChart({
        query: {
            metrics: 'ga:sessions',
            dimensions: 'ga:date',
            'start-date': '30daysAgo',
            'end-date': 'yesterday'
        },
        chart: {
            container: 'chart-container',
            type: 'LINE',
            options: {
                width: '100%'
            }
        }
    });


    /**
     * Render the dataChart on the page whenever a new view is selected.
     */
    viewSelector.on('change', function(ids) {
        dataChart.set({query: {ids: ids}}).execute();
    });

});
/*
function getToken(serviceUrl) {
    debugger;
}

function authorize(event) {
    gapi.analytics.auth.authorize({
        serverAuth: {
            access_token: 'XXXXXX'
        }
    });
}


function queryAccounts() {
    // Load the Google Analytics client library.
    gapi.client.load('analytics', 'v3').then(function() {

        // Get a list of all Google Analytics accounts for this user
        gapi.client.analytics.management.accounts.list().then(handleAccounts);
    });
}


function handleAccounts(response) {
    // Handles the response from the accounts list method.
    if (response.result.items && response.result.items.length) {
        // Get the first Google Analytics account.
        var firstAccountId = response.result.items[0].id;

        // Query for properties.
        queryProperties(firstAccountId);
    } else {
        console.log('No accounts found for this user.');
    }
}


function queryProperties(accountId) {
    // Get a list of all the properties for the account.
    gapi.client.analytics.management.webproperties.list(
        {'accountId': accountId})
        .then(handleProperties)
        .then(null, function(err) {
            // Log any errors.
            console.log(err);
        });
}


function handleProperties(response) {
    // Handles the response from the webproperties list method.
    if (response.result.items && response.result.items.length) {

        // Get the first Google Analytics account
        var firstAccountId = response.result.items[0].accountId;

        // Get the first property ID
        var firstPropertyId = response.result.items[0].id;

        // Query for Views (Profiles).
        queryProfiles(firstAccountId, firstPropertyId);
    } else {
        console.log('No properties found for this user.');
    }
}


function queryProfiles(accountId, propertyId) {
    // Get a list of all Views (Profiles) for the first property
    // of the first Account.
    gapi.client.analytics.management.profiles.list({
        'accountId': accountId,
        'webPropertyId': propertyId
    })
        .then(handleProfiles)
        .then(null, function(err) {
            // Log any errors.
            console.log(err);
        });
}


function handleProfiles(response) {
    // Handles the response from the profiles list method.
    if (response.result.items && response.result.items.length) {
        // Get the first View (Profile) ID.
        var firstProfileId = response.result.items[0].id;

        // Query the Core Reporting API.
        queryCoreReportingApi(firstProfileId);
    } else {
        console.log('No views (profiles) found for this user.');
    }
}


function queryCoreReportingApi(profileId) {
    // Query the Core Reporting API for the number sessions for
    // the past seven days.
    gapi.client.analytics.data.ga.get({
        'ids': 'ga:' + profileId,
        'start-date': '7daysAgo',
        'end-date': 'today',
        'metrics': 'ga:sessions'
    })
        .then(function(response) {
            var formattedJson = JSON.stringify(response.result, null, 2);
            document.getElementById('query-output').value = formattedJson;
        })
        .then(null, function(err) {
            // Log any errors.
            console.log(err);
        });
}

// Add an event listener to the 'auth-button'.
document.getElementById('auth-button').addEventListener('click', authorize);
*/