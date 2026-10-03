package com.example.mdfsoc.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.DashboardData;
import com.example.mdfsoc.repositories.DashboardRepository;
import com.example.mdfsoc.repositories.RepositoryCallback;
import com.example.mdfsoc.utils.ApiErrorParser;
import com.example.mdfsoc.utils.Resource;

import retrofit2.Call;
import retrofit2.Response;

public class DashboardViewModel extends AndroidViewModel {

    private final DashboardRepository repository;
    private final MutableLiveData<Resource<DashboardData>> dashboardData = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = new DashboardRepository(application);
    }

    public LiveData<Resource<DashboardData>> getDashboardData() {
        return dashboardData;
    }

    public void refresh() {
        dashboardData.setValue(Resource.loading());
        repository.loadDashboard(new RepositoryCallback<DashboardData>() {
            @Override
            public void onSuccess(Call<ApiEnvelope<DashboardData>> call,
                                  ApiEnvelope<DashboardData> envelope) {
                if (envelope.isSuccess() && envelope.getData() != null) {
                    dashboardData.setValue(Resource.success(envelope.getData()));
                } else {
                    dashboardData.setValue(Resource.error(
                            ApiErrorParser.messageFor(getApplication(), null, null)));
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<DashboardData>> call,
                                  Response<?> response, Throwable throwable) {
                String message = ApiErrorParser.messageFor(getApplication(), throwable, response);
                dashboardData.setValue(Resource.error(message));
            }
        });
    }
}