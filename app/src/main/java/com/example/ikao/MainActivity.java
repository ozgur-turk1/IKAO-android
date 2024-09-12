package com.example.ikao;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView myWebView = findViewById(R.id.webview);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.getSettings().setJavaScriptEnabled(true);  // Activer JavaScript si nécessaire
        myWebView.loadUrl("https://www.ikao.fr");  // Charger le site web dans la WebView

        // S'abonner à un sujet pour recevoir des notifications
        FirebaseMessaging.getInstance().subscribeToTopic("promotions")
                .addOnCompleteListener(task -> {
                    String msg = "Abonné aux notifications promotions!";
                    if (!task.isSuccessful()) {
                        msg = "Abonnement échoué!";
                    }
                    Log.d("MainActivity", msg);
                });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
