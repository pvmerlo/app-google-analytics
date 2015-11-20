var portal = require('/lib/xp/portal');

exports.responseFilter = function (req, res) {
    var siteConfig = portal.getSiteConfig();
    var trackingID = siteConfig['trackingId'] || '';
    var enableTracking = siteConfig['enableTracking'] || false;

    if (trackingID != '' && enableTracking) {
        // Only add snippet if in live mode
        if (req.mode === 'live') {
            if (!res.pageContributions.headEnd) {
                res.pageContributions.headEnd = [];
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

            res.pageContributions.headEnd.push(snippet);
        }
    }
    return res;
};


