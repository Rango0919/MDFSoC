package com.example.mdfsoc.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mdfsoc.R;
import com.example.mdfsoc.utils.SessionManager;
import com.example.mdfsoc.utils.Resource;
import com.example.mdfsoc.viewmodels.LoginViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private View loginButton;
    private TextView errorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = SessionManager.getInstance(this);
        if (session.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.input_username);
        passwordInput = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.btn_login);
        errorView = findViewById(R.id.login_error);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        loginButton.setOnClickListener(v -> attemptLogin());

        viewModel.getLoginResult().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.isLoading()) {
                loginButton.setEnabled(false);
                errorView.setVisibility(View.GONE);
                return;
            }
            loginButton.setEnabled(true);
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                SessionManager.getInstance(this).saveSession(
                        resource.getData().getToken(),
                        resource.getData().getExpiresAt(),
                        getUsername());
                finish();
            } else {
                showError(resource.getMessage() != null
                        ? resource.getMessage()
                        : getString(R.string.error_auth_failed));
            }
        });
    }

    private void attemptLogin() {
        String username = inputText(usernameInput);
        String password = inputText(passwordInput);
        errorView.setVisibility(View.GONE);
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_auth_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.login(username, password);
    }

    private String getUsername() {
        return inputText(usernameInput);
    }

    private String inputText(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }
}