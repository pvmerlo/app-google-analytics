var portal = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');

function handleGet(req) {
    var uid = req.url.split('?uid=')[1];
    var view = resolve('ga-report.html');
    var siteConfig = portal.getSiteConfig();
    var content = portal.getContent();
    var site = portal.getSite();
    var pageId = "";

    if (content.type.indexOf(":site") == -1 && !!site) {
        pageId = content._path.replace(site._path, "");
    }

    var params = {
        googleAnalyticsCssUrl: portal.assetUrl({path: 'css/google-analytics.css'}),
        embedApiJsUrl: portal.assetUrl({path: 'js/embed-api.js'}),
        googleAnalyticsJsUrl: portal.assetUrl({path: 'js/google-analytics.js'}),
        serviceUrl: '/admin/rest/google-analytics/authenticate',
        trackingId: siteConfig && siteConfig.trackingId ? siteConfig.trackingId : "",
        uid: uid,
        pageId: pageId
    }

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, params)
    };
}
exports.get = handleGet;
