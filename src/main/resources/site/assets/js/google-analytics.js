function getDocument(script) {
    return script ? script.ownerDocument : document;
}

function getCurrentScript() {
    var script = window.HTMLImports ? window.HTMLImports.currentScript : undefined;

    if (!script && !!document.currentScript) {
        script = document.currentScript.__importElement || document.currentScript;
    }

    return script;
}

var gaScript = getCurrentScript();
var gaDocument = getDocument(gaScript);
var serviceUrl = gaScript.getAttribute('serviceurl');
var trackingId = gaScript.getAttribute('trackingid');
var pageId = gaScript.getAttribute('pageid');
var uid = gaDocument.baseURI.split('?uid=')[1];
var viewId;
var dataCharts = [];

function createTitle() {
    var container = getContainer("ga-authenticated");
    var title = gaDocument.createElement("h1");
    title.innerHTML = "Statistics for the " + (pageId ? "page" : "site");

    container.appendChild(title);
}

function getContainer(containerId) {
    containerId = containerId + "_" + uid;
    return document.getElementById(containerId) || gaDocument.getElementById(containerId);
}

function createContainerDiv(id, cls, parentId) {
    var divId = id + "_" + uid;
    var container = getContainer(parentId || "ga-authenticated");
    var div = container.querySelector("#" + divId);

    if (!div) {
        div = gaDocument.createElement("div");

        div.setAttribute("id", divId);
        if (cls) {
            div.setAttribute("class", cls);
        }
        container.appendChild(div);
    }

    return div;
}

function setContainerVisible(containerId, visible) {
    var container = getContainer(containerId);
    if (container) {
        container.hidden = !visible;
    }

    return container;
}

function showAuthenticationError(errorMessage) {
    setContainerVisible('ga-not-authenticated', true).innerHTML = "Authentication failed" + (errorMessage ? ": " + errorMessage : "");
    setContainerVisible('ga-authenticated', false);
}

function showError(errorMessage) {
    setContainerVisible('ga-not-authenticated', true).innerHTML = "Error: " + errorMessage;
    setContainerVisible('ga-authenticated', false);
}

function queryAccounts() {
    gapi.client.analytics.management.accounts.list().then(handleAccounts);
}

function getPropertyUrl(accountId, propertyId) {
    gapi.client.analytics.management.webproperties.get({
        'accountId': accountId,
        'webPropertyId': propertyId
    }).then(function(response) {
        setTitle(response.result.name);
    });
}

function handleAccounts(response) {
    // Handles the response from the accounts list method.
    if (response.result.items && response.result.items.length) {
        // Get the first Google Analytics account.
        var firstAccountId = response.result.items[0].id;

        // Uncomment if we need to show url for selected tracking Id
        //getPropertyUrl(firstAccountId, trackingId);

        // Query for properties.
        queryProfiles(firstAccountId, trackingId);
    } else {
        showError('No accounts found for the user.');
    }
}

function showRequestError(errorObject) {
    if (errorObject.error && errorObject.error.message) {
        showError(errorObject.error.message);
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
            showRequestError(err.result);
        });
}

function createCookie(name, value, days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime()+(days*24*60*60*1000));
        var expires = "; expires="+date.toGMTString();
    }

    document.cookie = "ga_" + name + "=" + value + expires + "; path=/";
}

function saveUidInCookie() {
    createCookie("uid", uid);
}

function saveDateRangeInCookie(dateRange) {
    createCookie(uid + ".start-date", dateRange["start-date"]);
    createCookie(uid + ".end-date", dateRange["end-date"]);
}

function getDateFromCookie(name) {
    return getCookie(uid + "." + name);
}

function getCookie(cname) {
    var name = "ga_" + cname + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
        var c = ca[i].trim();
        if (c.indexOf(name) == 0) return c.substring(name.length, c.length);
    }
    return "";
}

