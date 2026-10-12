package com.rov3r.extension.vsco;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Verifies device-bound, manually issued offline subscriber authorizations. */
@SuppressWarnings("unused")
public final class OfflineAuthorization {
    private static final String INSTALLATION_ID_KEY = "offline_auth_installation_id";
    private static final String INSTALLATION_KEY_ALIAS = "rov3r_vsco_offline_installation_v1";
    private static final String LICENSE_KEY = "offline_auth_license";
    private static final String LAST_SEEN_TIME_KEY = "offline_auth_last_seen_seconds";

    private static final String REQUEST_PREFIX = "vsco-offline-request-v1.";
    private static final String LICENSE_PREFIX = "vsco-offline-license-v1.";
    private static final String PACKAGE_NAME = "com.vsco.cam";
    private static final String ISSUER_KEY_ID = "2a0b9d13332a8fa2";
    private static final String PUBLIC_EXPONENT = "AQAB";
    private static final String PUBLIC_MODULUS =
        "zItT9GqrFJYU3U+ImohflfQiNtLdjYL2FO8CdfaiKkPfo/QHebm8QUU6Fu74XpO1UoM3KLvDCydOPj36rkwjpes1czR+rDrOrEVduiOzGHIyYA8sBxxLjQGOuyuszLzhYFqgOqGrS8tO3siS2gu60TYPNJwAAmqXeFibgVkJELi9JvEtUFQ3vsmr870LA8C+YXpTT9VvE9slcx33nAiZOjQ0z2EqCUI3uiVcrpL+R9Q73YNSfVtQaaHbVq4QEdPbLbtvK59xZGGCo0UcM4IcXpH4DprHv4QD0DRozjP4grdlkzBfjpmIVlChLRQCdboc6k5JveZpb06fLhPXlPhSC5Zs+HFLY6GBjnp2CymNtdrjrlpZCTNIx1UBJe1PzPrwuY5ibZ7D9stjI3pfj6KujDFyEacAgpNOX+13YZGJAYb8R/g3Sfa+T0YWZg57xVcbRanNYuaPU7QSBVn+ytNapT9BIbdL5V3842eRXiE6NXJcCov8tFyfAbz3SzmETGst";

    private static volatile Context applicationContext;
    private static volatile Authorization cachedAuthorization;

    private OfflineAuthorization() {
    }

    public static void initialize(Context context) {
        if (context == null) {
            return;
        }
        applicationContext = context.getApplicationContext();
        cachedAuthorization = readStoredAuthorization(applicationContext);
    }

