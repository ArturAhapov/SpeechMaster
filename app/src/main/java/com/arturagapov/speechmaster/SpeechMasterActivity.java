package com.arturagapov.speechmaster;

import android.app.Activity;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.vending.billing.IInAppBillingService;
import com.arturagapov.speechmaster.PhraseCreator;
import com.arturagapov.speechmaster.Util.*;
import com.arturagapov.speechmaster.Util.Inventory;
import com.facebook.FacebookSdk;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.*;


public class SpeechMasterActivity extends Activity implements InterstitialAdListener, IabBroadcastReceiver.IabBroadcastListener {
    PhraseCreator phraseCreator = new PhraseCreator();
    //Serializable
    public static String fileNameData = "SpeechMasterData.ser";
    //Firebase EventLog
    private FirebaseAnalytics mFirebaseAnalytics;

    //Billing
    static final String TAG = "Speech Master";
    // Does the user have the premium upgrade?
    boolean mIsPremium = false;
    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    static final String SKU_PREMIUM = "premium";
    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;
    // The helper object
    IabHelper mHelper;
    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;


    private String[] phrase = new String[10000];
    private int quantity = 0;
    private int addFrequency = 6;

    //Реклама
    //Interstitial by Facebook
    private InterstitialAd interstitialAd;
    //Interstitial by Admob
    private com.google.android.gms.ads.InterstitialAd mInterstitial;
    AdRequest mInterstitialAdRequest;
    //isNative
    private boolean isNativeShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_master);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (savedInstanceState != null) {
            quantity = savedInstanceState.getInt("quantity");
        }
        readFromFileData();

        //Billing
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwA7qsZ9chvogS+TYTNCYtHVDn/3qMQbD9Ryb3t0J0C9FCB/HKCE+3OAbTYyKWcGeVBcFNCYf88U87rAi0roSCsGWE8eti+n52rmCP+Nms+AjJ7Oyqj6/Nl2ZSgrYxGMgT/xA3ZY3sLSxhRssinvmTQOvR8THgJRF33YoIbd4ukrddo/wB2iZlyx/vTuYNRi8/fU55fUqdI4HFg+rYzv5AoLMPYohhWl7LIKwzoGZAOOhfVmQO9I0/3guwlav/9ByeMPH8bnXl2M4RPsa8CWYAAkGlxqraKmQqqpJ7Aif69Gq1Z4HZOp2MZW2fiexJQAsVlklDJLS7WjXrfwzG1wQTwIDAQAB";

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }
                if (mHelper == null) return;
                mBroadcastReceiver = new IabBroadcastReceiver(SpeechMasterActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });

        updateUi();
        setAd();
        setText();
        setPhrase();
        nextPhrase();
    }

    private void setNativeAd() {
        if (!Data.userData.isPremium()) {
            //Подключаем рекламу
            NativeExpressAdView adView = (NativeExpressAdView) findViewById(R.id.nativeAdmobAds);
            AdRequest requestNative = new AdRequest.Builder().build();
            adView.loadAd(requestNative);
        }
    }

    private void setAd() {
        if (!Data.userData.isPremium()) {
            //Подключаем InterstitialAd by Admob
            mInterstitial = new com.google.android.gms.ads.InterstitialAd(this);
            mInterstitial.setAdUnitId("ca-app-pub-1399393260153583/4231761154");
            mInterstitial.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    nextPhrase();
                    //super.onAdClosed();
                }
            });
            mInterstitialAdRequest = new AdRequest.Builder().build();
            //Подключаем InterstitialAd by Facebook
            try {
                mInterstitial.loadAd(mInterstitialAdRequest);
                loadInterstitialAd();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateUi() {
        if (mIsPremium) {
            Data.userData.setPremium(true);
            TextView premiumButton = (TextView) findViewById(R.id.premiun);
            premiumButton.setVisibility(View.INVISIBLE);
            saveToFileData();
        }
    }

    private void eventShare() {
        Bundle bundle = new Bundle();
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle);
    }

    private void eventPremium() {
        Bundle bundle = new Bundle();
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE_REFUND, bundle);
    }

    private void setText() {
        TextView phraseText = (TextView) findViewById(R.id.phrase);
        //Меняем шрифт
        Typeface font = Typeface.createFromAsset(getAssets(), "9844.otf");
        phraseText.setTypeface(font);
    }

    private void setPhrase() {

        int bonus1 = (int) (Math.random() * 20);
        int bonus2 = bonus1 + (int) (Math.random() * 25);

        for (int i = 0; i < phrase.length; i++) {
            phraseCreator.createPhrase();
            phrase[i] = phraseCreator.getPhrase();
        }
        phrase[bonus1] = "ДЕНЕГ НЕТ, НО ВЫ ДЕРЖИТЕСЬ ЗДЕСЬ!\nВАМ ВСЕГО ДОБРОГО, ХОРОШЕГО НАСТРОЕНИЯ И ЗДОРОВЬЯ";
        phrase[bonus2] = "ПРОСТО ДЕНЕГ НЕТ СЕЙЧАС.\nНАЙДЕМ ДЕНЬГИ - СДЕЛАЕМ ИНДЕКСАЦИЮ";
    }

    private void nextPhrase() {
        TextView topText = (TextView) findViewById(R.id.textontop);
        int x = quantity + 1;
        topText.setText(x + "/10.000");

        String k = phrase[quantity];
        TextView phraseText = (TextView) findViewById(R.id.phrase);
        phraseText.setText(k);
    }

    private void checkForAdd() {
        if (!Data.userData.isPremium()) {
            if (!isNativeShown) {
                setNativeAd();
                isNativeShown = true;
            }
            if ((quantity + 1) % addFrequency == 0) {
                if (interstitialAd.isAdLoaded()) {
                    interstitialAd.show();
                } else {
                    requestAdmobInterstitial();
                }
            } else {
                nextPhrase();
            }
        } else {
            nextPhrase();
        }
    }

    public void onClickNext(View view) {
        quantity++;
        checkForAdd();
    }

    public void onClickPrevious(View view) {
        if (quantity >= 1) {
            quantity = quantity - 1;
            checkForAdd();
        }
    }

    public void onClickShare(View view) {
        eventShare();
        String text = phrase[quantity];
        String textForShare = (getResources().getString(R.string.begin_with) + "\n" + text + "\n" + Uri.parse("https://play.google.com/store/apps/details?id=com.arturagapov.speechmaster"));
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, textForShare);
        startActivity(shareIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("quantity", quantity);
    }


    //Реклама
    //Подключаем Interstitial by Facebook
    private void loadInterstitialAd() {
        interstitialAd = new InterstitialAd(this, "1629936247306355_1629939493972697");
        interstitialAd.setAdListener(SpeechMasterActivity.this);
        interstitialAd.loadAd();
    }

    @Override
    public void onInterstitialDisplayed(Ad ad) {
    }

    @Override
    public void onInterstitialDismissed(Ad ad) {
        nextPhrase();
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        //Если рекламы для отображения нет, вызывается метод onError, в котором error.code имеет значение 1001. Если вы используете собственную индивидуальную службу отчетов или промежуточную платформу, возможно, вам понадобится проверить значение кода, чтобы обнаружить подобные случаи. В этой ситуации вы можете перейти на другую рекламную сеть, но не отправляйте сразу же после этого повторный запрос на получение рекламы.
        //requestAdmobInterstitial();
    }

    @Override
    public void onAdLoaded(Ad ad) {
    }

    @Override
    public void onAdClicked(Ad ad) {
    }
    //конец реализации Interstitial by Facebook

    private void requestAdmobInterstitial() {
        if (mInterstitial.isLoaded()) {
            mInterstitial.show();
            setAd();
        } else {
            nextPhrase();
        }
    }


    //Billing
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }
            Log.d(TAG, "Query inventory was successful.");
            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
            updateUi();
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }

    public void onClickPremium(View view) {
        Log.d(TAG, "Premium button clicked.");
        eventPremium();
        String payload = "Amsterdam87";

        try {
            mHelper.launchPurchaseFlow(this, SKU_PREMIUM, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        if (payload == "Amsterdam87" || payload.equals("Amsterdam87")) {
            return true;
        } else {
            return false;
        }
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;
            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }
            Log.d(TAG, "Purchase successful.");
            if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                alert("Thank you for upgrading to premium!");
                mIsPremium = true;
                updateUi();
            }
        }
    };
    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("Error: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (interstitialAd != null) {
            interstitialAd.destroy();
        }

        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
    //End Billing

    // Serializes an object and saves it to a file
    public void saveToFileData() {//Context context) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileNameData, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(Data.userData);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Creates an object by reading it from a file
    public Data readFromFileData() {//Context context) {
        try {
            FileInputStream fileInputStream = openFileInput(fileNameData);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Data.userData = (Data) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Data.userData;
    }
}
