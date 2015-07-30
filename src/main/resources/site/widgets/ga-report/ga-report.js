var portal = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');

function handleGet(req) {
    var content = portal.getContent();
    var serviceUrl = req.baseUrl + content._path + "/_/rest/google-analytics/authenticate";
    var view = resolve('ga-report.html');

    var params = {
        serviceUrl: serviceUrl
    };

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, params),
        pageContributions: {
            bodyEnd: [
                '<script src="' + portal.assetUrl({path: 'js/embed-api.js'}) + '" type="text/javascript"></script>',
                '<script src="' + portal.assetUrl({path: 'js/google-analytics.js'}) + '" type="text/javascript" serviceurl="' + serviceUrl + '"></script>'
            ]
        }
    };
}
exports.get = handleGet;
