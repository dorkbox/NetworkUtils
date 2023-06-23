/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dorkbox.netUtil.web


import dorkbox.netUtil.Dns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.UnknownHostException
import java.security.cert.X509Certificate
import java.util.regex.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Suppress("unused")
object WebUtil {
    private val SECOND_LEVEL_DOMAIN_PATTERN = Pattern.compile("^(https?:\\/\\/)?([\\dA-Za-z\\.-]+)\\.([a-z\\.]{2,6})([\\w \\.-]*)*$")

    /**
     * Regular expression to match all IANA top-level domains.
     * List accurate as of 2010/02/05.  List taken from:
     * http://data.iana.org/TLD/tlds-alpha-by-domain.txt
     * This pattern is auto-generated by frameworks/base/common/tools/make-iana-tld-pattern.py
     */
    @Volatile
    private var TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL = ("((aaa|aarp|abarth|abb|abbott|abbvie|abc|able|abogado|abudhabi|academy|accenture|accountant|accountants|aco|actor|adac|ads|adult|aeg|aero|aetna|afamilycompany|afl|africa|agakhan|agency|aig|airbus|airforce|airtel|akdn|alfaromeo|alibaba|alipay|allfinanz|allstate|ally|alsace|alstom|amazon|americanexpress|americanfamily|amex|amfam|amica|amsterdam|analytics|android|anquan|anz|aol|apartments|app|apple|aquarelle|arab|aramco|archi|army|arpa|art|arte|asda|asia|associates|athleta|attorney|auction|audi|audible|audio|auspost|author|auto|autos|avianca|aws|axa|azure|a[cdefgilmoqrstuwxz])"
            + "|(baby|baidu|banamex|bananarepublic|band|bank|bar|barcelona|barclaycard|barclays|barefoot|bargains|baseball|basketball|bauhaus|bayern|bbc|bbt|bbva|bcg|bcn|beats|beauty|beer|bentley|berlin|best|bestbuy|bet|bharti|bible|bid|bike|bing|bingo|bio|biz|black|blackfriday|blockbuster|blog|bloomberg|blue|bms|bmw|bnpparibas|boats|boehringer|bofa|bom|bond|boo|book|booking|bosch|bostik|boston|bot|boutique|box|bradesco|bridgestone|broadway|broker|brother|brussels|budapest|bugatti|build|builders|business|buy|buzz|bzh|b[abdefghijmnorstvwyz])"
            + "|(cab|cafe|cal|call|calvinklein|cam|camera|camp|cancerresearch|canon|capetown|capital|capitalone|car|caravan|cards|care|career|careers|cars|casa|case|caseih|cash|casino|cat|catering|catholic|cba|cbn|cbre|cbs|ceb|center|ceo|cern|cfa|cfd|chanel|channel|charity|chase|chat|cheap|chintai|christmas|chrome|church|cipriani|circle|cisco|citadel|citi|citic|city|cityeats|claims|cleaning|click|clinic|clinique|clothing|cloud|club|clubmed|coach|codes|coffee|college|cologne|com|comcast|commbank|community|company|compare|computer|comsec|condos|construction|consulting|contact|contractors|cooking|cookingchannel|cool|coop|corsica|country|coupon|coupons|courses|cpa|credit|creditcard|creditunion|cricket|crown|crs|cruise|cruises|csc|cuisinella|cymru|cyou|c[acdfghiklmnoruvwxyz])"
            + "|(dabur|dad|dance|data|date|dating|datsun|day|dclk|dds|deal|dealer|deals|degree|delivery|dell|deloitte|delta|democrat|dental|dentist|desi|design|dev|dhl|diamonds|diet|digital|direct|directory|discount|discover|dish|diy|dnp|docs|doctor|dog|domains|dot|download|drive|dtv|dubai|duck|dunlop|dupont|durban|dvag|dvr|d[ejkmoz])"
            + "|(earth|eat|eco|edeka|edu|education|email|emerck|energy|engineer|engineering|enterprises|epson|equipment|ericsson|erni|esq|estate|etisalat|eurovision|eus|events|exchange|expert|exposed|express|extraspace|e[cegrstu])"
            + "|(fage|fail|fairwinds|faith|family|fan|fans|farm|farmers|fashion|fast|fedex|feedback|ferrari|ferrero|fiat|fidelity|fido|film|final|finance|financial|fire|firestone|firmdale|fish|fishing|fit|fitness|flickr|flights|flir|florist|flowers|fly|foo|food|foodnetwork|football|ford|forex|forsale|forum|foundation|fox|free|fresenius|frl|frogans|frontdoor|frontier|ftr|fujitsu|fujixerox|fun|fund|furniture|futbol|fyi|f[ijkmor])"
            + "|(gal|gallery|gallo|gallup|game|games|gap|garden|gay|gbiz|gdn|gea|gent|genting|george|ggee|gift|gifts|gives|giving|glade|glass|gle|global|globo|gmail|gmbh|gmo|gmx|godaddy|gold|goldpoint|golf|goo|goodyear|goog|google|gop|got|gov|grainger|graphics|gratis|green|gripe|grocery|group|guardian|gucci|guge|guide|guitars|guru|g[abdefghilmnpqrstuwy])"
            + "|(hair|hamburg|hangout|haus|hbo|hdfc|hdfcbank|health|healthcare|help|helsinki|here|hermes|hgtv|hiphop|hisamitsu|hitachi|hiv|hkt|hockey|holdings|holiday|homedepot|homegoods|homes|homesense|honda|horse|hospital|host|hosting|hot|hoteles|hotels|hotmail|house|how|hsbc|hughes|hyatt|hyundai|h[kmnrtu])"
            + "|(ibm|icbc|ice|icu|ieee|ifm|ikano|imamat|imdb|immo|immobilien|inc|industries|infiniti|info|ing|ink|institute|insurance|insure|int|intel|international|intuit|investments|ipiranga|irish|ismaili|ist|istanbul|itau|itv|iveco|i[delmnoqrst])"
            + "|(jaguar|java|jcb|jcp|jeep|jetzt|jewelry|jio|jll|jmp|jnj|jobs|joburg|jot|joy|jpmorgan|jprs|juegos|juniper|j[emop])"
            + "|(kaufen|kddi|kerryhotels|kerrylogistics|kerryproperties|kfh|kia|kim|kinder|kindle|kitchen|kiwi|koeln|komatsu|kosher|kpmg|kpn|krd|kred|kuokgroup|kyoto|k[eghimnprwyz])"
            + "|(lacaixa|lamborghini|lamer|lancaster|lancia|land|landrover|lanxess|lasalle|lat|latino|latrobe|law|lawyer|lds|lease|leclerc|lefrak|legal|lego|lexus|lgbt|lidl|life|lifeinsurance|lifestyle|lighting|like|lilly|limited|limo|lincoln|linde|link|lipsy|live|living|lixil|llc|llp|loan|loans|locker|locus|loft|lol|london|lotte|lotto|love|lpl|lplfinancial|ltd|ltda|lundbeck|lupin|luxe|luxury|l[abcikrstuvy])"
            + "|(macys|madrid|maif|maison|makeup|man|management|mango|map|market|marketing|markets|marriott|marshalls|maserati|mattel|mba|mckinsey|med|media|meet|melbourne|meme|memorial|men|menu|merckmsd|miami|microsoft|mil|mini|mint|mit|mitsubishi|mlb|mls|mma|mobi|mobile|moda|moe|moi|mom|monash|money|monster|mormon|mortgage|moscow|moto|motorcycles|mov|movie|msd|mtn|mtr|museum|mutual|m[acdeghklmnopqrstuvwxyz])"
            + "|(nab|nagoya|name|nationwide|natura|navy|nba|nec|net|netbank|netflix|network|neustar|new|newholland|news|next|nextdirect|nexus|nfl|ngo|nhk|nico|nike|nikon|ninja|nissan|nissay|nokia|northwesternmutual|norton|now|nowruz|nowtv|nra|nrw|ntt|nyc|n[acefgilopruz])"
            + "|(obi|observer|off|office|okinawa|olayan|olayangroup|oldnavy|ollo|omega|one|ong|onl|online|onyourside|ooo|open|oracle|orange|org|organic|origins|osaka|otsuka|ott|ovh|om)"
            + "|(page|panasonic|paris|pars|partners|parts|party|passagens|pay|pccw|pet|pfizer|pharmacy|phd|philips|phone|photo|photography|photos|physio|pics|pictet|pictures|pid|pin|ping|pink|pioneer|pizza|place|play|playstation|plumbing|plus|pnc|pohl|poker|politie|porn|post|pramerica|praxi|press|prime|pro|prod|productions|prof|progressive|promo|properties|property|protection|pru|prudential|pub|pwc|p[aefghklmnrstwy])"
            + "|(qpon|quebec|quest|qvc|qa)"
            + "|(racing|radio|raid|read|realestate|realtor|realty|recipes|red|redstone|redumbrella|rehab|reise|reisen|reit|reliance|ren|rent|rentals|repair|report|republican|rest|restaurant|review|reviews|rexroth|rich|richardli|ricoh|ril|rio|rip|rmit|rocher|rocks|rodeo|rogers|room|rsvp|rugby|ruhr|run|rwe|ryukyu|r[eosuw])"
            + "|(saarland|safe|safety|sakura|sale|salon|samsclub|samsung|sandvik|sandvikcoromant|sanofi|sap|sarl|sas|save|saxo|sbi|sbs|sca|scb|schaeffler|schmidt|scholarships|school|schule|schwarz|science|scjohnson|scot|search|seat|secure|security|seek|select|sener|services|ses|seven|sew|sex|sexy|sfr|shangrila|sharp|shaw|shell|shia|shiksha|shoes|shop|shopping|shouji|show|showtime|shriram|silk|sina|singles|site|ski|skin|sky|skype|sling|smart|smile|sncf|soccer|social|softbank|software|sohu|solar|solutions|song|sony|soy|space|sport|spot|spreadbetting|srl|stada|staples|star|statebank|statefarm|stc|stcgroup|stockholm|storage|store|stream|studio|study|style|sucks|supplies|supply|support|surf|surgery|suzuki|swatch|swiftcover|swiss|sydney|systems|s[abcdeghijklmnorstuvxyz])"
            + "|(tab|taipei|talk|taobao|target|tatamotors|tatar|tattoo|tax|taxi|tci|tdk|team|tech|technology|tel|temasek|tennis|teva|thd|theater|theatre|tiaa|tickets|tienda|tiffany|tips|tires|tirol|tjmaxx|tjx|tkmaxx|tmall|today|tokyo|tools|top|toray|toshiba|total|tours|town|toyota|toys|trade|trading|training|travel|travelchannel|travelers|travelersinsurance|trust|trv|tube|tui|tunes|tushu|tvs|t[cdfghjklmnortvwz])"
            + "|(ubank|ubs|unicom|university|uno|uol|ups|u[agksyz])"
            + "|(vacations|vana|vanguard|vegas|ventures|verisign|versicherung|vet|viajes|video|vig|viking|villas|vin|vip|virgin|visa|vision|viva|vivo|vlaanderen|vodka|volkswagen|volvo|vote|voting|voto|voyage|vuelos|v[aceginu])"
            + "|(wales|walmart|walter|wang|wanggou|watch|watches|weather|weatherchannel|webcam|weber|website|wed|wedding|weibo|weir|whoswho|wien|wiki|williamhill|win|windows|wine|winners|wme|wolterskluwer|woodside|work|works|world|wow|wtc|wtf|w[fs])"
            + "|(xbox|xerox|xfinity|xihuan|xin|xn\\-\\-11b4c3d|xn\\-\\-1ck2e1b|xn\\-\\-1qqw23a|xn\\-\\-2scrj9c|xn\\-\\-30rr7y|xn\\-\\-3bst00m|xn\\-\\-3ds443g|xn\\-\\-3e0b707e|xn\\-\\-3hcrj9c|xn\\-\\-3oq18vl8pn36a|xn\\-\\-3pxu8k|xn\\-\\-42c2d9a|xn\\-\\-45br5cyl|xn\\-\\-45brj9c|xn\\-\\-45q11c|xn\\-\\-4gbrim|xn\\-\\-54b7fta0cc|xn\\-\\-55qw42g|xn\\-\\-55qx5d|xn\\-\\-5su34j936bgsg|xn\\-\\-5tzm5g|xn\\-\\-6frz82g|xn\\-\\-6qq986b3xl|xn\\-\\-80adxhks|xn\\-\\-80ao21a|xn\\-\\-80aqecdr1a|xn\\-\\-80asehdb|xn\\-\\-80aswg|xn\\-\\-8y0a063a|xn\\-\\-90a3ac|xn\\-\\-90ae|xn\\-\\-90ais|xn\\-\\-9dbq2a|xn\\-\\-9et52u|xn\\-\\-9krt00a|xn\\-\\-b4w605ferd|xn\\-\\-bck1b9a5dre4c|xn\\-\\-c1avg|xn\\-\\-c2br7g|xn\\-\\-cck2b3b|xn\\-\\-cckwcxetd|xn\\-\\-cg4bki|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-czr694b|xn\\-\\-czrs0t|xn\\-\\-czru2d|xn\\-\\-d1acj3b|xn\\-\\-d1alf|xn\\-\\-e1a4c|xn\\-\\-eckvdtc9d|xn\\-\\-efvy88h|xn\\-\\-fct429k|xn\\-\\-fhbei|xn\\-\\-fiq228c5hs|xn\\-\\-fiq64b|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fjq720a|xn\\-\\-flw351e|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-fzys8d69uvgm|xn\\-\\-g2xx48c|xn\\-\\-gckr3f0f|xn\\-\\-gecrj9c|xn\\-\\-gk3at1e|xn\\-\\-h2breg3eve|xn\\-\\-h2brj9c|xn\\-\\-h2brj9c8c|xn\\-\\-hxt814e|xn\\-\\-i1b6b1a6a2e|xn\\-\\-imr513n|xn\\-\\-io0a7i|xn\\-\\-j1aef|xn\\-\\-j1amh|xn\\-\\-j6w193g|xn\\-\\-jlq480n2rg|xn\\-\\-jlq61u9w7b|xn\\-\\-jvr189m|xn\\-\\-kcrx77d1x4a|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-kput3i|xn\\-\\-l1acc|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgb9awbf|xn\\-\\-mgba3a3ejt|xn\\-\\-mgba3a4f16a|xn\\-\\-mgba7c0bbn0a|xn\\-\\-mgbaakc7dvf|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbab2bd|xn\\-\\-mgbah1a3hjkrd|xn\\-\\-mgbai9azgqp6j|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbbh1a|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgbca7dzdo|xn\\-\\-mgbcpq6gpa1a|xn\\-\\-mgberp4a5d4ar|xn\\-\\-mgbgu82a|xn\\-\\-mgbi4ecexp|xn\\-\\-mgbpl2fh|xn\\-\\-mgbt3dhd|xn\\-\\-mgbtx2b|xn\\-\\-mgbx4cd0ab|xn\\-\\-mix891f|xn\\-\\-mk1bu44c|xn\\-\\-mxtq1m|xn\\-\\-ngbc5azd|xn\\-\\-ngbe9e0a|xn\\-\\-ngbrx|xn\\-\\-node|xn\\-\\-nqv7f|xn\\-\\-nqv7fs00ema|xn\\-\\-nyqy26a|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-otu796d|xn\\-\\-p1acf|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-pssy2u|xn\\-\\-q7ce6a|xn\\-\\-q9jyb4c|xn\\-\\-qcka1pmc|xn\\-\\-qxa6a|xn\\-\\-qxam|xn\\-\\-rhqv96g|xn\\-\\-rovu88b|xn\\-\\-rvc1e0am3e|xn\\-\\-s9brj9c|xn\\-\\-ses554g|xn\\-\\-t60b56a|xn\\-\\-tckwe|xn\\-\\-tiq49xqyj|xn\\-\\-unup4y|xn\\-\\-vermgensberater\\-ctb|xn\\-\\-vermgensberatung\\-pwb|xn\\-\\-vhquv|xn\\-\\-vuq861b|xn\\-\\-w4r85el8fhu5dnra|xn\\-\\-w4rs40l|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xhq521b|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-y9a3aq|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zfr164b|xxx|xyz)"
            + "|(yachts|yahoo|yamaxun|yandex|yodobashi|yoga|yokohama|you|youtube|yun|y[et])"
            + "|(zappos|zara|zero|zip|zone|zuerich|z[amw])))")


