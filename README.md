FantasyWear is an Android application to view fantasy sports scores from Yahoo&trade; Fantasy Sports leagues. It supports Android 4.0+ (Ice Cream Sandwich) and all current Android Wear devices. Log into accounts on a phone/tablet and a score card will show up with the current week's results for all leagues for those accounts.

The application is available for free on <a href="https://play.google.com/store/apps/details?id=com.jeffpdavidson.fantasywear">Google Play</a>&trade;. Google Play is a trademark of Google Inc.

Special thanks to the following open source projects which were used to create FantasyWear:
<ul>
<li><a href="https://github.com/square/wire">Wire Mobile Protocol Buffers</a></li>
<li><a href="https://android.googlesource.com/platform/frameworks/volley/">Volley</a></li>
<li><a href="https://github.com/mockito/mockito">Mockito</a></li>
<li><a href="https://github.com/crittercism/dexmaker">Dexmaker</a></li>
<li><a href="https://code.google.com/p/robotium/">Robotium</a></li>
</ul>

Also thanks to <a href="https://openclipart.org/">openclipart</a> for providing images used in the example screenshots.

Compiling FantasyWear
---------------------

First, obtain an API key at the <a href="https://developer.apps.yahoo.com/projects">Yahoo! Developer Network</a>.

To compile a debug APK, the following <a href="https://gradle.org/docs/current/userguide/build_environment.html">gradle properties</a> must be set:
<ul>
<li>com.jeffpdavidson.fantasywear.consumerKey: the OAuth consumer key</li>
<li>com.jeffpdavidson.fantasywear.consumerSecret: the OAuth consumer secret</li>
<li>com.jeffpdavidson.fantasywear.callbackUrl: the OAuth callback URL</li>
</ul>

Then run ./gradlew mobile:assembleDebug wear:assembleDebug to build the mobile and wearable APKs, which must be installed separately.

For release builds, specify a keystore with the com.jeffpdavidson.fantasywear.keystore property. This keystore should have a store password of "jeffpdavidson" and a key with alias/password "fantasywear". Then ./gradlew mobile:assembleRelease will build a release APK with the wear app embedded inside of it, suitable for standalone installation.
