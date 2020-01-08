package org.godotengine.godot;

import android.app.Activity;

import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.YandexMetricaConfig;

public class GodotAppMetrica extends Godot.SingletonBase
{
	private Activity activity = null;

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
		registerClass("AppMetrica", new String[] {"init"});
		activity = p_activity;

	}

    public void init(final String appid) {
        YandexMetricaConfig config = YandexMetricaConfig.newConfigBuilder(appid).build();
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(activity.getApplicationContext(), config);
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(activity.getApplication());
    }

}