    /**
     * Good characters for Internationalized Resource Identifiers (IRI).
     * This comprises most common used Unicode characters allowed in IRI
     * as detailed in RFC 3987.
     * Specifically, those two byte Unicode characters are not included.
     */
    const val GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF"



    /**
     * Marks the WEB_URL pattern as dirty, and will recompile it on its next usage
     */
    @Volatile
    private var MARK_URL_PATTERN_DIRTY = false

    /**
     *  Regular expression pattern to match most part of RFC 3987
     *  Internationalized URLs, aka IRIs.  Commonly used Unicode characters are
     *  added.
     */
    @Volatile
    private var WEB_URL = compileWebUrl()

    /**
     * Updates the web URL mega-regex, and marks usages as dirty (so they are updated)
     */
    fun updateWebUrlRegex(topLeveDomainUrls: String) {
        TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL = topLeveDomainUrls
        MARK_URL_PATTERN_DIRTY = true // update the next time we use it.
    }


    private fun compileWebUrl(): Pattern {
        return Pattern.compile(
            "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                    + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                    + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                    + "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+" // named host
                    + TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
                    + "|(?:(?:25[0-5]|2[0-4]" // or ip address
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
                    + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9])))"
                    + "(?:\\:\\d{1,5})?)" // plus option port number
                    + "(\\/(?:(?:[a-zA-Z0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~" // plus option query params
                    + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                    + "(?:\\b|$)")

    }

    /**
     * Only removes the path and query parameters. Only the transport + domain remain.
     * ie:
     * http://foo.com/index.php  -->  http://foo.com
     * https://www.aa.foo.com/index.php  --> https://www.aa.foo.com
     * https://www.aa.foo.com/index&foo%bar --> https://www.aa.foo.com
     * https://www.aa.foo.com%foobar --> https://www.aa.foo.com
     */
    fun cleanupAndRemovePath(fullDomainName: String): String {
        var start = fullDomainName.indexOf("://")
        if (start == -1) {
            start = 0
        }
        else {
            start += 3 // 3 is the length of ://
        }

        var end = fullDomainName.length

        val slash = fullDomainName.indexOf("/", start + 3)
        if (slash > -1 && slash < end) {
            end = slash
        }

        val colon = fullDomainName.indexOf(":", start + 3)
        if (colon > -1 && colon < end) {
            end = colon
        }

        val percent = fullDomainName.indexOf("%", start)
        if (percent > -1 && percent < end) {
            end = percent
        }

        val ampersand = fullDomainName.indexOf("&", start)
        if (ampersand > -1 && ampersand < end) {
            end = ampersand
        }

        val question = fullDomainName.indexOf("?", start)
        if (question > -1 && question < end) {
            end = question
        }


        return fullDomainName.substring(0, end)
    }



    /**
     * Only removes http?s:// and the path (if it's present) and www. (if it's present). Also removes *. (if it's present)
     * ie:
     * http://foo.com/index.php  -->  foo.com
     * https://www.aa.foo.com/index.php  --> aa.foo.com
     * https://www.aa.foo.com/index&foo%bar --> aa.foo.com
     * https://www.aa.foo.com%foobar --> aa.foo.com
     */
    fun cleanupAndRemoveWwwAndPath(fullDomainName: String): String {
        var start = fullDomainName.indexOf("://")
        if (start == -1) {
            start = 0
        }
        else {
            start += 3 // 3 is the length of ://
        }

        // get rid of the www. part if it exists.
        val www = fullDomainName.indexOf("www.", start)
        if (www > -1 && www <= 8) {
            start = www + 4 // 4 is the length of www.
        }

        val star = fullDomainName.indexOf("*.", start)
        if (star > -1) {
            start = star + 2 // 2 is the length of *.
        }

        var end = fullDomainName.length

        val slash = fullDomainName.indexOf("/", start + 3)
        if (slash > -1 && slash < end) {
            end = slash
        }

        val colon = fullDomainName.indexOf(":", start + 3)
        if (colon > -1 && colon < end) {
            end = colon
        }

        val percent = fullDomainName.indexOf("%", start)
        if (percent > -1 && percent < end) {
            end = percent
        }

        val ampersand = fullDomainName.indexOf("&", start)
        if (ampersand > -1 && ampersand < end) {
            end = ampersand
        }

        val question = fullDomainName.indexOf("?", start)
        if (question > -1 && question < end) {
            end = question
        }


        return fullDomainName.substring(start, end)
    }

    /**
     * Only removes http?s:// and www. (if it's present). Also removes *. (if it's present)
     * ie:
     * http://foo.com/index.php  -->  foo.com/index.php
     * https://www.aa.foo.com/index.php  --> aa.foo.com/index.php
     * https://www.aa.foo.com/index&foo%bar --> aa.foo.com/index&foo%bar
     * https://www.aa.foo.com%foobar --> aa.foo.com%foobar
     */
    fun cleanupAndPreservePath(fullDomainName: String, removeQueryString: Boolean = true): String {
        var start = fullDomainName.indexOf("://")
        if (start == -1) {
            start = 0
        }
        else {
            start += 3 // 3 is the length of ://
        }

        // get rid of the www. part if it exists.
        val www = fullDomainName.indexOf("www.", start)
        if (www > -1 && www <= 8) {
            start = www + 4 // 4 is the length of www.
        }

        val star = fullDomainName.indexOf("*.", start)
        if (star > -1) {
            start = star + 2 // 2 is the length of *.
        }

        var end = if (removeQueryString) {
            var end = fullDomainName.length

            val percent = fullDomainName.indexOf("%", start)
            if (percent > -1 && percent < end) {
                end = percent
            }

            val ampersand = fullDomainName.indexOf("&", start)
            if (ampersand > -1 && ampersand < end) {
                end = ampersand
            }

            val question = fullDomainName.indexOf("?", start)
            if (question > -1 && question < end) {
                end = question
            }

            end
        } else {
            fullDomainName.length
        }

        // If the last char is a /, remove it
        if (end -1 >= 0 && fullDomainName[end - 1] == '/') {
            end--
        }

        return fullDomainName.substring(start, end)
    }


    /**
     * Only removes www. (if it's present). Also removes *. (if it's present)
     *
     *
     * ie:
     * foo.com/index.php  -->  foo.com
     * www.aa.foo.com/index.php  --> aa.foo.com
     * www.aa.foo.com/index&foo%bar --> aa.foo.com
     * www.aa.foo.com%foobar --> aa.foo.com
     *
     *
     * NOTE: ONLY use this if you can GUARANTEE that there is no http?s://
     */
    fun removeWww(fullDomainName: String?): String? {
        if (fullDomainName == null) {
            return null
        }

        // get rid of the www. part if it exists.
        var start = fullDomainName.indexOf("www.")
        if (start > -1) {
            start += 4 // 4 is the length of www.
        }
        else {
            start = 0
        }

        val star = fullDomainName.indexOf("*.", start)
        if (star > -1) {
            start = star + 2 // 2 is the length of *.
        }

        var end = fullDomainName.indexOf("/", start + 3)
        if (end == -1) {
            if (start == 0) {
                // it was already clean.
                return fullDomainName
            }

            end = fullDomainName.length
        }

        val percent = fullDomainName.indexOf("%", start)
        if (percent > -1 && percent < end) {
            end = percent
        }

        return fullDomainName.substring(start, end)
    }

    fun isValidUrl(url: String?): Boolean {
        return if (url.isNullOrEmpty()) {
            false // Don't even need to check, not a valid domain
        }
        else {
            if (MARK_URL_PATTERN_DIRTY) {
                // race conditions don't matter, this just guarantees that it's updated.
                WEB_URL = compileWebUrl()
                MARK_URL_PATTERN_DIRTY = false
            }

            val m = WEB_URL.matcher(url)
            m.matches()
        }
    }

    fun isSubDomain(fullDomainName: String): Boolean {
        var start = fullDomainName.indexOf("://")
        if (start == -1) {
            start = 0
        }
        else {
            start += 3
        }

        if (fullDomainName.contains("www.")) {
            start += 4 // 4 is the length of www.
        }

        var end = fullDomainName.indexOf("/", start + 3)
        if (end == -1) {
            end = fullDomainName.length
        }

        val substring = fullDomainName.substring(start, end)

        val dots = substring.count { it == '.' }

        return dots > 1
    }

    /**
     * Only remove http?s://www and the path (if it's present).
     * Get the next level domain after cleanup if next level domain is not top level domain.
     * ie:
     * http://www.a.b.foo.com -> b.foo.com
     * https://www.foo.com -> foo.com
     * foo.com -> foo.com
     */

    fun cleanupAndGetNextLevelDomain(fullDomainName: String): String? {
        var start = fullDomainName.indexOf("://")
        if (start == -1) {
            start = 0
        }
        else {
            start += 3
        }

        if (fullDomainName.contains("www.")) {
            start += 4 // 4 is the length of www.
        }

        var end = fullDomainName.indexOf("/", start + 3)
        if (end == -1) {
            end = fullDomainName.length
        }

        var substring = fullDomainName.substring(start, end)
        val last = substring

        val nextDot = substring.indexOf(".")
        if (nextDot == -1) {
            return null
        }

        substring = substring.substring(nextDot + 1)

        if (Dns.isTLD(substring)) {
            substring = last
        }

        return substring
    }

    fun getNextLevelDomain(fullDomainName: String): String? {
        val nextDot = fullDomainName.indexOf(".")
        if (nextDot == -1) {
            return null
        }

        return fullDomainName.substring(nextDot + 1)
    }

    /**
     * Only removes http?s:// and the path (if it's present).
     * ie:
     * http://foo.com/index.php  -->  foo.com
     * https://www.aa.foo.com/index.php  --> foo.com
     */
    fun cleanupAndGetSecondLevelDomain(fullDomainName: String): String? {
        // File URLs will return null at the extractSLD step, so this case is explicitly for logging purposes.
        // We want to know when the returned value is null because it's a file, vs returning null for other reasons.
        if (fullDomainName.startsWith("file://", true)){
            return null
        }

        var start = fullDomainName.indexOf("://")
        if (start == -1) {
            start = 0
        }
        else {
            start += 3
        }

        var end = fullDomainName.indexOf("/", start + 3)
        if (end == -1) {
            if (start == 0) {
                // it was already clean.
                return Dns.extractSLD(fullDomainName)
            }

            end = fullDomainName.length
        }

        // for now, get the SLD as well
        val substring = fullDomainName.substring(start, end)
        return Dns.extractSLD(substring)
    }

    /**
     * Get the third level domain of google domains if it has one.
     * ie:
     * http://google.com/index.php -> google.com
     * http://docs.google.com/index.php -> docs.google.com
     * https://32.32.432.fdsa.docs.google.com/index.php -> docs.google.com
     */

    fun cleanupAndGetThirdLevelDomain(fullDomainName: String): String {
        var cleanDomain = cleanupAndRemoveWwwAndPath(fullDomainName)

        val periodCount = cleanDomain.count { it == '.'}

        if (periodCount <= 2) {
            return cleanDomain
        }


        for (x in periodCount downTo 3) {
            val nextDot = cleanDomain.indexOf(".")

            cleanDomain = cleanDomain.substring(nextDot + 1)
        }

        return cleanDomain
    }

    /**
     * Get the last portion of the file uri, the file name itself.
     * ie:
     * file://Downloads/example.pdf -> example.pdf
     * file:///media.jpg -> media.jpg
     */
    fun cleanupFileUri(domain: String): String {
        val lastSlashIndex = domain.lastIndexOf("/")

        if (lastSlashIndex == -1) {
            return domain
        }

        return domain.substring(lastSlashIndex + 1)
    }


    fun forceAcceptAllTlsCertificates() {
        /*
         *  fix for
         *    Exception in thread "main" javax.net.ssl.SSLHandshakeException:
         *       sun.security.validator.ValidatorException:
         *           PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException:
         *               unable to find valid certification path to requested target
         */
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }

            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}

            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        })


        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

        // Create all-trusting host name verifier
        val allHostsValid = HostnameVerifier { _, _ -> true }

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
    }



