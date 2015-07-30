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
            if (responseObject.errorMesage) {
                showAuthenticationError(responseObject.errorMesage);
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

    }

    getToken();

});
