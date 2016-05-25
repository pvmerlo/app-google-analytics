var contentLib = require('/lib/xp/content');
var portalLib = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');

function handleGet(req) {
    var uid = req.params.uid;

    var contentId = req.params.contentid;
    if (!contentId) {
        contentId = portalLib.getContent()._id;
    }

    var content = contentLib.get({key: contentId});
    log.info("content: %s", JSON.stringify(content, null, 2));
    var site = contentLib.getSite({key: contentId});
    log.info("site: %s", JSON.stringify(site, null, 2));
    var siteConfig = contentLib.getSiteConfig({key: contentId, applicationKey: app.name});
    log.info("siteConfig: %s", JSON.stringify(siteConfig, null, 2));
    var pageId = "";

    if (content.type.indexOf(":site") == -1 && !!site) {
        pageId = content._path.replace(site._path, "");
    }

    var view = resolve('ga-report.html');
    var params = {
        googleAnalyticsCssUrl: portalLib.assetUrl({path: 'css/google-analytics.css'}),
        embedApiJsUrl: portalLib.assetUrl({path: 'js/embed-api.js'}),
        googleAnalyticsJsUrl: portalLib.assetUrl({path: 'js/google-analytics.js'}),
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
