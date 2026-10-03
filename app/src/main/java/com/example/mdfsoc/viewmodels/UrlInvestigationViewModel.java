package com.example.mdfsoc.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mdfsoc.R;
import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.UrlReputation;
import com.example.mdfsoc.repositories.RepositoryCallback;
import com.example.mdfsoc.repositories.UrlInvestigationRepository;
import com.example.mdfsoc.utils.ApiErrorParser;
import com.example.mdfsoc.utils.PhishingTextParser;
import com.example.mdfsoc.utils.Resource;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Response;

public class UrlInvestigationViewModel extends AndroidViewModel {

    private final UrlInvestigationRepository repository;
    private final MutableLiveData<Resource<UrlReputation>> result = new MutableLiveData<>();
    private final MutableLiveData<java.util.List<String>> warnings = new MutableLiveData<>();
    private String lastUrl;

    public UrlInvestigationViewModel(@NonNull Application application) {
        super(application);
        repository = new UrlInvestigationRepository(application);
    }

    public LiveData<Resource<UrlReputation>> getResult() {
        return result;
    }

    public LiveData<java.util.List<String>> getWarnings() {
        return warnings;
    }

    public void retry() {
        if (lastUrl != null && !lastUrl.isEmpty()) {
            investigate(lastUrl);
        }
    }

    public void investigate(String rawUrl) {
        String normalized = PhishingTextParser.normalize(rawUrl);
        if (normalized.isEmpty()) {
            result.setValue(Resource.error(getApplication().getString(R.string.error_empty_url)));
            return;
        }
        lastUrl = normalized;
        result.setValue(Resource.loading());
        warnings.setValue(Collections.emptyList());
        repository.loadUrlReputation(normalized, new RepositoryCallback<UrlReputation>() {
            @Override
            public void onSuccess(Call<ApiEnvelope<UrlReputation>> call,
                                  ApiEnvelope<UrlReputation> envelope) {
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
            public void onFailure(Call<ApiEnvelope<UrlReputation>> call,
                                  Response<?> response, Throwable throwable) {
                result.setValue(Resource.error(
                        ApiErrorParser.messageFor(getApplication(), throwable, response)));
            }
        });
    }
}