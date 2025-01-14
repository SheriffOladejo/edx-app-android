package com.grassroot.academy.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.databinding.DataBindingUtil;

import com.grassroot.academy.BuildConfig;
import com.grassroot.academy.R;
import com.grassroot.academy.authentication.LoginTask;
import com.grassroot.academy.databinding.ActivityLoginBinding;
import com.grassroot.academy.deeplink.DeepLink;
import com.grassroot.academy.deeplink.DeepLinkManager;
import com.grassroot.academy.exception.LoginErrorMessage;
import com.grassroot.academy.exception.LoginException;
import com.grassroot.academy.http.HttpStatus;
import com.grassroot.academy.http.HttpStatusException;
import com.grassroot.academy.model.authentication.AuthResponse;
import com.grassroot.academy.module.analytics.Analytics;
import com.grassroot.academy.module.prefs.LoginPrefs;
import com.grassroot.academy.social.SocialFactory;
import com.grassroot.academy.social.SocialLoginDelegate;
import com.grassroot.academy.task.Task;
import com.grassroot.academy.util.AppStoreUtils;
import com.grassroot.academy.util.Config;
import com.grassroot.academy.util.IntentFactory;
import com.grassroot.academy.util.NetworkUtil;
import com.grassroot.academy.util.TextUtils;
import com.grassroot.academy.util.images.ErrorUtils;
import com.grassroot.academy.view.dialog.ResetPasswordDialogFragment;
import com.grassroot.academy.view.login.LoginPresenter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity
        extends PresenterActivity<LoginPresenter, LoginPresenter.LoginViewInterface>
        implements SocialLoginDelegate.MobileLoginCallback {
    private SocialLoginDelegate socialLoginDelegate;
    private ActivityLoginBinding activityLoginBinding;

    @Inject
    LoginPrefs loginPrefs;

    @NonNull
    public static Intent newIntent(@Nullable DeepLink deepLink) {
        final Intent intent = IntentFactory.newIntentForComponent(LoginActivity.class);
        intent.putExtra(Router.EXTRA_DEEP_LINK, deepLink);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    @NonNull
    @Override
    protected LoginPresenter createPresenter(@Nullable Bundle savedInstanceState) {
        return new LoginPresenter(
                environment.getConfig(),
                new NetworkUtil.ZeroRatedNetworkInfo(getApplicationContext(), environment.getConfig()));
    }

    @NonNull
    @Override
    protected LoginPresenter.LoginViewInterface createView(@Nullable Bundle savedInstanceState) {
        activityLoginBinding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        hideSoftKeypad();
        socialLoginDelegate = new SocialLoginDelegate(this, savedInstanceState, this,
                environment.getConfig(), environment.getLoginPrefs(), SocialLoginDelegate.Feature.SIGN_IN);

        activityLoginBinding.socialAuth.facebookButton.getRoot().setOnClickListener(
                socialLoginDelegate.createSocialButtonClickHandler(
                        SocialFactory.SOCIAL_SOURCE_TYPE.TYPE_FACEBOOK));
        activityLoginBinding.socialAuth.googleButton.getRoot().setOnClickListener(
                socialLoginDelegate.createSocialButtonClickHandler(
                        SocialFactory.SOCIAL_SOURCE_TYPE.TYPE_GOOGLE));
        activityLoginBinding.socialAuth.microsoftButton.getRoot().setOnClickListener(
                socialLoginDelegate.createSocialButtonClickHandler(
                        SocialFactory.SOCIAL_SOURCE_TYPE.TYPE_MICROSOFT));

        activityLoginBinding.loginButtonLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check for Validation˜
                callServerForLogin();
            }
        });

        activityLoginBinding.forgotPasswordTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Calling help dialog
                if (NetworkUtil.isConnected(LoginActivity.this)) {
                    showResetPasswordDialog();
                } else {
                    showAlertDialog(getString(R.string.reset_no_network_title), getString(R.string.network_not_connected));
                }
            }
        });

        activityLoginBinding.emailEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                activityLoginBinding.usernameWrapper.setError(null);
            }
        });

        activityLoginBinding.passwordEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                activityLoginBinding.passwordWrapper.setError(null);
            }
        });

        activityLoginBinding.endUserAgreementTv.setMovementMethod(LinkMovementMethod.getInstance());
        activityLoginBinding.endUserAgreementTv.setText(TextUtils.generateLicenseText(
                environment.getConfig(), this, R.string.by_signing_in));

        environment.getAnalyticsRegistry().trackScreenView(Analytics.Screens.LOGIN);

        // enable login buttons at launch
        tryToSetUIInteraction(true);

        Config config = environment.getConfig();
        setTitle(getString(R.string.login_title));

        String envDisplayName = config.getEnvironmentDisplayName();
        if (envDisplayName != null && envDisplayName.length() > 0) {
            activityLoginBinding.versionEnvTv.setVisibility(View.VISIBLE);
            String versionName = BuildConfig.VERSION_NAME;
            String text = String.format("%s %s %s",
                    getString(R.string.label_version), versionName, envDisplayName);
            activityLoginBinding.versionEnvTv.setText(text);
        }

        return new LoginPresenter.LoginViewInterface() {
            @Override
            public void disableToolbarNavigation() {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setHomeButtonEnabled(false);
                    actionBar.setDisplayHomeAsUpEnabled(false);
                    actionBar.setDisplayShowHomeEnabled(false);
                }
            }

            @Override
            public void setSocialLoginButtons(boolean googleEnabled, boolean facebookEnabled,
                                              boolean microsoftEnabled) {
                if (!facebookEnabled && !googleEnabled && !microsoftEnabled) {
                    activityLoginBinding.panelLoginSocial.setVisibility(View.GONE);
                } else {
                    if (!facebookEnabled) {
                        activityLoginBinding.socialAuth.facebookButton.getRoot().setVisibility(View.GONE);
                    }
                    if (!googleEnabled) {
                        activityLoginBinding.socialAuth.googleButton.getRoot().setVisibility(View.GONE);
                    }
                    if (!microsoftEnabled) {
                        activityLoginBinding.socialAuth.microsoftButton.getRoot().setVisibility(View.GONE);
                    }
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socialLoginDelegate.onActivityDestroyed();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("username", activityLoginBinding.emailEt.getText().toString().trim());

        socialLoginDelegate.onActivitySaveInstanceState(outState);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (activityLoginBinding.emailEt.getText().toString().length() == 0) {
            displayLastEmailId();
        }

        socialLoginDelegate.onActivityStarted();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            activityLoginBinding.emailEt.setText(savedInstanceState.getString("username"));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        tryToSetUIInteraction(true);
        socialLoginDelegate.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ResetPasswordDialogFragment.REQUEST_CODE: {
                if (resultCode == Activity.RESULT_OK) {
                    showAlertDialog(getString(R.string.success_dialog_title_help),
                            getString(R.string.success_dialog_message_help));
                }
                break;
            }
        }
    }

    private void displayLastEmailId() {
        activityLoginBinding.emailEt.setText(loginPrefs.getLastAuthenticatedEmail());
    }

    @SuppressLint("StaticFieldLeak")
    public void callServerForLogin() {
        if (!NetworkUtil.isConnected(this)) {
            showAlertDialog(getString(R.string.no_connectivity),
                    getString(R.string.network_not_connected));
            return;
        }

        final String emailStr = activityLoginBinding.emailEt.getText().toString().trim();
        final String passwordStr = activityLoginBinding.passwordEt.getText().toString().trim();

        if (passwordStr.length() == 0) {
            activityLoginBinding.passwordWrapper.setError(getString(R.string.error_enter_password));
            activityLoginBinding.passwordEt.requestFocus();
        }
        if (emailStr.length() == 0) {
            activityLoginBinding.usernameWrapper.setError(getString(R.string.error_enter_email));
            activityLoginBinding.emailEt.requestFocus();
        }
        if (emailStr.length() > 0 && passwordStr.length() > 0) {
            activityLoginBinding.emailEt.setEnabled(false);
            activityLoginBinding.passwordEt.setEnabled(false);
            activityLoginBinding.forgotPasswordTv.setEnabled(false);
            activityLoginBinding.endUserAgreementTv.setEnabled(false);

            LoginTask logintask = new LoginTask(this, activityLoginBinding.emailEt.getText().toString().trim(),
                    activityLoginBinding.passwordEt.getText().toString()) {
                @Override
                protected void onPostExecute(AuthResponse result) {
                    super.onPostExecute(result);
                    if (result != null) {
                        onUserLoginSuccess();
                    }
                }

                @Override
                public void onException(Exception ex) {
                    if (ex instanceof HttpStatusException &&
                            ((HttpStatusException) ex).getStatusCode() == HttpStatus.BAD_REQUEST) {
                        onUserLoginFailure(new LoginException(new LoginErrorMessage(
                                getString(R.string.login_error),
                                getString(R.string.login_failed))), null, null);
                    } else {
                        onUserLoginFailure(ex, null, null);
                    }
                }
            };
            tryToSetUIInteraction(false);
            logintask.setProgressDialog(activityLoginBinding.progress.progressIndicator);
            logintask.execute();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        socialLoginDelegate.onActivityStopped();
    }

    public String getEmail() {
        return activityLoginBinding.emailEt.getText().toString().trim();
    }

    private void showResetPasswordDialog() {
        ResetPasswordDialogFragment.newInstance(getEmail()).show(getSupportFragmentManager(), null);
    }

    // make sure that on the login activity, all errors show up as a dialog as opposed to a flying snackbar
    @Override
    public void showAlertDialog(@Nullable String header, @NonNull String message) {
        super.showAlertDialog(header, message);
    }


    /**
     * Starts fetching profile of the user after login by Facebook or Google.
     *
     * @param accessToken
     * @param backend
     */
    public void onSocialLoginSuccess(String accessToken, String backend, Task task) {
        tryToSetUIInteraction(false);
        task.setProgressDialog(activityLoginBinding.progress.progressIndicator);
    }

    public void onUserLoginSuccess() {
        setResult(RESULT_OK);
        finish();
        final DeepLink deepLink = getIntent().getParcelableExtra(Router.EXTRA_DEEP_LINK);
        if (deepLink != null) {
            DeepLinkManager.onDeepLinkReceived(this, deepLink);
            return;
        }
        if (!environment.getConfig().isRegistrationEnabled()) {
            environment.getRouter().showMainDashboard(this);
        }
    }

    public void onUserLoginFailure(Exception ex, String accessToken, String backend) {
        Toast.makeText(LoginActivity.this, "Exception: " + ex.toString(), Toast.LENGTH_LONG).show();
        System.out.println("Exception: " + ex.toString());
        tryToSetUIInteraction(true);

        if (ex != null && ex instanceof LoginException) {
            LoginErrorMessage errorMessage = (((LoginException) ex).getLoginErrorMessage());
            showAlertDialog(
                    errorMessage.getMessageLine1(),
                    (errorMessage.getMessageLine2() != null) ?
                            errorMessage.getMessageLine2() : getString(R.string.login_failed));
        } else if (ex != null && ex instanceof HttpStatusException) {
            switch (((HttpStatusException) ex).getStatusCode()) {
                case HttpStatus.UPGRADE_REQUIRED:

                    LoginActivity.this.showAlertDialog(null,
                            getString(R.string.app_version_unsupported_login_msg),
                            getString(R.string.label_update),
                            (dialog, which) -> AppStoreUtils
                                    .openAppInAppStore(LoginActivity.this),
                            getString(android.R.string.cancel), null);
                    break;
                case HttpStatus.FORBIDDEN:
                    Toast.makeText(LoginActivity.this, "here 1", Toast.LENGTH_LONG).show();
                    LoginActivity.this.showAlertDialog(getString(R.string.login_error),
                            getString(R.string.auth_provider_disabled_user_error),
                            getString(R.string.label_customer_support),
                            (dialog, which) -> environment.getRouter()
                                    .showFeedbackScreen(LoginActivity.this,
                                            getString(R.string.email_subject_account_disabled)), getString(android.R.string.cancel), null);
                    break;
                default:
                    Toast.makeText(LoginActivity.this, "here 1", Toast.LENGTH_LONG).show();
                    showAlertDialog(getString(R.string.login_error), ErrorUtils.getErrorMessage(ex, LoginActivity.this));
                    logger.error(ex);
            }
        } else {
            showAlertDialog(getString(R.string.login_error), ErrorUtils.getErrorMessage(ex, LoginActivity.this));
            logger.error(ex);
        }
    }

    @Override
    public boolean tryToSetUIInteraction(boolean enable) {
        if (enable) {
            unblockTouch();
            activityLoginBinding.loginButtonLayout.setEnabled(enable);
            activityLoginBinding.loginBtnTv.setText(getString(R.string.login));
        } else {
            blockTouch();
            activityLoginBinding.loginButtonLayout.setEnabled(enable);
            activityLoginBinding.loginBtnTv.setText(getString(R.string.signing_in));
        }


        activityLoginBinding.socialAuth.facebookButton.getRoot().setClickable(enable);
        activityLoginBinding.socialAuth.googleButton.getRoot().setClickable(enable);
        activityLoginBinding.socialAuth.microsoftButton.getRoot().setClickable(enable);

        activityLoginBinding.emailEt.setEnabled(enable);
        activityLoginBinding.passwordEt.setEnabled(enable);

        activityLoginBinding.forgotPasswordTv.setEnabled(enable);
        activityLoginBinding.endUserAgreementTv.setEnabled(enable);

        return true;
    }
}
