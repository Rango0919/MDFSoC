package com.example.mdfsoc.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mdfsoc.models.Alert;
import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.repositories.AlertsRepository;
import com.example.mdfsoc.repositories.RepositoryCallback;
import com.example.mdfsoc.utils.ApiErrorParser;
import com.example.mdfsoc.utils.Resource;

import retrofit2.Call;
import retrofit2.Response;

public class AlertDetailViewModel extends AndroidViewModel {

    private final AlertsRepository repository;
    private final MutableLiveData<Resource<Alert>> alertDetail = new MutableLiveData<>();

    public AlertDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new AlertsRepository(application);
    }

    public LiveData<Resource<Alert>> getAlertDetail() {
        return alertDetail;
    }

    public void load(long id) {
        alertDetail.setValue(Resource.loading());
        repository.loadAlert(id, new RepositoryCallback<Alert>() {
            @Override
            public void onSuccess(Call<ApiEnvelope<Alert>> call, ApiEnvelope<Alert> envelope) {
                if (envelope.isSuccess() && envelope.getData() != null) {
                    alertDetail.setValue(Resource.success(envelope.getData()));
                    return;
                }
                alertDetail.setValue(Resource.error(
                        ApiErrorParser.messageFor(getApplication(), null, null)));
            }

            @Override
            public void onFailure(Call<ApiEnvelope<Alert>> call,
                                  Response<?> response, Throwable throwable) {
                String message = ApiErrorParser.messageFor(getApplication(), throwable, response);
                alertDetail.setValue(Resource.error(message));
            }
        });
    }
}