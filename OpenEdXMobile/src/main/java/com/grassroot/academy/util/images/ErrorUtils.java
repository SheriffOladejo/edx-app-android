package com.grassroot.academy.util.images;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.grassroot.academy.R;
import com.grassroot.academy.exception.CourseContentNotValidException;
import com.grassroot.academy.http.HttpStatus;
import com.grassroot.academy.http.HttpStatusException;
import com.grassroot.academy.http.callback.CallTrigger;
import com.grassroot.academy.http.notifications.DialogErrorNotification;
import com.grassroot.academy.http.notifications.ErrorNotification;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.util.NetworkUtil;

import java.io.IOException;

public enum ErrorUtils {
    ;

    protected static final Logger logger = new Logger(ErrorUtils.class.getName());

    @NonNull
    public static String getErrorMessage(@NonNull Throwable error, @NonNull Context context) {
        return context.getString(getErrorMessageRes(context, error));
    }

    @NonNull
    public static String getErrorMessage(@NonNull Throwable error, @NonNull CallTrigger callTrigger,
                                         @NonNull Context context) {
        return context.getString(getErrorMessageRes(context, error, callTrigger));
    }

    @StringRes
    public static int getErrorMessageRes(@NonNull Context context, @NonNull Throwable error) {
        return getErrorMessageRes(context, error, CallTrigger.LOADING_UNCACHED);
    }

    @StringRes
    public static int getErrorMessageRes(@NonNull Context context, @NonNull Throwable error,
                                         @NonNull ErrorNotification errorNotification) {
        if (errorNotification instanceof DialogErrorNotification) {
            return getErrorMessageRes(context, error, CallTrigger.USER_ACTION);
        }
        return getErrorMessageRes(context, error, CallTrigger.LOADING_UNCACHED);
    }

    @StringRes
    public static int getErrorMessageRes(@NonNull Context context, @NonNull Throwable error,
                                         @NonNull CallTrigger callTrigger) {
        @StringRes
        int errorResId = R.string.error_unknown;
        if (error instanceof IOException) {
            if (NetworkUtil.isConnected(context)) {
                errorResId = R.string.network_connected_error;
            } else {
                errorResId = R.string.reset_no_network_message;
            }
        } else if (error instanceof HttpStatusException) {
            switch (((HttpStatusException) error).getStatusCode()) {
                case HttpStatus.SERVICE_UNAVAILABLE:
                case HttpStatus.INTERNAL_SERVER_ERROR:
                    errorResId = R.string.network_service_unavailable;
                    break;
                case HttpStatus.BAD_REQUEST:
                case HttpStatus.NOT_FOUND:
                    if (callTrigger == CallTrigger.USER_ACTION) {
                        errorResId = R.string.action_not_completed;
                    }
                    break;
                case HttpStatus.UPGRADE_REQUIRED:
                    errorResId = R.string.app_version_unsupported;
                    break;
            }
        } else if (error instanceof CourseContentNotValidException) {
            errorResId = R.string.course_error_content_invalid;
        }
        if (errorResId == R.string.error_unknown) {
            // Submit crash report since this is an unknown type of error
            logger.error(error, true);
        }
        return errorResId;
    }

    @DrawableRes
    public static int getErrorIconResId(@NonNull Throwable ex) {
        if (ex instanceof IOException) {
            return R.drawable.ic_wifi;
        } else if (ex instanceof HttpStatusException || ex instanceof CourseContentNotValidException) {
            return R.drawable.ic_error;
        } else {
            return 0;
        }
    }
}