//    @JvmStatic
//    fun main(args: Array<String>) {
//        println(cleanupAndPreservePath("https://www.youtube.com/watch?v=YP6EaIDlmEg&t=1s", removeQueryString = true))
//        println(cleanupAndPreservePath("https://www.khanacademy.org/", removeQueryString = true))
//        println(cleanupAndRemoveWwwAndPath("https://sat184.cloud1.tds.airast.org/student/V746/Pages/TestShell.aspx"))
//        println(cleanupAndRemoveWwwAndPath("https://sat184.cloud1.tds.airast.org/student/V746/Pages/TestShell.aspx"))
//
//    }

//        println(WEB_URL.matcher("https://www.youtube.com/watch?v=WEVctuQTeaI").matches())
//        println(WEB_URL.matcher("www.youtube.com/watch?v=WEVctuQTeaI").matches())
//        println(WEB_URL.matcher("youtube.com/watch?v=WEVctuQTeaI").matches())
//        println(WEB_URL.matcher("youtube.com").matches())
//        println(WEB_URL.matcher("https://www.espn.com/nba/").matches())
//        println(WEB_URL.matcher("https://www.espn.com/nba").matches())
//        println(getNextLevelDomain("admin.regression.net-ref.com"))
//        println(cleanupAndGetGoogleDomain("https://www.google.com/search?rlz=1CAZGSZ_enUS848&tbm=isch&q=pretty+backgrounds&chips=q:pretty+backgrounds,g_1:iphone:lJzZkCc6kg8%3D&usg=AI4_-kSfq6w5oVz33oUhcFfHeJC-MtmIww&sa=X&ved=0ahUKEwi0hP-Sk4riAhUUpJ4KHaWJDi0Q4lYIJigA&biw=1517&bih=695&dpr=0.9&safe=active&ssui=on"));
//        println(cleanupAndRemoveWww("http://fasttmath.capousd.org:55880/fmng/loader/"))
//        println(cleanupAndRemoveWww("http://fasttmath.capousd.org:55880/fmng/loader/"))
//        println(cleanupAndRemoveWww("http://fasttmath.capousd.org:55880/fmng/loader/"))
//        println(cleanupAndRemoveWww("https://clever.com/oauth/authorize?channel=clever-portal&client_id=8c54ced0462a3fe2da0a&confirmed=true&district_id=556cc0739496cf01000003f2" +
//                "&redirect_uri=https%3A%2F%2Fapp.typingagent.com%2Fclever%2Findex%3Foauth%3Dtrue&response_type=code"))
//        println(cleanupAndRemoveWww(
//                "https://www.clever.com/oauth/authorize?channel=clever-portal&client_id=ae17f3b6f000d1bb4f2c&confirmed=true&district_id=556cc0739496cf01000003f2&redirect_uri=https%3A%2F%2Fwww" +
//                        ".khanacademy.org%2Flogin%2Fclever&response_type=code"))
//        println(cleanupAndRemoveWww(cleanupAndRemoveWww("https://sat184.cloud1.tds.airast.org/student/V746/Pages/TestShell.aspx")))
//
//        println(cleanupAndPreservePath("http://fasttmath.capousd.org:55880/fmng/loader/"))
//        println(cleanupAndPreservePath(
//                "https://www.clever.com/oauth/authorize?channel=clever-portal&client_id=ae17f3b6f000d1bb4f2c&confirmed=true&district_id=556cc0739496cf01000003f2&redirect_uri=https%3A%2F%2Fwww" +
//                        ".khanacademy.org%2Flogin%2Fclever&response_type=code"))

