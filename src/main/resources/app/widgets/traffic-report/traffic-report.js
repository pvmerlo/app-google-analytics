var portal = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');

exports.get = function() {
    var content = portal.getContent();
    var view = resolve('traffic-report.html');
    var siteConfigs = portal.getSite().siteConfigs;
    var googleUA;

    log.info("Showing content of Google Analytics app");

    if (siteConfigs["com.enonic.app.ga"]) {
        googleUA = siteConfigs["com.enonic.app.ga"].googleUA;

        log.info("GA key is: " + googleUA);
    }

    var params = {
        googleUA: googleUA
    };

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, params)
    };
};
