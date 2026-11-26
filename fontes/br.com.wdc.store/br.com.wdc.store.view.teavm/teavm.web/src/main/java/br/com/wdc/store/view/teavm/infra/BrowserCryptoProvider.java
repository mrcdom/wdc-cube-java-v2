package br.com.wdc.store.view.teavm.infra;

import org.teavm.jso.JSBody;

import br.com.wdc.framework.domain.security.CryptoProvider;

/**
 * {@link CryptoProvider} implementation for the browser: MD5 in JavaScript, SHA-256 and HMAC-SHA256 in Java
 * (synchronous; TeaVM compiles them to plain integer arithmetic, with no copying across to JavaScript on each of the
 * thousands of HMACs of a SCRAM sign-in).
 * <p>
 * Does not use the Web Crypto API (which is asynchronous) to stay compatible
 * with the synchronous interface.
 */
@SuppressWarnings("java:S6126") // String concatenation in @JSBody is intentional for TeaVM JS interop
public class BrowserCryptoProvider implements CryptoProvider {

    @Override
    public byte[] md5(byte[] input) {
        String hex = md5Hex(bytesToJsArray(input));
        return hexToBytes(hex);
    }

    @Override
    public byte[] sha256(byte[] input) {
        return Sha256.digest(input);
    }

    @Override
    public byte[] hmacSha256(byte[] key, byte[] data) {
        return Sha256.hmac(key, data);
    }

