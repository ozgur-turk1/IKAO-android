package fr.ikao.IkaoParis;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WebView myWebView;
    private SharedPreferences sharedPreferences;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkUpdate();

        myWebView = findViewById(R.id.webview);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.getSettings().setJavaScriptEnabled(true);     // Activation JavaScript
        myWebView.getSettings().setDomStorageEnabled(true);     // Sessions
        myWebView.getSettings().setAllowFileAccess(false);      // Désactive l'accès aux fichiers locaux

        // Activer les cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(myWebView, true);

        myWebView.loadUrl(getString(R.string.url_main));  // Charger le site web dans la WebView

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Vérifier si la question a déjà été posée
        boolean askedForNotificationPermission = sharedPreferences.getBoolean("askedForNotificationPermission", false);

        if (!askedForNotificationPermission) {
            askNotificationPermission();
        }

        // Gérer les redirections
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            // Cette version est utilisée pour les appareils plus anciens
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // Utiliser OnBackPressedDispatcher pour gérer le bouton retour
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Si la WebView peut revenir en arrière, on revient à la page précédente
                if (myWebView.canGoBack()) {
                    myWebView.goBack();
                } else {
                    // Sinon, on quitte l'application
                    setEnabled(false); // Désactiver le callback pour permettre de quitter l'appli
                }
            }
        });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // Get new FCM registration token
                    String token = task.getResult();

                    // Log and toast
                    String msg = getString(R.string.msg_token_fmt, token);
                    Log.d(TAG, msg);
                });
    }

    private void askNotificationPermission() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notif_active)
                .setMessage(R.string.notif_question)
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Enregistrer la réponse et activer les notifications
                    sharedPreferences.edit().putBoolean("askedForNotificationPermission", true).apply();
                    enableNotifications();
                })
                .setNegativeButton("Non", (dialog, which) -> {
                    // Enregistrer la réponse et désactiver les notifications
                    sharedPreferences.edit().putBoolean("askedForNotificationPermission", true).apply();
                    disableNotifications();
                })
                .show();
    }

    private void enableNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("promotions")
                .addOnCompleteListener(task -> {
                    String msg = getString(R.string.notif_on);
                    if (!task.isSuccessful()) {
                        msg = getString(R.string.notif_fail);
                    }
                    Log.d("MyApp", msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void disableNotifications() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("promotions")
                .addOnCompleteListener(task -> {
                    String msg = getString(R.string.notif_off);
                    if (!task.isSuccessful()) {
                        msg = getString(R.string.notif_off_fail);
                    }
                    Log.d("MyApp", msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUpdate() {
        Log.d("UpdateCheck", "Starting check for update...");

        RemoteConfigManager.fetchAndActivate(isSuccess -> {
            if (isSuccess) {
                Log.d("UpdateCheck", "Fetch and activate SUCCESSFUL.");

                long minVersionCode = RemoteConfigManager.getMinVersionCode();
                Log.d("UpdateCheck", "Min version from Firebase: " + minVersionCode);

                boolean isForced = RemoteConfigManager.isUpdateForced();

                Log.d("UpdateCheck", "isForced: " + isForced);

                long currentVersionCode;

                try {
                    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        currentVersionCode = packageInfo.getLongVersionCode();
                    } else {
                        currentVersionCode = packageInfo.versionCode;
                    }
                    Log.d("UpdateCheck", "Current app version: " + currentVersionCode);

                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("MainActivity", "Package name not found", e);
                    currentVersionCode = -1;
                }

                if (currentVersionCode != -1 && currentVersionCode < minVersionCode) {
                    Log.d("UpdateCheck", "UPDATE REQUIRED! Showing dialog."); // LOG 5

                    runOnUiThread(() -> showUpdateDialog(isForced));
                } else Log.d("UpdateCheck", "No update required. Current: " + currentVersionCode + ", Min: " + minVersionCode); // LOG 6


            } else {
                Log.w("UpdateCheck", "Fetch and activate FAILED."); // LOG 7
            }
        });
    }

    private void showUpdateDialog(boolean isForced) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_title))
                .setMessage(getString(R.string.update_message))
                .setPositiveButton(R.string.upfdate_button, (dialog, which) -> {
                    // redirection vers PlayStore
                    final String appPackageName = getPackageName();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_url) + appPackageName)));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.play_store_url) + appPackageName)));
                    }
                });
        if (isForced) {
            builder.setCancelable(false);
        } else {
            builder.setNegativeButton(R.string.later, (dialog, which) -> dialog.dismiss());
        }
        if (!isFinishing()) {
            builder.create().show();
        }
    }

}