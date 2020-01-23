package org.godotengine.godot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import android.app.Activity;
import android.widget.FrameLayout;
import android.view.ViewGroup.LayoutParams;
import android.provider.Settings;
import android.graphics.Color;
import android.util.Log;
import java.util.Locale;
import android.view.Gravity;
import android.view.View;
import android.os.Bundle;

import com.yandex.mobile.ads.AdEventListener;
import com.yandex.mobile.ads.AdRequest;
import com.yandex.mobile.ads.AdRequestError;
import com.yandex.mobile.ads.AdSize;
import com.yandex.mobile.ads.AdView;
import com.yandex.mobile.ads.InterstitialAd;
import com.yandex.mobile.ads.InterstitialEventListener;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;

public class GodotYandexAds extends Godot.SingletonBase
{

	private Activity activity = null; // The main activity of the game
	private int instance_id = 0;

    private HashMap<String, InterstitialAd> interstitials = new HashMap<>();
    private HashMap<String, AdView> banners = new HashMap<>();
    private HashMap<String, RewardedAd> rewardeds = new HashMap<>();

	private boolean ProductionMode = true; // Store if is real or not
	private boolean isForChildDirectedTreatment = false; // Store if is children directed treatment desired
	private String maxAdContentRating = ""; // Store maxAdContentRating ("G", "PG", "T" or "MA")
	private Bundle extras = null;

	private FrameLayout layout = null; // Store the layout
	private FrameLayout.LayoutParams adParams = null; // Store the layout params

	/* Init
	 * ********************************************************************** */

	/**
	 * Prepare for work with YandexAds
	 * @param boolean ProductionMode Tell if the enviroment is for real or test
	 * @param int gdscript instance id
	 */
	public void init(boolean ProductionMode, int instance_id) {
		this.initWithContentRating(ProductionMode, instance_id, false, "");
	}

	/**
	 * Init with content rating additional options 
	 * @param boolean ProductionMode Tell if the enviroment is for real or test
	 * @param int gdscript instance id
	 * @param boolean isForChildDirectedTreatment
	 * @param String maxAdContentRating must be "G", "PG", "T" or "MA"
	 */
	public void initWithContentRating(boolean ProductionMode, int instance_id, boolean isForChildDirectedTreatment, String maxAdContentRating)
	{
		this.ProductionMode = ProductionMode;
		this.instance_id = instance_id;
		this.isForChildDirectedTreatment = isForChildDirectedTreatment;
		this.maxAdContentRating = maxAdContentRating;
		if (maxAdContentRating != null && maxAdContentRating != "")
		{
			extras = new Bundle();
			extras.putString("max_ad_content_rating", maxAdContentRating);
		}
		Log.d("godot", "YandexAds: init with content rating options");
	}


	/**
	 * Returns AdRequest object constructed considering the parameters set in constructor of this class.
	 * @return AdRequest object
	 */
	private AdRequest getAdRequest()
	{
		AdRequest.Builder adBuilder = new AdRequest.Builder();
		AdRequest adRequest;
        /*
		if (!this.isForChildDirectedTreatment && extras != null)
		{
			adBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
		}
		if (this.isForChildDirectedTreatment)
		{
			adBuilder.tagForChildDirectedTreatment(true);
		}
		if (!ProductionMode) {
			adBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
			adBuilder.addTestDevice(getAdmobDeviceId());
		}
        */
		adRequest = adBuilder.build();
		return adRequest;
	}

