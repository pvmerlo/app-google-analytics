var portal = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');
var view = resolve('main.page.html');

function handleGet(req) {
    var reqContent = portal.getContent();
    var params = {
        reqContent: reqContent,
        mainRegion: reqContent.page.regions["main"]
    };
    var body = thymeleaf.render(view, params);

    return {
        contentType: 'text/html',
        body: body
    };
}

exports.get = handleGet;


