package com.grassroot.academy.event;

import androidx.annotation.NonNull;

import com.grassroot.academy.model.user.Account;

/**
 * Fired whenever we get/update a user's account details.
 */
public class AccountDataLoadedEvent {
    @NonNull
    private final Account account;

    public AccountDataLoadedEvent(@NonNull Account account) {
        this.account = account;
    }

    @NonNull
    public Account getAccount() {
        return account;
    }
}
