function getServiceUrl() {
    var scripts = document.getElementsByTagName('script');
    var lastScript = scripts[scripts.length - 1];
    var scriptName = lastScript;
    return scriptName.getAttribute('serviceurl');
}

var serviceUrl = getServiceUrl();

gapi.analytics.ready(function() {

    function setContainerVisible(containerId, visible) {
        var container = document.getElementById(containerId);
        if (container) {
            container.hidden = !visible;
        }

        return container;
    }

    function showAuthenticationError(errorMessage) {
        setContainerVisible('ga-not-authenticated', true).innerText = "Authentication failed" + (errorMessage ? ": " + errorMessage : "");
        setContainerVisible('ga-authenticated', false);
    }

    function getToken() {
        if (!serviceUrl) {
            showAuthenticationError();
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
                setContainerVisible('ga-authenticated', true);
                setContainerVisible('ga-not-authenticated', false);
                authorize(responseObject.token);
            }
        }
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
         * Line chart by sessions
         */
        var dataChart = new gapi.analytics.googleCharts.DataChart({
            query: {
                metrics: 'ga:sessions',
                dimensions: 'ga:date',
                'start-date': '30daysAgo',
                'end-date': 'yesterday'
            },
            chart: {
                container: 'chart-container-1',
                type: 'LINE',
                options: {
                    title: 'Traffic by Date'
                }
            }
        });

        /**
         * List of pages sorted by pageviews
         */
            /*
        var dataChart2 = new gapi.analytics.googleCharts.DataChart({
            query: {
                metrics: 'ga:uniquePageviews',
                dimensions: 'ga:pageTitle',
                'start-date': '30daysAgo',
                'end-date': 'yesterday'
            },
            chart: {
                container: 'chart-container-2',
                type: 'TABLE',
                options: {
                    title: 'Unique Pageviews',
                    height: '300px',
                    page: 'enable',
                    sortColumn: 1,
                    sortAscending: false
                }
            }
        });
*/

        /**
         * Pie chart by referer
         */
        var dataChart2 = new gapi.analytics.googleCharts.DataChart({
            query: {
                metrics: 'ga:users',
                dimensions: 'ga:source',
                'start-date': '30daysAgo',
                'end-date': 'yesterday'
            },
            chart: {
                container: 'chart-container-2',
                type: 'PIE',
                options: {
                    title: 'Traffic by Referer',
                    is3D: true
                }
            }
        });

        /**
         * Column chart by users/new users
         */
        var dataChart3 = new gapi.analytics.googleCharts.DataChart({
            query: {
                metrics: 'ga:users,ga:newUsers',
                dimensions: 'ga:date',
                'start-date': '30daysAgo',
                'end-date': 'yesterday'
            },
            chart: {
                container: 'chart-container-3',
                type: 'COLUMN',
                options: {
                    title: 'Users by Date',
                    bar: { groupWidth: '75%' },
                    isStacked: true
                }
            }
        });


        /**
         * Bar chart by countries
         */
        var dataChart4 = new gapi.analytics.googleCharts.DataChart({
            query: {
                metrics: 'ga:users',
                dimensions: 'ga:country',
                'start-date': '30daysAgo',
                'end-date': 'yesterday'
            },
            chart: {
                container: 'chart-container-4',
                type: 'GEO',
                options: {
                    height: '90%'
                }
            }
        });

        /**
         * Render the dataChart on the page whenever a new view is selected.
         */
        viewSelector.on('change', function(ids) {
            dataChart.set({query: {ids: ids}}).execute();
            dataChart2.set({query: {ids: ids}}).execute();
            dataChart3.set({query: {ids: ids}}).execute();
            dataChart4.set({query: {ids: ids}}).execute();
        });

    }

    getToken();

});