function getDateRange() {
    return {
        'start-date': getDateFromCookie('start-date') || '30daysAgo',
        'end-date': getDateFromCookie('end-date') || 'yesterday'
    };
}

function cleanupCookies() {
    var tempUid = getCookie("uid");

    if (tempUid && tempUid != uid) {
        createCookie("uid", tempUid, -1);
        createCookie(tempUid + ".start-date", "", -1);
        createCookie(tempUid + ".end-date", "", -1);
    }

    if (!tempUid || tempUid != uid) {
        saveUidInCookie(uid);
    }
}

function handleProfiles(response) {
    // Handles the response from the profiles list method.
    if (response.result.items && response.result.items.length) {
        // Get the first View (Profile) ID.
        viewId = "ga:" + response.result.items[0].id;

        /**
         * Create a new DateRangeSelector instance to be rendered inside of an
         * element with the id "date-range-container", set its date range
         * and then render it to the page.
         */
        var dateRangeSelector = new gapi.analytics.ext.DateRangeSelector({
            container: getContainer("date-range-container")
        })
        .set(getDateRange())
        .execute();

        /**
         * Register a handler to run whenever the user changes the date range from
         * the datepicker. The handler will update the first dataChart
         * instance as well as change the dashboard subtitle to reflect the range.
         */
        dateRangeSelector.on('change', function(data) {
            saveDateRangeInCookie(data);
            dataCharts.forEach(function (dataChart) {
                dataChart.set({query: data}).execute();
            });
        });

        // Show statistics for found View ID
        if (pageId) {
            showStatisticsForPage();
        }
        else {
            showStatisticsForSite();
        }
    } else {
        showError('No views (profiles) found for the user.');
    }
}

function getToken() {
    if (!serviceUrl) {
        showAuthenticationError();
        return;
    }
    if (!trackingId) {
        showAuthenticationError("Tracking Id not found");
        return;
    }

    var request = new XMLHttpRequest();
    request.open("GET", serviceUrl, true);
    request.onload = function (){
        var responseObject = JSON.parse(request.responseText);
        if (responseObject.errorMessage) {
            showAuthenticationError(responseObject.errorMessage);
        }
        else if (responseObject.token) {

            cleanupCookies();

            setContainerVisible('ga-authenticated', true);
            setContainerVisible('ga-not-authenticated', false);

            createTitle();
            createContainerDiv("date-range-container");

            if (pageId) {
                createContainerDiv("chart-container-1");
                createContainerDiv("chart-container-2");
                createContainerDiv("chart-container-3", "ga-kpi-chart");
            }
            else {
                createContainerDiv("chart-container-1");
                createContainerDiv("chart-container-2");
                createContainerDiv("chart-container-3", "ga-bydevice-container");
                createContainerDiv("chart-container-4", "ga-bycountry-container");
                createContainerDiv("chart-container-5", "ga-byreferer-container");
            }

            authorize(responseObject.token);
        }
    };
    request.send(null);
}

function authorize(token) {
    /**
     * Authorize the user with a token.
     */
    gapi.analytics.auth.authorize({
        serverAuth: {
            access_token: token
        }
    });

    queryAccounts();
}

function drawChart(containerId, config) {
    var queryCfg = {
            ids: viewId,
            metrics: config.metrics,
            dimensions: config.dimensions,
            'start-date': '30daysAgo',
            'end-date': 'yesterday'
        };

    if (config.filters) {
        queryCfg.filters = config.filters;
    }
    if (config['max-results']) {
        queryCfg['max-results'] = config['max-results'];
    }
    if (config.sort) {
        queryCfg.sort = config.sort;
    }

    var chart = new gapi.analytics.googleCharts.DataChart({
        query: queryCfg,
        chart: {
            container: getContainer(containerId),
            type: config.type,
            options: {
                title: config.title,
                width: '100%',
                is3D: true
            }
        }
    });

    dataCharts.push(chart);

    chart.execute();
}

