package com.example.ikao;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
                    onBackPressed(); // Appeler le comportement par défaut pour quitter
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
}