(function() {
    'use strict';

    // -- Cookie helpers --
    function getCookie(name) {
        const re = new RegExp(String.raw`(^|;)\s*` + name + String.raw`\s*=\s*([^;]+)`);
        const m = re.exec(document.cookie);
        return m ? decodeURIComponent(m.pop()) : null;
    }
    function setCookie(name, value, path) {
        document.cookie = name + '=' + encodeURIComponent(value) + ';path=' + (path || '/');
    }
    function removeCookie(name) {
        document.cookie = name + '=;path=/;expires=Thu,01 Jan 1970 00:00:00 GMT';
    }

    // -- Base64 URL-safe (RFC 4648, no padding) --
    const B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
    function b64urlEncode(buf) {
        let r = '';
        for (let i = 0; i < buf.length; i += 3) {
            const c = (buf[i] << 16) | (i+1 < buf.length ? buf[i+1] << 8 : 0) | (i+2 < buf.length ? buf[i+2] : 0);
            r += B64[(c >> 18) & 63];
            r += B64[(c >> 12) & 63];
            if (i+1 < buf.length) r += B64[(c >> 6) & 63];
            if (i+2 < buf.length) r += B64[c & 63];
        }
        return r;
    }

    // -- Standard Base64 (RFC 2045, with padding) --
    const B64STD = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    function b64encode(buf) {
        let r = '';
        for (let i = 0; i < buf.length; i += 3) {
            const c = (buf[i] << 16) | (i+1 < buf.length ? buf[i+1] << 8 : 0) | (i+2 < buf.length ? buf[i+2] : 0);
            r += B64STD[(c >> 18) & 63];
            r += B64STD[(c >> 12) & 63];
            r += (i+1 < buf.length) ? B64STD[(c >> 6) & 63] : '=';
            r += (i+2 < buf.length) ? B64STD[c & 63] : '=';
        }
        return r;
    }

    // -- UTF8 encode string to Uint8Array --
    function utf8encode(str) {
        return new TextEncoder().encode(str);
    }

    // -- BigInt helpers --
    function bufToBigInt(buf) {
        let r = 0n;
        for (const b of buf) {
            r = (r << 8n) + BigInt(b);
        }
        return r;
    }

    function modPow(base, exp, mod) {
        if (mod === 1n) return 0n;
        let result = 1n;
        base = base % mod;
        while (exp > 0n) {
            if (exp % 2n === 1n) {
                result = (result * base) % mod;
            }
            exp = exp / 2n;
            base = (base * base) % mod;
        }
        return result;
    }

    function parseBigInt36(str) {
        const ks = '0123456789abcdefghijklmnopqrstuvwxyz';
        let r = 0n;
        for (const ch of str) {
            r = r * 36n + BigInt(ks.indexOf(ch));
        }
        return r;
    }

    function bigIntToBase36(n) {
        if (n === 0n) return '0';
        const ks = '0123456789abcdefghijklmnopqrstuvwxyz';
        let r = '';
        while (n > 0n) {
            r = ks[Number(n % 36n)] + r;
            n = n / 36n;
        }
        return r;
    }

    // -- RSA encrypt --
    function rsaEncryptToBase36(messageBytes, exponent, modulus) {
        const messageAsBase64 = b64encode(messageBytes);
        const safeBytes = utf8encode(messageAsBase64);
        const messageBigInt = bufToBigInt(safeBytes);
        const encrypted = modPow(messageBigInt, exponent, modulus);
        return bigIntToBase36(encrypted);
    }

    // -- Main security boot --
    const appSKey = getCookie('app_skey');
    if (appSKey) {
        try {
            const parts = appSKey.split(':');
            const exponent = parseBigInt36(parts[0]);
            const modulus = parseBigInt36(parts[1]);

            // Generate random password: base64url of 12 random bytes
            const rndBytes = new Uint8Array(12);
            crypto.getRandomValues(rndBytes);
            const pwd = b64urlEncode(rndBytes);
            const pwdBuf = utf8encode(pwd);

            // RSA-encrypt the password
            const cryptedPwd = rsaEncryptToBase36(pwdBuf, exponent, modulus);

            // Generate salt (16 bytes) and IV (12 bytes)
            const salt = new Uint8Array(16);
            crypto.getRandomValues(salt);
            const iv = new Uint8Array(12);
            crypto.getRandomValues(iv);

            // Build signature: {rsaEncryptedPwd_base36}.{salt_base64url}.{iv_base64url}
            const signature = cryptedPwd + '.' + b64urlEncode(salt) + '.' + b64urlEncode(iv);

            // Set app_signature cookie
            setCookie('app_signature', signature, '/');

            // Store for potential future use
            globalThis.__wdc_appSignature = signature;

            // Store crypto material for AES key derivation
            globalThis.__wdc_cryptoPwdBuf = pwdBuf;
            globalThis.__wdc_cryptoSalt = salt;
            globalThis.__wdc_cryptoIv = iv;

            // Remove app_skey (one-time use)
            removeCookie('app_skey');

            console.log('[security-boot] app_signature cookie set');
        } catch (e) {
            console.error('[security-boot] Failed to generate signature:', e);
        }
    } else {
        console.warn('[security-boot] No app_skey cookie found');
    }

    // Derive AES-GCM key from password (async, ready by the time user interacts)
    (async function() {
        try {
            const pwdBuf = globalThis.__wdc_cryptoPwdBuf;
            const salt = globalThis.__wdc_cryptoSalt;
            const iv = globalThis.__wdc_cryptoIv;
            if (!pwdBuf || !salt || !iv) return;

            const keyMaterial = await crypto.subtle.importKey('raw', pwdBuf, { name: 'PBKDF2' }, false, ['deriveKey']);
            const aesKey = await crypto.subtle.deriveKey(
                { name: 'PBKDF2', salt: salt, iterations: 250000, hash: 'SHA-256' },
                keyMaterial,
                { name: 'AES-GCM', length: 256 },
                false,
                ['encrypt', 'decrypt']
            );
            globalThis.__wdc_aesKey = aesKey;
            globalThis.__wdc_aesIv = iv;

            // Clean up intermediate crypto material
            delete globalThis.__wdc_cryptoPwdBuf;
            delete globalThis.__wdc_cryptoSalt;
            delete globalThis.__wdc_cryptoIv;

            console.log('[security-boot] AES-GCM key derived');
        } catch (e) {
            console.error('[security-boot] AES key derivation failed:', e);
        }
    })();

    // Global cipher function: encrypts text with AES-GCM, calls callback with base64 result
    globalThis.__wdc_cipher = function(text, callback) {
        const key = globalThis.__wdc_aesKey;
        const iv = globalThis.__wdc_aesIv;
        if (!key || !iv) {
            console.error('[cipher] AES key not yet available');
            callback('');
            return;
        }
        const encoded = new TextEncoder().encode(text);
        crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, key, encoded).then(function(ciphertext) {
            const bytes = new Uint8Array(ciphertext);
            let binary = '';
            for (const b of bytes) binary += String.fromCodePoint(b);
            callback(btoa(binary));
        }).catch(function(e) {
            console.error('[cipher] Encryption failed:', e);
            callback('');
        });
    };

    // Also persist app_id to sessionStorage if present as cookie
    const appId = getCookie('app_id');
    if (appId) {
        try { sessionStorage.setItem('app_id', appId); } catch(e) { console.warn('[security-boot] sessionStorage unavailable:', e); }
        removeCookie('app_id');
        console.log('[security-boot] app_id stored:', appId);
    }
})();
