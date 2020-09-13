package com.example.coolweather.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.R;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.service.AutoUpdadteService;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    //设置切换城市视图,切换城市按钮navButton
    public DrawerLayout drawerLayout;
    private Button navButton;
    //设置下拉刷新列表视图
    public SwipeRefreshLayout swipeRefreshLayout;
    //用于保存缓存和选中的weatherid
    private  String mWeatherId;
    //定义控件
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private LinearLayout forecastLayout;

    //设置可变的背景图片
    private ImageView bingPicImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //将背景图和标题栏融合在一起
        if (Build.VERSION.SDK_INT>=21){
            View decorView =getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化控件
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        //为下拉视图设置绑定，设置下拉进度条颜色
        swipeRefreshLayout =findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        //为切换城市视图和按钮绑定
        drawerLayout=findViewById(R.id.drawer_layout);
        navButton =findViewById(R.id.nav_button);
        //使用sharepreferences来存储缓存
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString =prefs.getString("weather",null);
        if (weatherString!=null){
            //有缓冲时，直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            //保存缓存中的城市信息，用于为下面下拉刷新时提供weatherid数据
            mWeatherId =weather.basic.weatherId;
            //展示weather中的数据
            showWeatherInfo(weather);
        }else {
            //无缓存时去服务器查询天气
            //通过intent传过来的数据来确定weatherid就是城市id
            mWeatherId =getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        //为切换城市按钮设置监听器
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打开切换的滑动菜单
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        //为下拉刷新设置监听器
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
        //初始化可变背景图片
        bingPicImg=findViewById(R.id.bing_pic_img);
        String bingPic =prefs.getString("bing_pic",null);
        if (bingPic!=null){
            //通过Glid方法解析图片链接，并将其加载到图片上
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            //加载可变图片
            loadBingPic();
        }
    }



    /**
     * 根据天气id请求城市天气信息
     * */
    public void requestWeather(String weatherId) {
        String weatherUrl ="http://guolin.tech/api/weather?cityid="+weatherId+
                "&key= a66c1f9d443446609d53b0b99bbfa072";
        System.out.println("经访问后网址："+weatherUrl);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.out.println("失败后的错误信息："+e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,
                                "获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);

                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                //注意：下面不能写，tostirng，不然会出现获取失败
                //String()属于强制转换， null转换的结果为null；undefined转换的结果为undefined；
                // 其余的如果有toString()方法，即调用该方法，返回相应的结果；
                final String responseText =response.body().string();
                final Weather weather =Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather!=null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor =PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("Weather",responseText);
                            editor.apply();
                            //每次查询都保存当前查询的weatherid
                            mWeatherId =weather.basic.weatherId;
                            showWeatherInfo(weather);
                        }else {
                            System.out.println("请求成功但是数据为空或是显示数据状态有问题");
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                        //用于表示刷新时间结束，并隐藏刷新进度条
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        //加载可变背景图片
        loadBingPic();
    }


    /**
     * 加载每日一图
     * */
    private void loadBingPic() {
        String requestBingPic ="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.
                        getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });

    }



    /**
     * 处理并展示Weather中的数据
     * */
    public void showWeatherInfo(Weather weather) {

        System.out.println("显示时的weather对象的信息展示："+weather);
        String cityName = weather.basic.cityName;
        //spilt是将其分隔开，括号里是用什么分开
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "°C";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        //把原先的加入主视图中的子视图给清除
        forecastLayout.removeAllViews();
        //把weather中预告信息遍历，在一个个的添加到预告（forecast）视图中
        for (Forecast forecast : weather.forecastList) {
            //将R.layout.forecast_item资源实例化到forecastLayout主视图中
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item
                    ,forecastLayout ,false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);


            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }

        if (weather.aqi != null) {

            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度： "+weather.suggestion.comfort.info;
        String carWash = "洗车指数："+weather.suggestion.carWash.info;
        String sport = "运动建议："+weather.suggestion.sport.info;

        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        //激活自动更新服务
        Intent intent =new Intent(this, AutoUpdadteService.class);
        startService(intent);
        System.out.println("成功开启服务");
    }
}