package com.myweather.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.myweather.android.gson.Forecase;
import com.myweather.android.gson.Weather;
import com.myweather.android.util.HttpUtil;
import com.myweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        mContext = this;

        weatherLayout=findViewById(R.id.sv_weather_layout);
        titleCity=findViewById(R.id.tv_title_city);
        titleUpdateTime=findViewById(R.id.tv_title_update_time);
        degreeText=findViewById(R.id.tv_degree_text);
        weatherInfoText=findViewById(R.id.tv_weather_info);
        forecastLayout=findViewById(R.id.forecast_layout);
        aqiText=findViewById(R.id.tv_aqi_text);
        pm25Text=findViewById(R.id.tv_pm25_text);
        comfortText=findViewById(R.id.tv_comfort_text);
        carWashText=findViewById(R.id.tv_car_wash_text);
        sportText=findViewById(R.id.tv_sport_text);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preferences.getString("weather",null);
        if (weatherString != null){
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        }else {
            //无缓存时去服务器查询天气
            String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
    }

    private void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=762cbdce2e7145fba41eda737b15477a";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"获取天气信息失败！", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(mContext).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else
                            Toast.makeText(mContext,"获取天气信息失败！",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.tmp + "°C";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecase forecase:weather.forecaseList){
            View view = LayoutInflater.from(mContext).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = view.findViewById(R.id.tv_date_text);
            TextView infoText = view.findViewById(R.id.tv_info_text);
            TextView maxText = view.findViewById(R.id.tv_max_text);
            TextView minText = view.findViewById(R.id.tv_min_text);
            dateText.setText(forecase.date);
            infoText.setText(forecase.more.info);
            maxText.setText(forecase.temperature.max);
            minText.setText(forecase.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度："+weather.suggestion.comfort.info;
        String carWash = "洗车指数："+weather.suggestion.carWash.info;
        String sport = "运动建议："+weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }
}