//    }


    /**
     * Runs the 'action' function when the scheme+domain+path(s) when it was successful. Runs the 'onError' function when it fails.
     */
    suspend fun fetchData(scheme: String, domain: String, vararg paths: String, retryCount: Int = 10,
                          onError: (String) ->Unit,
                          onSuccess: suspend (InputStream)->Unit) = withContext(Dispatchers.IO) {
        val encodedPath = paths.joinToString(separator = "/") { URLEncoder.encodePathSegment(it, Charsets.UTF_8) }
        var location = "$scheme://$domain/$encodedPath"
        var alreadyTriedOtherScheme = false

//        logger.trace{ "Getting data: $location" }

        // We DO want to support redirects, in case OLD code is running in the wild.
        var base: URL
        var next: URL
        var visitedCount = 0

        while (true) {
            visitedCount += 1
            if (visitedCount > retryCount)  {
                onError("Stuck in a loop for '$location'  ---   more than $visitedCount attempts")
                return@withContext
            }

            try {
                base = URL(location)
                with(base.openConnection() as HttpURLConnection) {
                    useCaches = false
                    instanceFollowRedirects = true

//                    if (logger.isTraceEnabled) {
//                        logger.trace { "Requesting URL : $url" }
//                        logger.trace { "Response Code : $responseCode" }
//                    }

                    when (responseCode) {
                        HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                            location = getHeaderField("Location")
                            // java.net.URLDecoder is only valid for query parameters/headers -- NOT FOR ACTUAL URLS!
                            location = URLDecoder.decode(location, "US-ASCII")


                            // logger.trace { "Response to '$url' redirected to '$location'" }

                            next = URL(base, location) // Deal with relative URLs
                            location = next.toExternalForm()

                            // loop again with the new location
                            return@with
                        }
                        HttpURLConnection.HTTP_OK -> {
                            inputStream.use {
                                onSuccess(it)
                            }

                            // done
                            return@withContext
                        }
                        HttpsURLConnection.HTTP_NOT_FOUND -> {
                            if (alreadyTriedOtherScheme) {
                                onError("Error '$responseCode' getting location '$location'  HTTPS option exhausted.")

                                // done
                                return@withContext
                            }

                            // if we are HTTPS, retry again as HTTP.
                            alreadyTriedOtherScheme = true
                            visitedCount = 0

                            location = if (location.startsWith("https")) {
                                "http://$domain/$encodedPath"
                            } else {
                                "https://$domain/$encodedPath"
                            }

                            // loop again with the new location
                            return@with
                        }
                        else -> {
                            onError("Error '$responseCode' getting location '$location'")

                            // done
                            return@withContext
                        }
                    }
                }
            }
            catch (e: UnknownHostException) {
                // TMI for what's going on. We just can't, so leave it at that.
                onError("Failed to retrieve or write icon for location: '${location}'")
                return@withContext
            }
            catch (e: Exception) {
                onError("Failed to retrieve or write icon for location: '${location}'. ${e.message}")
                return@withContext
            }
        }

        @Suppress("UNREACHABLE_CODE")
        null
    }
}
