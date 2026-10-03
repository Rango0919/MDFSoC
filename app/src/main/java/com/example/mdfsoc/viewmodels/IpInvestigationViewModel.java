package com.example.mdfsoc.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.ThreatIntelligence;
import com.example.mdfsoc.repositories.IpInvestigationRepository;
import com.example.mdfsoc.repositories.RepositoryCallback;
import com.example.mdfsoc.utils.ApiErrorParser;
import com.example.mdfsoc.utils.IpAddressValidator;
import com.example.mdfsoc.utils.Resource;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class IpInvestigationViewModel extends AndroidViewModel {

    private final IpInvestigationRepository repository;
    private final MutableLiveData<Resource<ThreatIntelligence>> result = new MutableLiveData<>();
    private final MutableLiveData<List<String>> warnings = new MutableLiveData<>();

    public IpInvestigationViewModel(@NonNull Application application) {
        super(application);
        repository = new IpInvestigationRepository(application);
    }

    public LiveData<Resource<ThreatIntelligence>> getResult() {
        return result;
    }

    public LiveData<List<String>> getWarnings() {
        return warnings;
    }

    public void investigate(String rawIp) {
        String ip = rawIp == null ? "" : rawIp.trim();
        if (ip.isEmpty()) {
            result.setValue(Resource.error(getApplication()
                    .getString(com.example.mdfsoc.R.string.error_empty_ip)));
            return;
        }
        if (!IpAddressValidator.isValid(ip)) {
            result.setValue(Resource.error(getApplication()
                    .getString(com.example.mdfsoc.R.string.error_invalid_ip)));
            return;
        }
        result.setValue(Resource.loading());
        warnings.setValue(Collections.emptyList());
        repository.loadThreatIntelligence(ip, new RepositoryCallback<ThreatIntelligence>() {
            @Override
            public void onSuccess(Call<ApiEnvelope<ThreatIntelligence>> call,
                                  ApiEnvelope<ThreatIntelligence> envelope) {
                if (envelope.isSuccess() && envelope.getData() != null) {
                    warnings.setValue(envelope.getWarnings() != null
                            ? envelope.getWarnings()
                            : Collections.emptyList());
                    result.setValue(Resource.success(envelope.getData()));
                    return;
                }
                result.setValue(Resource.error(
                        ApiErrorParser.messageFor(getApplication(), null, null)));
            }

            @Override
            public void onFailure(Call<ApiEnvelope<ThreatIntelligence>> call,
                                  Response<?> response, Throwable throwable) {
                String message = ApiErrorParser.messageFor(getApplication(), throwable, response);
                result.setValue(Resource.error(message));
            }
        });
    }
}