package br.com.wdc.shopping.view.teavm;

import org.teavm.jso.JSBody;

import br.com.wdc.shopping.domain.security.CryptoProvider;

/**
 * Implementação de {@link CryptoProvider} para browser usando implementações
 * JavaScript puras de MD5 e HMAC-SHA256 (síncronas).
 * <p>
 * Não usa Web Crypto API (que é assíncrona) para manter compatibilidade
 * com a interface síncrona existente.
 */
@SuppressWarnings("java:S6126") // String concatenation in @JSBody is intentional for TeaVM JS interop
public class BrowserCryptoProvider implements CryptoProvider {

    @Override
    public byte[] md5(byte[] input) {
        String hex = md5Hex(bytesToJsArray(input));
        return hexToBytes(hex);
    }

    @Override
    public byte[] hmacSha256(byte[] key, byte[] data) {
        String hex = hmacSha256Hex(bytesToJsArray(key), bytesToJsArray(data));
        return hexToBytes(hex);
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
     * MD5 implementado em JavaScript puro (RFC 1321).
     * Retorna hex string.
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

    /**
     * HMAC-SHA256 implementado em JavaScript puro.
     * Retorna hex string.
     */
    @JSBody(params = { "key", "data" }, script = ""
            + "function sha256(bytes){"
            + "  var K=[0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,"
            + "    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,"
            + "    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,"
            + "    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,"
            + "    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,"
            + "    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,"
            + "    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,"
            + "    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2];"
            + "  var H=[0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19];"
            + "  var bl=bytes.length*8;"
            + "  bytes.push(0x80);"
            + "  while(bytes.length%64!==56)bytes.push(0);"
            + "  bytes.push((bl>>>24)&0xff,(bl>>>16)&0xff,(bl>>>8)&0xff,bl&0xff);"
            + "  bytes.push(0,0,0,0);"
            + "  // fix: big-endian 64-bit length\n"
            + "  var last8=bytes.length-8;"
            + "  bytes[last8]=0;bytes[last8+1]=0;bytes[last8+2]=0;bytes[last8+3]=0;"
            + "  bytes[last8+4]=(bl>>>24)&0xff;bytes[last8+5]=(bl>>>16)&0xff;bytes[last8+6]=(bl>>>8)&0xff;bytes[last8+7]=bl&0xff;"
            + "  // undo wrong push above, redo padding\n"
            + "  bytes.length=0;"
            + "  // restart\n"
            + "  return sha256_core(arguments[0]);"
            + "}"
            + "function sha256_core(inputBytes){"
            + "  var K=[0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,"
            + "    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,"
            + "    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,"
            + "    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,"
            + "    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,"
            + "    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,"
            + "    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,"
            + "    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2];"
            + "  var H=[0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19];"
            + "  var bytes=Array.from(inputBytes);"
            + "  var bl=bytes.length*8;"
            + "  bytes.push(0x80);"
            + "  while(bytes.length%64!==56)bytes.push(0);"
            + "  bytes.push(0,0,0,0,(bl>>>24)&0xff,(bl>>>16)&0xff,(bl>>>8)&0xff,bl&0xff);"
            + "  function rotr(n,x){return(x>>>n)|(x<<(32-n));}"
            + "  function ch(x,y,z){return(x&y)^((~x)&z);}"
            + "  function maj(x,y,z){return(x&y)^(x&z)^(y&z);}"
            + "  function sigma0(x){return rotr(2,x)^rotr(13,x)^rotr(22,x);}"
            + "  function sigma1(x){return rotr(6,x)^rotr(11,x)^rotr(25,x);}"
            + "  function gamma0(x){return rotr(7,x)^rotr(18,x)^(x>>>3);}"
            + "  function gamma1(x){return rotr(17,x)^rotr(19,x)^(x>>>10);}"
            + "  for(var i=0;i<bytes.length;i+=64){"
            + "    var w=[];"
            + "    for(var t=0;t<16;t++)w[t]=(bytes[i+t*4]<<24)|(bytes[i+t*4+1]<<16)|(bytes[i+t*4+2]<<8)|bytes[i+t*4+3];"
            + "    for(var t=16;t<64;t++)w[t]=(gamma1(w[t-2])+w[t-7]+gamma0(w[t-15])+w[t-16])|0;"
            + "    var a=H[0],b=H[1],c=H[2],d=H[3],e=H[4],f=H[5],g=H[6],h=H[7];"
            + "    for(var t=0;t<64;t++){"
            + "      var T1=(h+sigma1(e)+ch(e,f,g)+K[t]+w[t])|0;"
            + "      var T2=(sigma0(a)+maj(a,b,c))|0;"
            + "      h=g;g=f;f=e;e=(d+T1)|0;d=c;c=b;b=a;a=(T1+T2)|0;"
            + "    }"
            + "    H[0]=(H[0]+a)|0;H[1]=(H[1]+b)|0;H[2]=(H[2]+c)|0;H[3]=(H[3]+d)|0;"
            + "    H[4]=(H[4]+e)|0;H[5]=(H[5]+f)|0;H[6]=(H[6]+g)|0;H[7]=(H[7]+h)|0;"
            + "  }"
            + "  var hex='';"
            + "  for(var i=0;i<8;i++)hex+=('00000000'+((H[i]>>>0).toString(16))).slice(-8);"
            + "  return hex;"
            + "}"
            + "function hmacSha256(keyBytes,dataBytes){"
            + "  var blockSize=64;"
            + "  var k=Array.from(keyBytes);"
            + "  if(k.length>blockSize){"
            + "    var hk=sha256_core(k);"
            + "    k=[];for(var i=0;i<hk.length;i+=2)k.push(parseInt(hk.substr(i,2),16));"
            + "  }"
            + "  while(k.length<blockSize)k.push(0);"
            + "  var opad=[],ipad=[];"
            + "  for(var i=0;i<blockSize;i++){opad.push(k[i]^0x5c);ipad.push(k[i]^0x36);}"
            + "  var inner=ipad.concat(Array.from(dataBytes));"
            + "  var innerHash=sha256_core(inner);"
            + "  var innerBytes=[];for(var i=0;i<innerHash.length;i+=2)innerBytes.push(parseInt(innerHash.substr(i,2),16));"
            + "  var outer=opad.concat(innerBytes);"
            + "  return sha256_core(outer);"
            + "}"
            + "return hmacSha256(Array.from(key),Array.from(data));")
    private static native String hmacSha256Hex(int[] key, int[] data);

}
