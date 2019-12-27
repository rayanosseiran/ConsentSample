package consentsample.main;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import consentsample.R;

import java.net.MalformedURLException;
import java.net.URL;



public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private AdView mAdView;
    private ConsentForm form;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAdView = findViewById(R.id.mainFooterAd);	
        getConsentStatus();

    }


    /**
     * Gets the users consent status and either requests consent or displays the appropriate ad mode
     */
    private void getConsentStatus() {
        ConsentInformation consentInformation = ConsentInformation.getInstance(this);
        // Uncomment if you are testing outside the EU
        //consentInformation.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
        //consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        String[] publisherIds = {"PUBLISHER_ID_HERE"};
        consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                // User's consent status successfully updated.
                Log.d(TAG, "Consent status is " + consentStatus.toString());
                if (ConsentInformation.getInstance(getBaseContext()).isRequestLocationInEeaOrUnknown()) {
                    switch (consentStatus) {
                        case UNKNOWN:
                            displayConsentForm();
                            break;
                        case PERSONALIZED:
                            initializeAds(true);
                            break;
                        case NON_PERSONALIZED:
                            initializeAds(false);
                            break;
                    }
                } else {
                    Log.d(TAG, "Not in EU, displaying personalized ads");
                    // Note: You could technically put the if-else inside the UNKNOWN switch, however, if a user leaves the EU, they won't be put back into personalized advertising 
                    initializeAds(true);
                }
            }
            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                // User's consent status failed to update.
            }
        });
    }

    /**
     * Displays the consent form for advertisements through the Google Consent SDK
     */
    private void displayConsentForm() {
        URL privacyUrl = null;
        try {
            privacyUrl = new URL(getString(R.string.privacy_policy_link));
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error processing privacy policy url", e);
        }
        form = new ConsentForm.Builder(this, privacyUrl)
                .withListener(new ConsentFormListener() {
                    @Override
                    public void onConsentFormLoaded() {
                        // Consent form loaded successfully.
                        form.show();
                    }
                    @Override
                    public void onConsentFormOpened() {
                        // Consent form was displayed.
                    }
                    @Override
                    public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                        // Consent form was closed.
                        if (consentStatus.equals(ConsentStatus.PERSONALIZED))
                            initializeAds(true);
                        else
                            initializeAds(false);
                    }
                    @Override
                    public void onConsentFormError(String errorDescription) {
                        // Consent form error. This usually happens if the user is not in the EU.
                        Log.e(TAG, "Error loading consent form: " + errorDescription);
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build();

        form.load();
    }

    /**
     * Initializes the applications main banner ad
     *
     * @param isPersonalized true if the ad should be personalized
     */
    private void initializeAds(boolean isPersonalized) {
            MobileAds.initialize(this, "App ID");
            mAdView.setAdUnitId("Ad Unit"); 
            mAdView.setAdSize(AdSize.BANNER);

            // this is the part you need to add/modify on your code
            AdRequest adRequest;
            if (isPersonalized) {
                adRequest = new AdRequest.Builder().build();
            } else {
                Bundle extras = new Bundle();
                extras.putString("npa", "1");
                adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                        .build();
            }
            
            mAdView.loadAd(adRequest);
    }


}