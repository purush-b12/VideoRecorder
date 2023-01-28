package com.android.hopein.dancevideorecorder.ui.home

import android.content.Context
import com.google.android.gms.ads.*

object AdViewHandler {

    fun initiateAd(adView: AdView, context: Context){
        MobileAds.initialize(context) {}
        //binding.adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
        //ads:adUnitId="ca-app-pub-5655866277681532/2924355976"
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object: AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }
    }

}