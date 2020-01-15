package org.godotengine.godot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

	private InterstitialAd interstitialAd = null; // Interstitial object
	private AdView adView = null; // Banner view

	private boolean ProductionMode = true; // Store if is real or not
	private boolean isForChildDirectedTreatment = false; // Store if is children directed treatment desired
	private String maxAdContentRating = ""; // Store maxAdContentRating ("G", "PG", "T" or "MA")
	private Bundle extras = null;


	private FrameLayout layout = null; // Store the layout
	private FrameLayout.LayoutParams adParams = null; // Store the layout params

	private RewardedAd rewardedVideoAd = null; // Rewarded Video object

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
	private void initRewardedVideo(final String id)
	{
        rewardedVideoAd = new RewardedAd(activity);
        rewardedVideoAd.setBlockId(id);
        rewardedVideoAd.setRewardedAdEventListener(new RewardedAdEventListener.SimpleRewardedAdEventListener()
            {
                @Override
                public void onAdLeftApplication() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdLeftApplication");
                    GodotLib.calldeferred(instance_id, "_on_rewarded_video_ad_left_application", new Object[] { });
                }

                @Override
                public void onAdClosed() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdClosed");
                    GodotLib.calldeferred(instance_id, "_on_rewarded_video_ad_closed", new Object[] { });
                }

                @Override
                public void onAdFailedToLoad(final AdRequestError error) {
                    Log.w("godot", "YandexAds: onRewardedVideoAdFailedToLoad. error: " + error.toString());
                    GodotLib.calldeferred(instance_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { error.toString() });
                }

                @Override
                public void onAdLoaded() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdLoaded");
                    GodotLib.calldeferred(instance_id, "_on_rewarded_video_ad_loaded", new Object[] { });
                }

                @Override
                public void onAdOpened() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdOpened");
                    GodotLib.calldeferred(instance_id, "_on_rewarded_video_ad_opened", new Object[] { });
                }

                @Override
                public void onRewarded(final Reward reward) {
                    Log.w("godot", "YandexAds: " + String.format(" onRewarded! currency: %s amount: %d", reward.getType(), reward.getAmount()));
                    GodotLib.calldeferred(instance_id, "_on_rewarded", new Object[] { reward.getType(), reward.getAmount() });
                }

                /*
                  @Override
                  public void onRewardedVideoStarted() {
                  Log.w("godot", "YandexAds: onRewardedVideoStarted");
                  GodotLib.calldeferred(instance_id, "_on_rewarded_video_started", new Object[] { });
                  }

                  @Override
                  public void onRewardedVideoCompleted() {
                  Log.w("godot", "YandexAds: onRewardedVideoCompleted");
                  GodotLib.calldeferred(instance_id, "_on_rewarded_video_completed", new Object[] { });
                  }
                */
            });
	}

	/**
	 * Load a Rewarded Video
	 * @param String id AdMod Rewarded video ID
	 */
	public void loadRewardedVideo(final String id) {
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                try {
                    if (rewardedVideoAd == null) {
                        initRewardedVideo(id);
                    }

                    if (!rewardedVideoAd.isLoaded()) {
                        rewardedVideoAd.loadAd(getAdRequest());
                    }
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
	public void showRewardedVideo() {
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
				if (rewardedVideoAd.isLoaded()) {
					rewardedVideoAd.show();
				}
			}
		});
	}


	/* Banner
	 * ********************************************************************** */

	/**
	 * Load a banner
	 * @param String id AdMod Banner ID
	 * @param boolean isOnTop To made the banner top or bottom
	 */
	public void loadBanner(final String id, final boolean isOnTop)
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
                layout = (FrameLayout)activity.getWindow().getDecorView().getRootView();
				adParams = new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT
				);
				if(isOnTop) adParams.gravity = Gravity.TOP;
				else adParams.gravity = Gravity.BOTTOM;
				
				if (adView != null)
				{
					layout.removeView(adView); // Remove the old view
				}

				adView = new AdView(activity);
				adView.setBlockId(id);

				adView.setBackgroundColor(Color.TRANSPARENT);

				adView.setAdSize(new AdSize(AdSize.FULL_WIDTH, 50));
				adView.setAdEventListener(new AdEventListener.SimpleAdEventListener()
				{
					@Override
					public void onAdLoaded() {
						Log.w("godot", "YandexAds: onAdLoaded");
						GodotLib.calldeferred(instance_id, "_on_ad_loaded", new Object[]{ });
					}

					@Override
					public void onAdFailedToLoad(final AdRequestError error)
					{
						String	str;
						String callbackFunctionName = "_on_banner_failed_to_load";
						Log.w("godot", "YandexAds: onAdFailedToLoad -> " + error.toString());
						Log.w("godot", "YandexAds: callbackfunction -> " + callbackFunctionName);
						
						GodotLib.calldeferred(instance_id, callbackFunctionName, new Object[]{ });
					}
				});
				layout.addView(adView, adParams);

				// Request
				adView.loadAd(getAdRequest());
			}
		});
	}

	/**
	 * Show the banner
	 */
	public void showBanner()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
				if (adView.getVisibility() == View.VISIBLE) return;
				adView.setVisibility(View.VISIBLE);
				adView.resume();
				Log.d("godot", "YandexAds: Show Banner");
			}
		});
	}

	/**
	 * Resize the banner
	 *
	 */
	public void resize()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
				if (layout == null || adView == null || adParams == null)
				{
					return;
				}

				layout.removeView(adView); // Remove the old view

				// Extract params

				int gravity = adParams.gravity;
				FrameLayout	layout = (FrameLayout)activity.getWindow().getDecorView().getRootView();
				adParams = new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT
 				);
				adParams.gravity = gravity;
				AdEventListener adListener = adView.getAdEventListener();
				String id = adView.getBlockId();

				// Create new view & set old params
				adView = new AdView(activity);
				adView.setBlockId(id);
				adView.setBackgroundColor(Color.TRANSPARENT);
				adView.setAdSize(new AdSize(AdSize.FULL_WIDTH, 50));
				adView.setAdEventListener(adListener);

				// Add to layout and load ad
				layout.addView(adView, adParams);

				// Request
				adView.loadAd(getAdRequest());

				Log.d("godot", "YandexAds: Banner Resized");
			}
		});
	}




	/**
	 * Hide the banner
	 */
	public void hideBanner()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
				if (adView.getVisibility() == View.GONE) return;
				adView.setVisibility(View.GONE);
				adView.pause();
				Log.d("godot", "YandexAds: Hide Banner");
			}
		});
	}

	/**
	 * Get the banner width
	 * @return int Banner width
	 */
	public int getBannerWidth()
	{
        if(adView != null)
            return adView.getAdSize().getWidthInPixels(activity);
        else
            return 0; //320;
	}

	/**
	 * Get the banner height
	 * @return int Banner height
	 */
	public int getBannerHeight()
	{
        if(adView != null)
            return adView.getAdSize().getHeightInPixels(activity);
        else
            return 0; //50;
	}

	/* Interstitial
	 * ********************************************************************** */

	/**
	 * Load a interstitial
	 * @param String id AdMod Interstitial ID
	 */
	public void loadInterstitial(final String id)
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
				interstitialAd = new InterstitialAd(activity);
				interstitialAd.setBlockId(id);
		        interstitialAd.setInterstitialEventListener(new InterstitialEventListener.SimpleInterstitialEventListener()
				{
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
						GodotLib.calldeferred(instance_id, "_on_interstitial_close", new Object[] { });
                    }

					@Override
					public void onInterstitialFailedToLoad(final AdRequestError error) {
						Log.w("godot", "YandexAds: onInterstitialFailedToLoad(int errorCode) - error: " + error.toString());
						Log.w("godot", "YandexAds: _on_interstitial_not_loaded");
						GodotLib.calldeferred(instance_id, "_on_interstitial_not_loaded", new Object[] { });
					}

					@Override
					public void onInterstitialLoaded() {
						Log.w("godot", "YandexAds: onInterstitialLoaded");
						GodotLib.calldeferred(instance_id, "_on_interstitial_loaded", new Object[] { });
					}

                    @Override
                    public void onInterstitialShown() {
						Log.w("godot", "YandexAds: onInterstitialShown");
                    }
				});



				interstitialAd.loadAd(getAdRequest());
			}
		});
	}

	/**
	 * Show the interstitial
	 */
	public void showInterstitial()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override public void run()
			{
				if (interstitialAd.isLoaded()) {
					interstitialAd.show();
				} else {
					Log.w("w", "YandexAds: showInterstitial - interstitial not loaded");
				}
			}
		});
	}

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
			"loadBanner", "showBanner", "hideBanner", "getBannerWidth", "getBannerHeight", "resize",
			// Interstitial
			"loadInterstitial", "showInterstitial",
			// Rewarded video
			"loadRewardedVideo", "showRewardedVideo"
		});
		activity = p_activity;
	}
}
