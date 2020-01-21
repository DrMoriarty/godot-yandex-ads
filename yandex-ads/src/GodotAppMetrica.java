package org.godotengine.godot;

import android.app.Activity;

import com.yandex.metrica.Revenue;
import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.YandexMetricaConfig;
import com.yandex.metrica.profile.Attribute;
import com.yandex.metrica.profile.UserProfile;
import java.util.Currency;

public class GodotAppMetrica extends Godot.SingletonBase
{
	private Activity activity = null;
    static private boolean inited = false;

	/**
	 * Initilization Singleton
	 * @param Activity The main activity
	 */
 	static public Godot.SingletonBase initialize(Activity activity)
 	{
 		return new GodotAppMetrica(activity);
 	}

	/**
	 * Constructor
	 * @param Activity Main activity
	 */
	public GodotAppMetrica(Activity p_activity) {
		registerClass("AppMetrica", new String[] {
                "init", "logRevenue", "setUserId", "setUserProperties", "logEvent"
            });
		activity = p_activity;
	}

    public void init(final String appid) {
        YandexMetricaConfig config = YandexMetricaConfig.newConfigBuilder(appid).build();
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(activity.getApplicationContext(), config);
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(activity.getApplication());
        YandexMetrica.reportAppOpen(activity);
        inited = true;
    }

    public void logRevenue(float price, final String currency, final String productId, int quantity) {
        // Creating the Revenue instance.
        long micros = (long)(1000000 * price);
        Revenue revenue = Revenue.newBuilderWithMicros(micros, Currency.getInstance(currency))
            .withProductID(productId)
            .withQuantity(quantity)
            // Passing the OrderID parameter in the .withPayload(String payload) method to group purchases.
            //.withPayload("{\"OrderID\":\"Identifier\", \"source\":\"Google Play\"}")
            .build();
        // Sending the Revenue instance using reporter.
        YandexMetrica.reportRevenue(revenue);
    }

    public void setUserId(final String uid) {
        YandexMetrica.setUserProfileID(uid);
    }

    public void setUserProperties(final Dictionary properties) {
        UserProfile.Builder builder = UserProfile.newBuilder();
        for(String key: properties.get_keys()) {
            if(properties.get(key) != null) {
                String val = properties.get(key).toString();
                builder.apply(Attribute.customString(key).withValue(val));
            }
        }
        YandexMetrica.reportUserProfile(builder.build());
    }

    public void logEvent(final String event, final Dictionary params) {
        YandexMetrica.reportEvent(event, params);
    }

    @Override protected void onMainResume() {
        if(inited)
            YandexMetrica.reportAppOpen(activity);
    } 

}
