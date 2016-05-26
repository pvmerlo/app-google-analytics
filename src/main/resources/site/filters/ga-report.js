var contentLib = require('/lib/xp/content');
var portalLib = require('/lib/xp/portal');

exports.responseFilter = function (req, res) {
    var contentId = req.params.contentId;
    if (!contentId) {
        contentId = portalLib.getContent()._id;
    }
    var siteConfig = contentLib.getSiteConfig({
        key: contentId,
        applicationKey: app.name
    });
    var trackingID = siteConfig['trackingId'] || '';
    var enableTracking = siteConfig['enableTracking'] || false;

    if (!trackingID || !enableTracking) {
        return res;
    }

    if (req.mode !== 'live') {
        return res;
    }

    var snippet = '<!-- Google Analytics -->';
    snippet += '<script>';
    snippet += '(function(i,s,o,g,r,a,m){i[\'GoogleAnalyticsObject\']=r;i[r]=i[r]||function(){';
    snippet += '(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),';
    snippet += 'm=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)';
    snippet += '})(window,document,\'script\',\'//www.google-analytics.com/analytics.js\',\'ga\');';
    snippet += 'ga(\'create\', \'' + trackingID + '\', \'auto\');';
    snippet += 'ga(\'send\', \'pageview\');';
    snippet += '</script>';
    snippet += '<!-- End Google Analytics -->';

    var headEnd = res.pageContributions.headEnd;
    if (!headEnd) {
        res.pageContributions.headEnd = [];
    }
    else if (typeof(headEnd) == 'string') {
        res.pageContributions.headEnd = [headEnd];
    }

    res.pageContributions.headEnd.push(snippet);
    return res;
};
