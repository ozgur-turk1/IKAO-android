package fr.ikao;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
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
        myWebView.getSettings().setJavaScriptEnabled(true);  // Activer JavaScript si nécessaire
        myWebView.getSettings().setDomStorageEnabled(true); // Si nécessaire pour les sessions
        myWebView.getSettings().setAllowFileAccess(false); // Désactiver l'accès aux fichiers locaux

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
                .setTitle("Activer les notifications")
                .setMessage("Souhaitez-vous recevoir des notifications pour les nouvelles et les promotions ?")
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
                    String msg = "Notifications activées";
                    if (!task.isSuccessful()) {
                        msg = "Échec de l'activation des notifications";
                    }
                    Log.d("MyApp", msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void disableNotifications() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("promotions")
                .addOnCompleteListener(task -> {
                    String msg = "Notifications désactivées";
                    if (!task.isSuccessful()) {
                        msg = "Échec de la désactivation des notifications";
                    }
                    Log.d("MyApp", msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUpdate() {
        Log.d("UpdateCheck", "Starting check for update..."); // LOG 1

        RemoteConfigManager.fetchAndActivate(new RemoteConfigManager.OnConfigUpdateListener() {
            @Override
            public void onFetched(boolean isSuccess) {
                if (isSuccess) {
                    Log.d("UpdateCheck", "Fetch and activate SUCCESSFUL."); // LOG 2

                    long minVersionCode = RemoteConfigManager.getMinVersionCode();
                    Log.d("UpdateCheck", "Min version from Firebase: " + minVersionCode); // LOG 3

                    boolean isForced = RemoteConfigManager.isUpdateForced();

                    long currentVersionCode;

                    try {
                        PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            currentVersionCode = packageInfo.getLongVersionCode();
                        } else {
                            currentVersionCode = packageInfo.versionCode;
                        }
                        Log.d("UpdateCheck", "Current app version: " + currentVersionCode); // LOG 4

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
            }


        });
    }

    private void showUpdateDialog(boolean isForced) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_title))
                .setMessage(getString(R.string.update_message))
                .setPositiveButton(R.string.upfdate_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // redirection vers PlayStore
                        final String appPackageName = getPackageName();
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_url) + appPackageName)));
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.play_store_url) + appPackageName)));
                        }
                    }
                });
        if (isForced) {
            builder.setCancelable(false);
        } else {
            builder.setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }
        if (!isFinishing()) {
            builder.create().show();
        }
    }

}