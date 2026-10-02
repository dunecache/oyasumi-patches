package app.oyasumi.extension;

import android.content.Context;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.util.UUID;

/**
 * Supplies a stand-in advertising identifier.
 *
 * <p>The value is random rather than empty on purpose. The one app-owned reader of this
 * identifier that has been inspected explicitly handles an empty string by omitting the
 * parameter, so an empty value would also work and would hide the parameter entirely. Seven
 * further readers share the same cache and have not been inspected, and an empty identifier is
 * the shape most likely to surprise one of them, since an empty string is a valid
 * {@code getId()} result that no reader has a reason to expect. A random value in the usual
 * identifier shape is inert for that reason: it is not derived from the device or the account, so
 * it cannot be used to recognise the install, and it is constant for the process so it cannot be
 * used to correlate two reads within one session either.
 */
@SuppressWarnings("unused")
public class NeutralizeAdvertisingIdPatch {

    private static final String NEUTRAL_ID = UUID.randomUUID().toString();

    public static AdvertisingIdClient.Info getInfo(Context context) {
        return new AdvertisingIdClient.Info(NEUTRAL_ID, true);
    }
}