	/* Rewarded Video
	 * ********************************************************************** */
	private RewardedAd initRewardedVideo(final String id, final int callback_id)
	{
        Log.w("godot", "Prepare rewarded video: "+id+" callback: "+Integer.toString(callback_id));
        RewardedAd rewarded = new RewardedAd(activity);
        rewarded.setBlockId(id);
        rewarded.setRewardedAdEventListener(new RewardedAdEventListener.SimpleRewardedAdEventListener()
            {
                @Override
                public void onAdLeftApplication() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdLeftApplication");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_left_application", new Object[] { id });
                }

                @Override
                public void onAdClosed() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdClosed");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_closed", new Object[] { id });
                }

                @Override
                public void onAdFailedToLoad(final AdRequestError error) {
                    Log.w("godot", "YandexAds: onRewardedVideoAdFailedToLoad. error: " + error.toString());
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { id, error.toString() });
                }

                @Override
                public void onAdLoaded() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdLoaded");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_loaded", new Object[] { id });
                }

                @Override
                public void onAdOpened() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdOpened");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_opened", new Object[] { id });
                }

                @Override
                public void onRewarded(final Reward reward) {
                    Log.w("godot", "YandexAds: " + String.format(" onRewarded! currency: %s amount: %d", reward.getType(), reward.getAmount()));
                    GodotLib.calldeferred(callback_id, "_on_rewarded", new Object[] { id, reward.getType(), reward.getAmount() });
                }

                /*
                  @Override
                  public void onRewardedVideoStarted() {
                  Log.w("godot", "YandexAds: onRewardedVideoStarted");
                  GodotLib.calldeferred(instance_id, "_on_rewarded_video_started", new Object[] { id });
                  }

                  @Override
                  public void onRewardedVideoCompleted() {
                  Log.w("godot", "YandexAds: onRewardedVideoCompleted");
                  GodotLib.calldeferred(instance_id, "_on_rewarded_video_completed", new Object[] { id });
                  }
                */
            });
        rewarded.loadAd(getAdRequest());
        return rewarded;
	}

	/**
	 * Load a Rewarded Video
	 * @param String id AdMod Rewarded video ID
	 */
	public void loadRewardedVideo(final String id, final int callback_id) {
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                try {
                    RewardedAd r = null;
                    //if(rewardeds.containsKey(id)) {
                    //    r = rewardeds.get(id);
                    //    if (!r.isLoaded()) {
                    //        r.loadAd(getAdRequest());
                    //    }
                    //} else {
                        if(callback_id <= 0)
                            r = initRewardedVideo(id, instance_id);
                        else
                            r = initRewardedVideo(id, callback_id);
                        rewardeds.put(id, r);
                    //}
                } catch (Exception e) {
                    Log.e("godot", e.toString());
                    e.printStackTrace();
                }
			}
		});
	}

	/**
	 * Show a Rewarded Video
	 */
	public void showRewardedVideo(final String id) {
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                if(rewardeds.containsKey(id)) {
                    RewardedAd r = rewardeds.get(id);
                    if (r.isLoaded()) {
                        r.show();
                    } else {
                        Log.w("godot", "YandexAds: showRewardedVideo - rewarded not loaded");
                    }
                }
			}
		});
	}

    /*
    public boolean isRewardedVideoLoaded(final String id)
    {
        return rewardedVideoAd != null && rewardedVideoAd.isLoaded();
    }
    */

	/* Banner
	 * ********************************************************************** */

    private AdView initBanner(final String id, final boolean isOnTop, final int callback_id)
    {
        layout = (FrameLayout)activity.getWindow().getDecorView().getRootView();
        adParams = new FrameLayout.LayoutParams(
                                                FrameLayout.LayoutParams.MATCH_PARENT,
                                                FrameLayout.LayoutParams.WRAP_CONTENT
                                                );
        if(isOnTop) adParams.gravity = Gravity.TOP;
        else adParams.gravity = Gravity.BOTTOM;
				
        AdView banner = new AdView(activity);
        banner.setBlockId(id);

        banner.setBackgroundColor(/* Color.WHITE */Color.TRANSPARENT);

        banner.setAdSize(new AdSize(AdSize.FULL_WIDTH, 50));
        banner.setAdEventListener(new AdEventListener.SimpleAdEventListener() {
                @Override
                public void onAdLoaded() {
                    Log.w("godot", "YandexAds: onAdLoaded");
                    GodotLib.calldeferred(callback_id, "_on_banner_loaded", new Object[]{ id });
                }

                @Override
                public void onAdFailedToLoad(final AdRequestError error)
                {
                    String	str;
                    Log.w("godot", "YandexAds: onAdFailedToLoad -> " + error.toString());
						
                    GodotLib.calldeferred(callback_id, "_on_banner_failed_to_load", new Object[]{ id, error.toString() });
                }
            });
        layout.addView(banner, adParams);

        // Request
        banner.loadAd(getAdRequest());
        return banner;
    }

	/**
	 * Load a banner
	 * @param String id AdMod Banner ID
	 * @param boolean isOnTop To made the banner top or bottom
	 */
	public void loadBanner(final String id, final boolean isOnTop, final int callback_id)
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                if(!banners.containsKey(id)) {
                    AdView b = initBanner(id, isOnTop, callback_id);
                    banners.put(id, b);
				} else {
                    Log.w("godot", "YandexAds: Banner already created: "+id);
                }
			}
		});
	}

	/**
	 * Show the banner
	 */
	public void showBanner(final String id)
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                if(banners.containsKey(id)) {
                    AdView b = banners.get(id);
                    //if (b.getVisibility() == View.VISIBLE) return;
                    b.setVisibility(View.VISIBLE);
                    b.resume();
                    for (String key : banners.keySet()) {
                        if(!key.equals(id)) {
                            AdView b2 = banners.get(key);
                            //if (b2.getVisibility() != View.GONE) {
                                b2.setVisibility(View.GONE);
                                b2.pause();
                            //}
                        }
                    }
                    Log.d("godot", "YandexAds: Show Banner");
                } else {
                    Log.w("godot", "YandexAds: Banner not found: "+id);
                }
			}
		});
	}

    public void removeBanner(final String id)
    {
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
				if (layout == null || adParams == null)	{
					return;
				}

                if(banners.containsKey(id)) {
                    AdView b = banners.get(id);
                    banners.remove(id);
                    layout.removeView(b); // Remove the banner
                    Log.d("godot", "YandexAds: Remove Banner");
                } else {
                    Log.w("godot", "YandexAds: Banner not found: "+id);
                }
			}
		});
    }

	/**
	 * Resize the banner
	 *
	 */
    /*
	public void resize()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
				if (layout == null || adParams == null)
				{
					return;
				}

                for (String key : banners.keySet()) {
                    AdView b = banners.get(key);
                    layout.removeView(b); // Remove the old view

                    // Extract params
                    boolean isOnTop = adParams.gravity == Gravity.TOP;
                    b = initBanner(key, isOnTop);
                    banners.put(key, b);
                }

				Log.d("godot", "YandexAds: Banner Resized");
			}
		});
	}
    */


	/**
	 * Hide the banner
	 */
	public void hideBanner(final String id)
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                if(banners.containsKey(id)) {
                    AdView b = banners.get(id);
                    //if (b.getVisibility() == View.GONE) return;
                    b.setVisibility(View.GONE);
                    b.pause();
                    Log.d("godot", "YandexAds: Hide Banner");
                } else {
                    Log.w("godot", "YandexAds: Banner not found: "+id);
                }
			}
		});
	}

	/**
	 * Get the banner width
	 * @return int Banner width
	 */
	public int getBannerWidth(final String id)
	{
        if(banners.containsKey(id)) {
            AdView b = banners.get(id);
            return b.getAdSize().getWidthInPixels(activity);
        } else {
            return 0; //320;
        }
	}

	/**
	 * Get the banner height
	 * @return int Banner height
	 */
	public int getBannerHeight(final String id)
	{
        if(banners.containsKey(id)) {
            AdView b = banners.get(id);
            return b.getAdSize().getHeightInPixels(activity);
        } else {
            return 0; //50;
        }
	}

	/* Interstitial
	 * ********************************************************************** */

    private InterstitialAd initInterstitial(final String id, final int callback_id)
    {
        InterstitialAd interstitial = new InterstitialAd(activity);
        interstitial.setBlockId(id);
        interstitial.setInterstitialEventListener(new InterstitialEventListener.SimpleInterstitialEventListener() {
                @Override
                public void onAdClosed() {
                    Log.w("godot", "YandexAds: onAdClosed");
                }

                @Override
                public void onAdLeftApplication() {
                    Log.w("godot", "YandexAds: onAdLeftApplication");
                }

                @Override
                public void onAdOpened() {
                    Log.w("godot", "YandexAds: onAdOpened()");
                }

                @Override
                public void onInterstitialDismissed() {
                    Log.w("godot", "YandexAds: onInterstitialDismissed()");
                    GodotLib.calldeferred(callback_id, "_on_interstitial_close", new Object[] { id });
                }

                @Override
                public void onInterstitialFailedToLoad(final AdRequestError error) {
                    Log.w("godot", "YandexAds: onInterstitialFailedToLoad - error: " + error.toString());
                    GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, error.toString() });
                }

                @Override
                public void onInterstitialLoaded() {
                    Log.w("godot", "YandexAds: onInterstitialLoaded");
                    GodotLib.calldeferred(callback_id, "_on_interstitial_loaded", new Object[] { id });
                }

                @Override
                public void onInterstitialShown() {
                    Log.w("godot", "YandexAds: onInterstitialShown");
                }
            });
        interstitial.loadAd(getAdRequest());

        return interstitial;
    }

	/**
	 * Load a interstitial
	 * @param String id AdMod Interstitial ID
	 */
	public void loadInterstitial(final String id, final int callback_id)
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                //if(!interstitials.containsKey(id)) {
                InterstitialAd interstitial = initInterstitial(id, callback_id);
                interstitials.put(id, interstitial);
                //}
			}
		});
	}

	/**
	 * Show the interstitial
	 */
	public void showInterstitial(final String id)
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                if(interstitials.containsKey(id)) {
                    InterstitialAd interstitial = interstitials.get(id);
                    if (interstitial.isLoaded()) {
                        interstitial.show();
                    } else {
                        Log.w("godot", "YandexAds: showInterstitial - interstitial not loaded");
                    }
                }
			}
		});
	}

    /*
    public boolean isInterstitialLoaded(final String id)
    {
        return interstitialAd != null && interstitialAd.isLoaded();
    }
    */

	/* Utils
	 * ********************************************************************** */

	/**
	 * Generate MD5 for the deviceID
	 * @param String s The string to generate de MD5
	 * @return String The MD5 generated
	 */
	private String md5(final String s)
	{
		try {
			// Create MD5 Hash
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i=0; i<messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2) h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();
		} catch(NoSuchAlgorithmException e) {
			//Logger.logStackTrace(TAG,e);
		}
		return "";
	}

	/**
	 * Get the Device ID for YandexAds
	 * @return String Device ID
	 */
	private String getAdmobDeviceId()
	{
		String android_id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
		String deviceId = md5(android_id).toUpperCase(Locale.US);
		return deviceId;
	}

	/* Definitions
	 * ********************************************************************** */

	/**
	 * Initilization Singleton
	 * @param Activity The main activity
	 */
 	static public Godot.SingletonBase initialize(Activity activity)
 	{
 		return new GodotYandexAds(activity);
 	}

	/**
	 * Constructor
	 * @param Activity Main activity
	 */
	public GodotYandexAds(Activity p_activity) {
		registerClass("YandexAds", new String[] {
			"init",
			"initWithContentRating",
			// banner
			"loadBanner", "showBanner", "hideBanner", "removeBanner", "getBannerWidth", "getBannerHeight", //"resize",
			// Interstitial
			"loadInterstitial", "showInterstitial", //"isInterstitialLoaded",
			// Rewarded video
			"loadRewardedVideo", "showRewardedVideo" //, "isRewardedVideoLoaded"
		});
		activity = p_activity;
	}
}
