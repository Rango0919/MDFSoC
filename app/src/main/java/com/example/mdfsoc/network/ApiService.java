package com.example.mdfsoc.network;

import com.example.mdfsoc.BuildConfig;
import com.example.mdfsoc.models.Alert;
import com.example.mdfsoc.models.AlertsPage;
import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.DashboardData;
import com.example.mdfsoc.models.IpReputation;
import com.example.mdfsoc.models.LoginRequest;
import com.example.mdfsoc.models.LoginResponse;
import com.example.mdfsoc.models.ThreatIntelligence;
import com.example.mdfsoc.models.UrlReputation;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @GET("dashboard.php")
    Call<ApiEnvelope<DashboardData>> getDashboard();

    @GET("alerts.php")
    Call<ApiEnvelope<AlertsPage>> getAlerts(
            @Query("severity") String severity,
            @Query("limit") int limit,
            @Query("offset") int offset);

    @GET("alert.php")
    Call<ApiEnvelope<Alert>> getAlert(@Query("id") long id);

    @GET("ip-reputation.php")
    Call<ApiEnvelope<IpReputation>> getIpReputation(@Query("ip") String ip);

    @GET("url-reputation.php")
    Call<ApiEnvelope<UrlReputation>> getUrlReputation(@Query("url") String url);

    @GET("threat-intelligence.php")
    Call<ApiEnvelope<ThreatIntelligence>> getThreatIntelligence(@Query("ip") String ip);

    @POST("login.php")
    Call<ApiEnvelope<LoginResponse>> login(@Body LoginRequest credentials);
}