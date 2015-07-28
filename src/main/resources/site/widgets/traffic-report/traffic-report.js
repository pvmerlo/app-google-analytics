var portal = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');

exports.get = function() {
    var view = resolve('traffic-report.html');
    var siteConfigs = portal.getSite().siteConfigs;
    var clientId;

    if (siteConfigs["com.enonic.app.ga"]) {
        clientId = siteConfigs["com.enonic.app.ga"].clientId;

        log.info("GA Client Id is: " + clientId);
    }

    var params = {
        clientId: clientId
    };

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, params),
        pageContributions: {
            bodyEnd: [
                '<script src="' + portal.assetUrl({path: 'js/google-analytics.js'}) + '" type="text/javascript"></script>',
                '<script src="https://apis.google.com/js/client.js?onload=authorize"></script>'
            ]
        }
    };
};
