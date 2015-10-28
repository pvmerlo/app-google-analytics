var portal = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');

function handleGet(req) {
    var view = resolve('ga-report.html');
    var siteConfig = portal.getSiteConfig();

    var params = {
        googleAnalyticsCssUrl: portal.assetUrl({path: 'css/google-analytics.css'}),
        embedApiJsUrl: portal.assetUrl({path: 'js/embed-api.js'}),
        googleAnalyticsJsUrl: portal.assetUrl({path: 'js/google-analytics.js'}),
        serviceUrl: '/admin/rest/google-analytics/authenticate',
        trackingId: siteConfig && siteConfig.trackingId ? siteConfig.trackingId : ""
    }

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, params)
    };
}
exports.get = handleGet;