function formatSeconds(seconds) {
    var hours = parseInt( seconds / 3600 ) % 24;
    var minutes = parseInt( seconds / 60 ) % 60;
    var seconds = parseInt(seconds % 60);

    return (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds  < 10 ? "0" + seconds : seconds);
}

function showStatisticsForPage() {

    /**
     * Line chart by sessions
     */
    drawChart("chart-container-1", {
        title: 'Page views by date',
        type: 'LINE',
        metrics: 'ga:pageViews,ga:uniquePageviews',
        dimensions: 'ga:date',
        filters: 'ga:pagePath==' + pageId
    });

    /**
     * Pie chart by user type (new vs returning)
     */
    drawChart("chart-container-2", {
        title: "Visitors",
        type: 'PIE',
        metrics: 'ga:sessions',
        dimensions: 'ga:userType',
        filters: 'ga:pagePath==' + pageId
    });

    /**
     * Table with avg metrics
     */
    var kpiRequest = new gapi.analytics.report.Data({
        query: {
            ids: viewId,
            metrics: 'ga:avgTimeOnPage,ga:avgPageLoadTime,ga:bounceRate',
            'start-date': '30daysAgo',
            'end-date': 'yesterday',
            filters: 'ga:pagePath==' + pageId
        }
    });

    dataCharts.push(kpiRequest);

    kpiRequest.on("success", onKPILoaded);

    kpiRequest.execute();
}

function onKPILoaded(response) {
    if (response.totalsForAllResults) {
        createContainerDiv("kpi-container-data", "ga-kpi-container-data", "chart-container-3");
        var textContainer = createContainerDiv("kpi-container-text", "ga-kpi-container-text", "chart-container-3");

        var container = createContainerDiv("kpi-container-1", "ga-kpi-container", "kpi-container-data");
        container.innerHTML = "<div>" + parseFloat(response.totalsForAllResults["ga:avgPageLoadTime"]).toFixed(2) + "</div>";

        container = createContainerDiv("kpi-container-2", "ga-kpi-container", "kpi-container-data");
        container.innerHTML = "<div>" + formatSeconds(response.totalsForAllResults["ga:avgTimeOnPage"]) + "</div>";

        container = createContainerDiv("kpi-container-3", "ga-kpi-container", "kpi-container-data");
        container.innerHTML = "<div>" + parseFloat(response.totalsForAllResults["ga:bounceRate"]).toFixed(2) + "%</div>";

        textContainer.innerHTML = "<span>Avg.load time (sec)</span><span>Avg.time on page</span><span>Bounce rate</span>";
    }
}

function showStatisticsForSite() {
    /**
     * Line chart by sessions
     */
    drawChart("chart-container-1", {
        title: "Page views by date",
        type: 'LINE',
        metrics: 'ga:pageViews,ga:uniquePageviews',
        dimensions: 'ga:date'
    });

    /**
     * Pie chart with most popular pages
     */
    drawChart("chart-container-2", {
        title: "Top Pages",
        type: 'PIE',
        metrics: 'ga:uniquePageviews',
        dimensions: 'ga:pagePath',
        'max-results': 12,
        'sort': '-ga:uniquePageviews'
    });

    /**
     * Bar chart by device type
     */
    drawChart("chart-container-3", {
        title: "Devices",
        type: 'BAR',
        metrics: 'ga:users',
        dimensions: 'ga:deviceCategory'
    });

    /**
     * Geo chart by countries
     */
    drawChart("chart-container-4", {
        type: 'GEO',
        metrics: 'ga:users',
        dimensions: 'ga:country'
    });

    /**
     * Table with top 10 referers
     */
    drawChart("chart-container-5", {
        type: 'TABLE',
        metrics: 'ga:users',
        dimensions: 'ga:source',
        'max-results': 10,
        'sort': '-ga:users'
    });
}

if (gapi.analytics.auth) {
    getToken();
}
else {
    gapi.analytics.ready(function () {
        getToken();
   });
}