    public static String activationRequest(Context context) {
        initialize(context);
        try {
            JSONObject request = new JSONObject();
            request.put("schema", 1);
            request.put("installationId", installationId(context));
            request.put("packageName", PACKAGE_NAME);
            request.put("createdAt", System.currentTimeMillis() / 1000L);
            return REQUEST_PREFIX + encodeUrl(request.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create an activation request", exception);
        }
    }

    public static String importLicense(Context context, String rawToken) {
        initialize(context);
        Authorization authorization;
        try {
            authorization = verify(context, rawToken == null ? "" : rawToken.trim());
        } catch (Exception exception) {
            return exception.getMessage() == null ? "Invalid offline authorization" : exception.getMessage();
        }
        PatchSettings.preferences(context).edit()
            .putString(LICENSE_KEY, rawToken.trim())
            .apply();
        cachedAuthorization = authorization;
        return null;
    }

    public static void clear(Context context) {
        PatchSettings.preferences(context).edit()
            .remove(LICENSE_KEY)
            .remove(LAST_SEEN_TIME_KEY)
            .apply();
        cachedAuthorization = null;
    }

    public static boolean isFeatureEnabled(String feature) {
        Context context = applicationContext;
        if (context == null) {
            return false;
        }
        Authorization authorization = cachedAuthorization;
        if (authorization == null || authorization.expiresAt < trustedNow(context)) {
            return false;
        }
        return authorization.features.contains(feature);
    }

    /** Preserves VSCO's result and adds only a currently valid offline tool grant. */
    public static boolean allowEditing(boolean originalValue) {
        return originalValue || isFeatureEnabled("editing");
    }

    /** Preserves VSCO's result and adds only a currently valid offline preset grant. */
    public static boolean allowFilters(boolean originalValue) {
        return originalValue || isFeatureEnabled("filters");
    }

    public static String status(Context context) {
        initialize(context);
        Authorization authorization = cachedAuthorization;
        if (authorization == null) {
            return "No offline authorization installed";
        }
        long now = trustedNow(context);
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            .format(new Date(authorization.expiresAt * 1000L));
        if (authorization.expiresAt < now) {
            return authorization.tier + " authorization expired " + date;
        }
        return authorization.tier + " • valid through " + date + " • "
            + authorization.features.size() + " feature groups";
    }

    private static Authorization readStoredAuthorization(Context context) {
        String token = PatchSettings.preferences(context).getString(LICENSE_KEY, null);
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            return verify(context, token.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Authorization verify(Context context, String token) throws Exception {
        if (!token.startsWith(LICENSE_PREFIX)) {
            throw new IllegalArgumentException("The authorization format is not recognized");
        }
        String[] parts = token.substring(LICENSE_PREFIX.length()).split("\\.", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("The authorization is incomplete");
        }
        byte[] payloadBytes = decodeUrl(parts[0]);
        byte[] signatureBytes = decodeUrl(parts[1]);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey());
        verifier.update(payloadBytes);
        if (!verifier.verify(signatureBytes)) {
            throw new IllegalArgumentException("The authorization signature is invalid");
        }

        JSONObject payload = new JSONObject(new String(payloadBytes, StandardCharsets.UTF_8));
        if (payload.optInt("schema", 0) != 1
            || !PACKAGE_NAME.equals(payload.optString("packageName"))
            || !ISSUER_KEY_ID.equals(payload.optString("issuerKeyId"))) {
            throw new IllegalArgumentException("The authorization is not compatible with this patch");
        }
        if (!installationId(context).equals(payload.optString("installationId"))) {
            throw new IllegalArgumentException("The authorization belongs to a different installation");
        }
        String tier = payload.optString("tier", "").trim();
        String receiptHash = payload.optString("receiptHash", "");
        long issuedAt = payload.optLong("issuedAt", 0L);
        long expiresAt = payload.optLong("expiresAt", 0L);
        long now = trustedNow(context);
        if (tier.isEmpty() || !receiptHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("The authorization metadata is invalid");
        }
        if (issuedAt <= 0L || issuedAt > now + 86400L || expiresAt <= issuedAt) {
            throw new IllegalArgumentException("The authorization dates are invalid");
        }
        if (expiresAt < now) {
            throw new IllegalArgumentException("The offline authorization has expired");
        }

        JSONArray featureArray = payload.optJSONArray("features");
        Set<String> features = new HashSet<>();
        if (featureArray != null) {
            for (int i = 0; i < featureArray.length(); i++) {
                String feature = featureArray.optString(i, "");
                if ("filters".equals(feature) || "editing".equals(feature)) {
                    features.add(feature);
                }
            }
        }
        if (features.isEmpty()) {
            throw new IllegalArgumentException("The authorization contains no supported features");
        }
        return new Authorization(tier, expiresAt, features);
    }

    private static PublicKey publicKey() throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.decode(PUBLIC_MODULUS, Base64.DEFAULT));
        BigInteger exponent = new BigInteger(1, Base64.decode(PUBLIC_EXPONENT, Base64.DEFAULT));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private static String installationId(Context context) {
        SharedPreferences preferences = PatchSettings.preferences(context);
        String existing = preferences.getString(INSTALLATION_ID_KEY, null);
        if (existing != null && existing.startsWith("id-")) {
            return existing;
        }
        String created;
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (!keyStore.containsAlias(INSTALLATION_KEY_ALIAS)) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    "AndroidKeyStore"
                );
                generator.initialize(
                    new KeyGenParameterSpec.Builder(
                        INSTALLATION_KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY
                    )
                        .setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .build()
                );
                generator.generateKeyPair();
            }
            byte[] publicKey = keyStore.getCertificate(INSTALLATION_KEY_ALIAS)
                .getPublicKey()
                .getEncoded();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(publicKey);
            created = "ks-" + encodeUrl(digest);
        } catch (Exception ignored) {
            // Some vendor keystores are broken. A persisted random ID keeps activation usable.
            created = "id-" + UUID.randomUUID().toString();
        }
        preferences.edit().putString(INSTALLATION_ID_KEY, created).commit();
        return created;
    }

    private static long trustedNow(Context context) {
        long wallClock = System.currentTimeMillis() / 1000L;
        SharedPreferences preferences = PatchSettings.preferences(context);
        long lastSeen = preferences.getLong(LAST_SEEN_TIME_KEY, 0L);
        long trusted = Math.max(wallClock, lastSeen);
        if (trusted > lastSeen) {
            preferences.edit().putLong(LAST_SEEN_TIME_KEY, trusted).apply();
        }
        return trusted;
    }

    private static String encodeUrl(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static byte[] decodeUrl(String value) {
        return Base64.decode(value, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static final class Authorization {
        final String tier;
        final long expiresAt;
        final Set<String> features;

        Authorization(String tier, long expiresAt, Set<String> features) {
            this.tier = tier;
            this.expiresAt = expiresAt;
            this.features = features;
        }
    }
}
