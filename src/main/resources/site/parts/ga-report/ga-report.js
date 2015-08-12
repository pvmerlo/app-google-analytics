var portal = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');

function handleGet(req) {
    var content = portal.getContent();
    var view = resolve('ga-report.html');

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, {}),
        pageContributions: {
            headEnd: [
                '<link rel="stylesheet" href="' + portal.assetUrl({path: 'css/google-analytics.css'}) + '" type="text/css" />'
            ],
            bodyEnd: [
                '<script src="' + portal.assetUrl({path: 'js/embed-api.js'}) + '" type="text/javascript"></script>',
                '<script src="' + portal.assetUrl({path: 'js/google-analytics.js'}) +
                '" type="text/javascript" serviceurl="/admin/rest/google-analytics/authenticate/' + content._id +
                '"></script>'
            ]
        }
    };
}
exports.get = handleGet;
