(function() {
    'use strict';
    let p = location.pathname;
    if (p.lastIndexOf('/') > 0) p = p.substring(0, p.lastIndexOf('/') + 1);
    else if (!p.endsWith('/')) p += '/';
    const s = document.createElement('script');
    s.src = p + 'js/app.js';
    s.onload = function() { main(); };
    document.head.appendChild(s);
})();
