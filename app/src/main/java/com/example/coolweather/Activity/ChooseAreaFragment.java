package com.example.coolweather.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.coolweather.R;
import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.widget.AdapterView.*;

public class ChooseAreaFragment extends Fragment {
    public static  final int LEVEL_PROVINCE =0;
    public static  final int LEVEL_CITY =1;
    public static  final int LEVEL_COUNTY =2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    //数组适配器
    private ArrayAdapter<String>  adapter;
    private List<String> dataList = new ArrayList<>();
    //省列表
    private  List<Province> provinceList;
    //事列表
    private  List<City> cityList;
    //县列表
    private  List<County> countyList;
    //选中的省份
    private  Province selectedProvince;
    //选中的市
    private  City selectedCity;
    //选中的级别
    private  int  currentLevel;


    //onCreate是指创建该fragment类似于Activity.onCreate，你可以在其中初始化除了view之外的东西
    //onCreateView是创建该fragment对应的视图，你必须在这里创建自己的视图并返回给调用者.
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.choose_area,container,false);
        titleText =view.findViewById(R.id.title_text);
        backButton=view.findViewById(R.id.back_button);
        listView =view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel==LEVEL_PROVINCE){
                    selectedProvince =provinceList.get(position);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }else if (currentLevel ==LEVEL_COUNTY){
                    String weatherId =countyList.get(position).getWeatherId();
                    //如果是碎片是在MainActivity中，就正常intent过去
                    if (getActivity()instanceof MainActivity){
                        Intent intent =new Intent(getActivity(),WeatherActivity.class);
                        //把选中的当前城市的id通过intent传送过去
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }//instanceof 可以判断对象是否来自哪个类的实例
                    else if (getActivity()instanceof WeatherActivity){
                        //如果是天气菜单来的，就关闭活动菜单，显示下拉刷新进度条，请求新城市信息
                        WeatherActivity activity= (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeather(weatherId);

                    }

                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库中查找，如果没有查到再去服务器中查
     */
    private void queryProvinces() {
        titleText.setText("中国");
        //按钮设置为不可见
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询全国所有的市，优先从数据库中查找，如果没有查到再去服务器中查
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        //按钮设置为可见
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid =?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            try {
                dataList.clear();
                for (City city : cityList) {
                    dataList.add(city.getCityName());
                }
                //adapter.notifyDataSetChanged()通过一个外部的方法控制如果适配器的内容改变时需要强制调
                // 用getView来刷新每个Item的内容,可以实现动态的刷新列表的功能。
                adapter.notifyDataSetChanged();
                //表示将列表移动到指bai定的括号中的位置处，此处为定位到0位置上
                listView.setSelection(0);
                titleText.setText(selectedProvince.getProvinceName());
                currentLevel = LEVEL_CITY;
                /**
                 * 书上没有的优化部分
                 * */
            }catch (NullPointerException e){
                System.out.println("出现空指针异常123:  "+e);
                String url ="http://guolin.tech/api/china";
                queryFromServer(url,"province");
                int provinceCode =selectedProvince.getProvinceCode();
                url =url +provinceCode;
                queryFromServer(url,"city");
            }

        } else {
            int provinceCode =selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询选中的市内所有的县，优先从数据库中查，如果没有从服务器中查
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid =?",String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode =selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
        private void queryFromServer(String address, final String type){
            showProgressDialog();
            System.out.println("在展示进度条下面");
            HttpUtil.sendOkHttpRequest(address, new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    System.out.println("从服务器上调取的数据返回信息："+responseText);
                    boolean result = false;
                    if ("province".equals(type)) {
                        result = Utility.handleProvinceResponse(responseText);
                    } else if ("city".equals(type)) {
                        result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                    } else if ("county".equals(type)) {
                        result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                    }
                    System.out.println("解析后的数据:"+result);
                    if (result) {
                        // ͨ��runOnUiThread()�����ص����̴߳����߼�
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeProgressDialog();
                                if ("province".equals(type)) {
                                    queryProvinces();
                                } else if ("city".equals(type)) {
                                    queryCities();
                                } else if ("county".equals(type)) {
                                    queryCounties();
                                }
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    // 通过runOnUiThread()方法回到主线程处理逻辑
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            Toast.makeText(getContext(),
                                    "加载失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }

    /**
     * 显示对话框进度
     */
        private void showProgressDialog(){
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setMessage("正在加载");
                progressDialog.setCanceledOnTouchOutside(false);
            }
            progressDialog.show();
        }


        /**
         *关闭对话框进度
         */
        private void closeProgressDialog(){
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

}