package com.mrcktz.rxjavaformvalidation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subscribers.DisposableSubscriber;

public class RxExamplesActivity extends AppCompatActivity {

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private Button mEmailSignInButton;
    private Button mClickButton;
    private TextView mClickResultText;
    private TextView mMotivationText;
    private CompositeDisposable mDisposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);

        mClickButton = (Button) findViewById(R.id.click_counter_button);
        mClickResultText = (TextView) findViewById(R.id.latest_click_result_textview);
        mMotivationText = (TextView) findViewById(R.id.motivation_textview);

        mDisposables = new CompositeDisposable();
        setupRxLoginForm();
        setupRxClickCounter();
    }

    private void setupRxLoginForm() {
        mEmailSignInButton.setEnabled(false);

        DisposableSubscriber<Boolean> formValidationDisposable = new DisposableSubscriber<Boolean>() {

            @Override
            public void onNext(Boolean formValid) {
                mEmailSignInButton.setEnabled(formValid);
            }

            @Override
            public void onError(Throwable t) {
               /* Error */
            }

            @Override
            public void onComplete() {
                /* Completed */
            }
        };

        Flowable<Boolean> emailObservable = RxJavaInterop.toV2Flowable(RxTextView.textChanges(mEmailView))
                .flatMap((chars) -> {
                    final Editable editable = mEmailView.getText();
                    return Flowable.just(editable != null
                            && isEmailValid(editable.toString()));
                }).skip(1);

        Flowable<Boolean> passwordObservable = RxJavaInterop.toV2Flowable(
                RxTextView.textChanges(mPasswordView)
                        .filter(charSequence -> charSequence != null))
                .flatMap((chars) -> {
                    final Editable editable = mPasswordView.getText();
                    return Flowable.just(editable != null
                            && isPasswordValid(editable.toString()));
                }).skip(1);

        Flowable.combineLatest(emailObservable, passwordObservable,
                (emailValid, passwordValid) -> {
                    if (!emailValid) {
                        mEmailView.setError("Email invalid");
                    }
                    if (!passwordValid) {
                        mPasswordView.setError("Password invalid");
                    }
                    return emailValid && passwordValid;
                }).subscribeWith(formValidationDisposable);
    }

    private void setupRxClickCounter() {
        Flowable<List<Void>> clicksObservable = RxJavaInterop.toV2Flowable(RxView.clicks(mClickButton)
                .buffer(1, TimeUnit.SECONDS)
                .filter((clicks) -> clicks.size() > 0))
                .observeOn(AndroidSchedulers.mainThread());

        mDisposables.add(clicksObservable
                .flatMap(voids -> {
                    int clicks = voids.size();
                    mClickResultText.setText(clicks + " clicks/s");
                    if (clicks <= 3) {
                        return Flowable.just("Not impressive...");
                    } else if (clicks > 3 && clicks <= 6) {
                        return Flowable.just("You can do better!");
                    } else if (clicks > 6 && clicks <= 8) {
                        return Flowable.just("Nice!");
                    } else if (clicks > 8 && clicks <= 10) {
                        return Flowable.just("Awesome!");
                    } else if (clicks > 10) {
                        return Flowable.just("Godlike!");
                    } else {
                        return Flowable.just("WTF?!");
                    }
                }).subscribe((motivationalText) -> mMotivationText.setText(motivationalText)));
    }

    @Override
    protected void onDestroy() {
        mDisposables.dispose();
        super.onDestroy();
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email != null && email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

}

