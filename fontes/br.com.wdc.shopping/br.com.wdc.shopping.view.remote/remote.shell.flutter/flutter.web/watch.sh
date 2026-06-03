#!/bin/bash
set -e
cd "$(dirname "$0")"

DEPLOY_DIR="../../../../../work/frontend/remote.shell.flutter"
DEV_NOTIFY_URL="http://localhost:8080/__dev/notify?context=remote.shell.flutter"

inject_dev_reload() {
    cat > "$DEPLOY_DIR/js/dev-reload.js" << 'EOF'
(function() {
    if (!window.WebSocket) return;
    var protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    var context = location.pathname.split('/')[1] || '';
    var wsUrl = protocol + '//' + location.host + '/__dev/ws?context=' + context;
    var lastVersion = -1;
    function connect() {
        var ws = new WebSocket(wsUrl);
        ws.onmessage = function(event) {
            try {
                var data = JSON.parse(event.data);
                if (data.type === 'version') {
                    if (lastVersion >= 0 && data.version > lastVersion) {
                        console.log('[DevReload] Missed notification, reloading...');
                        location.reload();
                    }
                    lastVersion = data.version;
                } else if (data.type === 'reload' && (data.context === '*' || data.context === context)) {
                    lastVersion = data.version;
                    console.log('[DevReload] Reloading (v' + data.version + ')...');
                    location.reload();
                }
            } catch (e) {}
        };
        ws.onclose = function() { setTimeout(connect, 300); };
        ws.onerror = function() { ws.close(); };
    }
    connect();
    console.log('[DevReload] Live reload active for context: ' + context);
})();
EOF
    mkdir -p "$DEPLOY_DIR/js"
    if ! grep -q 'dev-reload.js' "$DEPLOY_DIR/index.html"; then
        sed -i '' 's|</body>|    <script src="js/dev-reload.js"></script>\
</body>|' "$DEPLOY_DIR/index.html"
    fi
}

echo "Watching for changes in lib/ and ../flutter.commons/lib/ ..."
echo "Press Ctrl+C to stop."

# Initial build
./build.sh && inject_dev_reload && curl -s -X POST "$DEV_NOTIFY_URL" > /dev/null 2>&1 || true

# Watch for changes and rebuild
fswatch -o lib/ ../flutter.commons/lib/ | while read; do
    echo ""
    echo "=== Change detected, rebuilding... ==="
    if ./build.sh; then
        inject_dev_reload
        curl -s -X POST "$DEV_NOTIFY_URL" > /dev/null 2>&1 || true
        echo "=== Browser notified ==="
    else
        echo "BUILD FAILED"
    fi
done
