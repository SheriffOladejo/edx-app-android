package com.grassroot.academy.http.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.grassroot.academy.BuildConfig;
import com.grassroot.academy.R;
import com.grassroot.academy.http.authenticator.OauthRefreshTokenAuthenticator;
import com.grassroot.academy.http.interceptor.NewVersionBroadcastInterceptor;
import com.grassroot.academy.http.interceptor.NoCacheHeaderStrippingInterceptor;
import com.grassroot.academy.http.interceptor.OauthHeaderRequestInterceptor;
import com.grassroot.academy.http.interceptor.StaleIfErrorHandlingInterceptor;
import com.grassroot.academy.http.interceptor.StaleIfErrorInterceptor;
import com.grassroot.academy.http.interceptor.UserAgentInterceptor;
import com.grassroot.academy.http.util.Tls12SocketFactory;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public interface OkHttpClientProvider extends Provider<OkHttpClient> {

    @NonNull
    OkHttpClient get();

    @NonNull
    OkHttpClient getWithOfflineCache();

    @NonNull
    OkHttpClient getNonOAuthBased();

    @Singleton
    class Impl implements OkHttpClientProvider {
        private static final int cacheSize = 10 * 1024 * 1024; // 10 MiB

        private static final int FLAG_IS_OAUTH_BASED = 1;
        private static final int USES_OFFLINE_CACHE = 1 << 1;

        private final Context context;

        private final OkHttpClient[] clients = new OkHttpClient[1 << 2];
        private final OauthRefreshTokenAuthenticator oauthRefreshTokenAuthenticator;

        @Inject
        public Impl(@ApplicationContext Context context,
                    OauthRefreshTokenAuthenticator oauthRefreshTokenAuthenticator) {
            this.context = context;
            this.oauthRefreshTokenAuthenticator = oauthRefreshTokenAuthenticator;
        }

        @NonNull
        @Override
        public OkHttpClient get() {
            return get(true, false);
        }

        @NonNull
        @Override
        public OkHttpClient getWithOfflineCache() {
            return get(true, true);
        }

        @NonNull
        @Override
        public OkHttpClient getNonOAuthBased() {
            return get(false, false);
        }

        @NonNull
        private synchronized OkHttpClient get(boolean isOAuthBased, boolean usesOfflineCache) {
            final int index = (isOAuthBased ? FLAG_IS_OAUTH_BASED : 0) |
                    (usesOfflineCache ? USES_OFFLINE_CACHE : 0);
            OkHttpClient client = clients[index];
            if (client == null) {
                final OkHttpClient.Builder builder = new OkHttpClient.Builder();
                List<Interceptor> interceptors = builder.interceptors();
                if (usesOfflineCache) {
                    final File cacheDirectory = new File(context.getFilesDir(), "http-cache");
                    if (!cacheDirectory.exists()) {
                        cacheDirectory.mkdirs();
                    }
                    final Cache cache = new Cache(cacheDirectory, cacheSize);
                    builder.cache(cache);
                    interceptors.add(new StaleIfErrorInterceptor());
                    interceptors.add(new StaleIfErrorHandlingInterceptor());
                    builder.networkInterceptors().add(new NoCacheHeaderStrippingInterceptor());
                }
                interceptors.add(new UserAgentInterceptor(
                        System.getProperty("http.agent") + " " +
                                context.getString(R.string.app_name) + "/" +
                                BuildConfig.APPLICATION_ID + "/" +
                                BuildConfig.VERSION_NAME));
                if (isOAuthBased) {
                    interceptors.add(new OauthHeaderRequestInterceptor(context));
                    interceptors.add(oauthRefreshTokenAuthenticator);
                }
                interceptors.add(new NewVersionBroadcastInterceptor());
                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    interceptors.add(loggingInterceptor);
                }
                builder.authenticator(oauthRefreshTokenAuthenticator);
                // Enable TLS 1.2 support
                client = Tls12SocketFactory.enableTls12OnPreLollipop(builder).build();
                clients[index] = client;
            }
            return client;
        }
    }
}
