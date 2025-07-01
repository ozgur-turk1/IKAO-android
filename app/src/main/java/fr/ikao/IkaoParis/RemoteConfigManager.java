package fr.ikao.IkaoParis;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.BuildConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class RemoteConfigManager {
    private static final String TAG = "RemoteConfigManager";
    private static final String MIN_VERSION_CODE_KEY = "min_android_version_code";
    private static final String IS_UPDATE_FORCED_KEY = "is_update_forced";

    private static FirebaseRemoteConfig mFirebaseRemoteConfig;

    public static FirebaseRemoteConfig getInstance() {
        if (mFirebaseRemoteConfig == null) {
            mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 0 : 3600)
                    .build();

            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        }
        return mFirebaseRemoteConfig;
    }

    public static void fetchAndActivate(OnConfigUpdateListener listener) {
        getInstance().fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                listener.onFetched(task.isSuccessful());
            }
        });
    }

    public static long getMinVersionCode() {
        return getInstance().getLong(MIN_VERSION_CODE_KEY);
    }

    public static boolean isUpdateForced() {
        return getInstance().getBoolean(IS_UPDATE_FORCED_KEY);
    }

    public interface OnConfigUpdateListener {
        void onFetched(boolean isSuccess);
    }

}
