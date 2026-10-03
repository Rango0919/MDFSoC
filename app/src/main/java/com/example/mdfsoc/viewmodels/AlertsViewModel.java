package com.example.mdfsoc.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.AlertsPage;
import com.example.mdfsoc.repositories.AlertsRepository;
import com.example.mdfsoc.repositories.RepositoryCallback;
import com.example.mdfsoc.utils.ApiErrorParser;
import com.example.mdfsoc.utils.Resource;

import retrofit2.Call;
import retrofit2.Response;

public class AlertsViewModel extends AndroidViewModel {

    private static final int PAGE_SIZE = 50;

    private final AlertsRepository repository;
    private final MutableLiveData<Resource<AlertsPage>> alerts = new MutableLiveData<>();
    private String currentSeverity = "all";

    public AlertsViewModel(@NonNull Application application) {
        super(application);
        repository = new AlertsRepository(application);
    }

    public LiveData<Resource<AlertsPage>> getAlerts() {
        return alerts;
    }

    public String getCurrentSeverity() {
        return currentSeverity;
    }

    public void loadAlerts(String severity) {
        currentSeverity = severity == null ? "all" : severity;
        alerts.setValue(Resource.loading());
        repository.loadAlerts(currentSeverity, PAGE_SIZE, 0,
                new RepositoryCallback<AlertsPage>() {
                    @Override
                    public void onSuccess(Call<ApiEnvelope<AlertsPage>> call,
                                          ApiEnvelope<AlertsPage> envelope) {
                        if (envelope.isSuccess() && envelope.getData() != null) {
                            alerts.setValue(Resource.success(envelope.getData()));
                            return;
                        }
                        alerts.setValue(Resource.error(
                                ApiErrorParser.messageFor(getApplication(), null, null)));
                    }

                    @Override
                    public void onFailure(Call<ApiEnvelope<AlertsPage>> call,
                                          Response<?> response, Throwable throwable) {
                        String message = ApiErrorParser.messageFor(getApplication(), throwable, response);
                        alerts.setValue(Resource.error(message));
                    }
                });
    }
}