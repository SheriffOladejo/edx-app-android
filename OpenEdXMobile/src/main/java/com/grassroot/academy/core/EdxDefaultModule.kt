package com.grassroot.academy.core

import android.app.DownloadManager
import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.grassroot.academy.authentication.LoginAPI
import com.grassroot.academy.course.CourseAPI
import com.grassroot.academy.discussion.DiscussionService
import com.grassroot.academy.http.provider.OkHttpClientProvider
import com.grassroot.academy.http.provider.RetrofitProvider
import com.grassroot.academy.http.serialization.ISO8601DateTypeAdapter
import com.grassroot.academy.http.serialization.JsonPageDeserializer
import com.grassroot.academy.inapppurchases.InAppPurchasesAPI
import com.grassroot.academy.model.Page
import com.grassroot.academy.model.api.EnrollmentResponse
import com.grassroot.academy.model.authentication.AuthResponse
import com.grassroot.academy.model.course.BlockData
import com.grassroot.academy.model.course.BlockList
import com.grassroot.academy.model.course.BlockType
import com.grassroot.academy.module.db.IDatabase
import com.grassroot.academy.module.db.impl.IDatabaseImpl
import com.grassroot.academy.module.download.IDownloadManager
import com.grassroot.academy.module.download.IDownloadManagerImpl
import com.grassroot.academy.module.notification.DummyNotificationDelegate
import com.grassroot.academy.module.notification.NotificationDelegate
import com.grassroot.academy.module.prefs.LoginPrefs
import com.grassroot.academy.module.storage.IStorage
import com.grassroot.academy.module.storage.Storage
import com.grassroot.academy.player.TranscriptManager
import com.grassroot.academy.repository.CourseDatesRepository
import com.grassroot.academy.repository.InAppPurchasesRepository
import com.grassroot.academy.services.CourseManager
import com.grassroot.academy.services.EdxCookieManager
import com.grassroot.academy.user.UserAPI
import com.grassroot.academy.user.UserService
import org.greenrobot.eventbus.EventBus
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EdxDefaultModule {

    @Binds
    abstract fun bindIEdxEnvironment(edxEnvironment: EdxEnvironment): IEdxEnvironment

    @Binds
    abstract fun bindIStorage(storage: Storage): IStorage

    @Binds
    abstract fun bindIDatabase(database: IDatabaseImpl): IDatabase

    @Binds
    abstract fun bindIDownloadManager(iDownloadManagerImpl: IDownloadManagerImpl): IDownloadManager

    @Binds
    abstract fun bindNotificationDelegate(dummyNotificationDelegate: DummyNotificationDelegate): NotificationDelegate

    @Binds
    abstract fun bindRetrofitProvider(impl: RetrofitProvider.Impl): RetrofitProvider

    @Binds
    abstract fun bindOkHttpClientProvider(impl: OkHttpClientProvider.Impl): OkHttpClientProvider

    /**
     * ref: https://dagger.dev/dev-guide/faq.html#why-cant-binds-and-instance-provides-methods-go-in-the-same-module
     */
    @Module
    @InstallIn(SingletonComponent::class)
    object ProvideModule {

        @Singleton
        @Provides
        fun provideDownloadManager(@ApplicationContext context: Context): DownloadManager {
            return (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
        }

        @Singleton
        @Provides
        fun provideEventBus(): EventBus {
            return EventBus.getDefault()
        }

        @Singleton
        @Provides
        fun provideRetrofit(impl: RetrofitProvider.Impl): Retrofit {
            return impl.get()
        }

        /**
         * The Gson instance for converting the response body to the desired type.
         */
        @Singleton
        @Provides
        fun provideGson(): Gson {
            return GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(ISO8601DateTypeAdapter.FACTORY)
                .registerTypeAdapter(Page::class.java, JsonPageDeserializer())
                .registerTypeAdapter(AuthResponse::class.java, AuthResponse.Deserializer())
                .registerTypeAdapter(BlockList::class.java, BlockList.Deserializer())
                .registerTypeAdapter(BlockType::class.java, BlockType.Deserializer())
                .registerTypeAdapter(BlockData::class.java, BlockData.Deserializer())
                .registerTypeAdapter(
                    EnrollmentResponse::class.java,
                    EnrollmentResponse.Deserializer()
                )
                .serializeNulls()
                .create()
        }

        @Singleton
        @Provides
        fun provideCourseDatesRepository(courseAPI: CourseAPI): CourseDatesRepository {
            return CourseDatesRepository(courseAPI)
        }

        @Singleton
        @Provides
        fun provideInAppPurchasesRepository(iapAPI: InAppPurchasesAPI): InAppPurchasesRepository {
            return InAppPurchasesRepository(iapAPI)
        }
    }

    // Inject dependencies in classes not supported by Hilt
    // Ref: https://developer.android.com/training/dependency-injection/hilt-android#not-supported
    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface ProviderEntryPoint {

        fun getEnvironment(): IEdxEnvironment

        fun getLoginAPI(): LoginAPI

        fun getUserAPI(): UserAPI

        fun getLoginPrefs(): LoginPrefs

        fun getEdxCookieManager(): EdxCookieManager

        fun getCourseManager(): CourseManager

        fun getTranscriptManager(): TranscriptManager

        fun getIDatabase(): IDatabase

        fun getOkHttpClientProvider(): OkHttpClientProvider

        fun getDiscussionService(): DiscussionService

        fun getCourseAPI(): CourseAPI

        fun getUserService(): UserService

        fun getGSon(): Gson

        fun getInAppPurchasesAPI(): InAppPurchasesAPI
    }
}