    private static int[] bytesToJsArray(byte[] bytes) {
        int[] arr = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            arr[i] = bytes[i] & 0xFF;
        }
        return arr;
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    /**
     * MD5 implemented in pure JavaScript (RFC 1321).
     * Returns a hex string.
     */
    @JSBody(params = { "input" }, script = ""
            + "function md5(bytes){"
            + "  function safeAdd(x,y){var l=(x&0xFFFF)+(y&0xFFFF);return((x>>16)+(y>>16)+(l>>16))<<16|l&0xFFFF;}"
            + "  function bitRotL(n,c){return(n<<c)|(n>>>(32-c));}"
            + "  function md5cmn(q,a,b,x,s,t){return safeAdd(bitRotL(safeAdd(safeAdd(a,q),safeAdd(x,t)),s),b);}"
            + "  function md5ff(a,b,c,d,x,s,t){return md5cmn((b&c)|((~b)&d),a,b,x,s,t);}"
            + "  function md5gg(a,b,c,d,x,s,t){return md5cmn((b&d)|(c&(~d)),a,b,x,s,t);}"
            + "  function md5hh(a,b,c,d,x,s,t){return md5cmn(b^c^d,a,b,x,s,t);}"
            + "  function md5ii(a,b,c,d,x,s,t){return md5cmn(c^(b|(~d)),a,b,x,s,t);}"
            + "  var n=bytes.length;"
            + "  var bl=n*8;"
            + "  bytes.push(0x80);"
            + "  while(bytes.length%64!==56)bytes.push(0);"
            + "  bytes.push(bl&0xff,(bl>>>8)&0xff,(bl>>>16)&0xff,(bl>>>24)&0xff,0,0,0,0);"
            + "  var a=0x67452301,b=0xEFCDAB89,c=0x98BADCFE,d=0x10325476;"
            + "  for(var i=0;i<bytes.length;i+=64){"
            + "    var w=[];"
            + "    for(var j=0;j<16;j++)w[j]=bytes[i+j*4]|(bytes[i+j*4+1]<<8)|(bytes[i+j*4+2]<<16)|(bytes[i+j*4+3]<<24);"
            + "    var aa=a,bb=b,cc=c,dd=d;"
            + "    a=md5ff(a,b,c,d,w[0],7,-680876936);d=md5ff(d,a,b,c,w[1],12,-389564586);"
            + "    c=md5ff(c,d,a,b,w[2],17,606105819);b=md5ff(b,c,d,a,w[3],22,-1044525330);"
            + "    a=md5ff(a,b,c,d,w[4],7,-176418897);d=md5ff(d,a,b,c,w[5],12,1200080426);"
            + "    c=md5ff(c,d,a,b,w[6],17,-1473231341);b=md5ff(b,c,d,a,w[7],22,-45705983);"
            + "    a=md5ff(a,b,c,d,w[8],7,1770035416);d=md5ff(d,a,b,c,w[9],12,-1958414417);"
            + "    c=md5ff(c,d,a,b,w[10],17,-42063);b=md5ff(b,c,d,a,w[11],22,-1990404162);"
            + "    a=md5ff(a,b,c,d,w[12],7,1804603682);d=md5ff(d,a,b,c,w[13],12,-40341101);"
            + "    c=md5ff(c,d,a,b,w[14],17,-1502002290);b=md5ff(b,c,d,a,w[15],22,1236535329);"
            + "    a=md5gg(a,b,c,d,w[1],5,-165796510);d=md5gg(d,a,b,c,w[6],9,-1069501632);"
            + "    c=md5gg(c,d,a,b,w[11],14,643717713);b=md5gg(b,c,d,a,w[0],20,-373897302);"
            + "    a=md5gg(a,b,c,d,w[5],5,-701558691);d=md5gg(d,a,b,c,w[10],9,38016083);"
            + "    c=md5gg(c,d,a,b,w[15],14,-660478335);b=md5gg(b,c,d,a,w[4],20,-405537848);"
            + "    a=md5gg(a,b,c,d,w[9],5,568446438);d=md5gg(d,a,b,c,w[14],9,-1019803690);"
            + "    c=md5gg(c,d,a,b,w[3],14,-187363961);b=md5gg(b,c,d,a,w[8],20,1163531501);"
            + "    a=md5gg(a,b,c,d,w[13],5,-1444681467);d=md5gg(d,a,b,c,w[2],9,-51403784);"
            + "    c=md5gg(c,d,a,b,w[7],14,1735328473);b=md5gg(b,c,d,a,w[12],20,-1926607734);"
            + "    a=md5hh(a,b,c,d,w[5],4,-378558);d=md5hh(d,a,b,c,w[8],11,-2022574463);"
            + "    c=md5hh(c,d,a,b,w[11],16,1839030562);b=md5hh(b,c,d,a,w[14],23,-35309556);"
            + "    a=md5hh(a,b,c,d,w[1],4,-1530992060);d=md5hh(d,a,b,c,w[4],11,1272893353);"
            + "    c=md5hh(c,d,a,b,w[7],16,-155497632);b=md5hh(b,c,d,a,w[10],23,-1094730640);"
            + "    a=md5hh(a,b,c,d,w[13],4,681279174);d=md5hh(d,a,b,c,w[0],11,-358537222);"
            + "    c=md5hh(c,d,a,b,w[3],16,-722521979);b=md5hh(b,c,d,a,w[6],23,76029189);"
            + "    a=md5hh(a,b,c,d,w[9],4,-640364487);d=md5hh(d,a,b,c,w[12],11,-421815835);"
            + "    c=md5hh(c,d,a,b,w[15],16,530742520);b=md5hh(b,c,d,a,w[2],23,-995338651);"
            + "    a=md5ii(a,b,c,d,w[0],6,-198630844);d=md5ii(d,a,b,c,w[7],10,1126891415);"
            + "    c=md5ii(c,d,a,b,w[14],15,-1416354905);b=md5ii(b,c,d,a,w[5],21,-57434055);"
            + "    a=md5ii(a,b,c,d,w[12],6,1700485571);d=md5ii(d,a,b,c,w[3],10,-1894986606);"
            + "    c=md5ii(c,d,a,b,w[10],15,-1051523);b=md5ii(b,c,d,a,w[1],21,-2054922799);"
            + "    a=md5ii(a,b,c,d,w[8],6,1873313359);d=md5ii(d,a,b,c,w[15],10,-30611744);"
            + "    c=md5ii(c,d,a,b,w[6],15,-1560198380);b=md5ii(b,c,d,a,w[13],21,1309151649);"
            + "    a=md5ii(a,b,c,d,w[4],6,-145523070);d=md5ii(d,a,b,c,w[11],10,-1120210379);"
            + "    c=md5ii(c,d,a,b,w[2],15,718787259);b=md5ii(b,c,d,a,w[9],21,-343485551);"
            + "    a=safeAdd(a,aa);b=safeAdd(b,bb);c=safeAdd(c,cc);d=safeAdd(d,dd);"
            + "  }"
            + "  function hex(n){var s='';for(var i=0;i<4;i++)s+=('0'+((n>>(i*8))&0xFF).toString(16)).slice(-2);return s;}"
            + "  return hex(a)+hex(b)+hex(c)+hex(d);"
            + "}"
            + "return md5(Array.from(input));")
    private static native String md5Hex(int[] input);

}
