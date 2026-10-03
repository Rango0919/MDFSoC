package com.example.mdfsoc.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.LoginResponse;
import com.example.mdfsoc.repositories.AuthRepository;
import com.example.mdfsoc.repositories.RepositoryCallback;
import com.example.mdfsoc.utils.ApiErrorParser;
import com.example.mdfsoc.utils.Resource;

import retrofit2.Call;
import retrofit2.Response;

public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<Resource<LoginResponse>> loginResult = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public LiveData<Resource<LoginResponse>> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        loginResult.setValue(Resource.loading());
        repository.login(username, password, new RepositoryCallback<LoginResponse>() {
            @Override
            public void onSuccess(Call<ApiEnvelope<LoginResponse>> call,
                                  ApiEnvelope<LoginResponse> envelope) {
                if (envelope.isSuccess() && envelope.getData() != null
                        && envelope.getData().getToken() != null) {
                    loginResult.setValue(Resource.success(envelope.getData()));
                    return;
                }
                String message = envelope.getError() != null
                        ? envelope.getError().getMessage()
                        : ApiErrorParser.messageFor(getApplication(), null, null);
                loginResult.setValue(Resource.error(message));
            }

            @Override
            public void onFailure(Call<ApiEnvelope<LoginResponse>> call,
                                  Response<?> response, Throwable throwable) {
                String message = ApiErrorParser.messageFor(getApplication(), throwable, response);
                loginResult.setValue(Resource.error(message));
            }
        });
    }
}