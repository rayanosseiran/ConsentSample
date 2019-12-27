package consentsample.main

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import consentsample.R
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private lateinit var mAdView: AdView
    private lateinit var form: ConsentForm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mAdView = findViewById(R.id.mainFooterAd)

        getConsentStatus()
    }

    /**
     * Gets the users consent status and either requests consent or displays the appropriate ad mode
     */
    private fun getConsentStatus() {
        val consentInformation = ConsentInformation.getInstance(this)
        // Uncomment if you are testing outside the EU
        // consentInformation.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
        // consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        val publisherIds = arrayOf("PUBLISHER_ID_HERE")
        consentInformation.requestConsentInfoUpdate(publisherIds, object : ConsentInfoUpdateListener {
            override fun onConsentInfoUpdated(consentStatus: ConsentStatus) { 
                // User's consent status successfully updated.
                Log.d(TAG, "Consent status is $consentStatus")
                if (ConsentInformation.getInstance(baseContext).isRequestLocationInEeaOrUnknown) {
                    when (consentStatus) {
                        ConsentStatus.UNKNOWN -> displayConsentForm()
                        ConsentStatus.PERSONALIZED -> initializeAds(true)
                        ConsentStatus.NON_PERSONALIZED -> initializeAds(false)
                    }
                } else {
                    Log.d(TAG, "Not in EU, displaying personalized ads")
                    // Note: You could technically put the if-else inside the UNKNOWN switch, however, if a user leaves the EU, they won't be put back into personalized advertising 
                    initializeAds(true)
                }
            }
            override fun onFailedToUpdateConsentInfo(errorDescription: String) {
                // User's consent status failed to update.
            }
        })
    }

    /**
     * Displays the consent form for advertisements through the Google Consent SDK
     */
    private fun displayConsentForm() {
        var privacyUrl: URL? = null
        try {
            privacyUrl = URL(getString(R.string.privacy_policy_link))
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error processing privacy policy url", e)
        }
        form = ConsentForm.Builder(this, privacyUrl)
                .withListener(object : ConsentFormListener() {
                    override fun onConsentFormLoaded() {
                        // Consent form loaded successfully.
                        form.show()
                    }

                    override fun onConsentFormOpened() {
                        // Consent form was displayed.
                    }

                    override fun onConsentFormClosed(consentStatus: ConsentStatus, userPrefersAdFree: Boolean) { 
                        // Consent form was closed.
                        if (consentStatus == ConsentStatus.PERSONALIZED)
                            initializeAds(true)
                        else
                            initializeAds(false)
                    }

                    override fun onConsentFormError(errorDescription: String) {
                        // Consent form error. This usually happens if the user is not in the EU.
                        Log.e(TAG, "Error loading consent form: $errorDescription")
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build()

        form.load()
    }

    /**
     * Initializes the applications main banner ad
     *
     * @param isPersonalized true if the ad should be personalized
     */
    private fun initializeAds(isPersonalized: Boolean) {
            MobileAds.initialize(this, "App ID")
            mAdView.adUnitId = "Ad Unit" 
            mAdView.adSize = AdSize.BANNER

            // this is the part you need to add/modify on your code
            val adRequest = if (isPersonalized) {
                AdRequest.Builder().build()
            } else {
                val extras = Bundle()
                extras.putString("npa", "1")
                AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                        .build()
            }

            mAdView.loadAd(adRequest)
    }
}