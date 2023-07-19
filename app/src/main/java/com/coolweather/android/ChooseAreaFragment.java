package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
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

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Provice;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

//首先,碎片需要继承Fragment类
public class ChooseAreaFragment extends Fragment {
    // 用于记录当前应该是在哪个阶段,应该展现什么数据
    public static final int LEVEL_PROVICE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    // 碎片中的需要用到的一些实例的定义
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    // 数据容器
    private List<Provice> proviceList;
    private List<City> cityList;
    private List<County> countyList;

    //选中的省份 城市 级别
    private Provice selectedProvince;
    private City selectedCity;
    private int currentLevel;

    /**
     * 在onCreate方法中,我们首先先获得了一些控件的实例,然后去初始化了ArrayAdapter
     * 并将它设置为了ListView的设配器..
     */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.tile_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    /**
     * 在onActivityCreated方法中,给ListView 以及 Button设置了点击事件
     * 初始化工作算是完成了..在该方法的最后,调用了queryProvinces方法,也就是从这里开始加载省级数据的..
     *
     * 当我们点击了某个省的时候会进入到ListView的onItemClick方法中,这个时候回根据当前的级别来判断时调用
     * queryCities方法还是queryCounties方法,这两个方法流程基本和queryProvince相同,这里不再赘述
     *
     * 另外一点还需要注意,在返回按钮的点击事件中,会对点前的ListView的列级进行判断,如果当前是县级列表,
     * 那么返回到市级列表,如果是市级列表就返回到省级列表,在该列表中,按钮被自动隐藏,我们也不用再进行处理
     */

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVICE){
                    selectedProvince = proviceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();    //该处添加了一个if判断,只要当前是county,就启动WeatherActivity
                }else if(currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    //5 这里运用了一个小技巧,instanceof关键字能够判断一个对象是否属于某一个类的实例
                    //5 我们在碎片中调用getActivity方法,然后配合instanceof关键字,就能判断我们当前
                    //5 碎片是位于哪个活动,如果是在Main活动中,则处理逻辑不变
                    //5 如果实在Weather活动中,则最后还需要关闭侧栏,显示下拉刷新进度条,重新申请数据
                    if(getActivity() instanceof MainActivity){  //5
                        Intent intent = new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if (getActivity() instanceof WeatherActivity){ //5
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.close();
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }

                }
            }
        });
        backButton.setOnClickListener((v)->{
            if(currentLevel == LEVEL_COUNTY){
                queryCities();
            }else if(currentLevel == LEVEL_CITY){
                queryProvinces();
            }
        });
        queryProvinces();
    }

    /**
     * queryProvinces方法中,首先将头布局中的标题设置成了中国,将返回按钮隐藏起来,
     * 因为省级列表已经不能再返回了..然后调用LitePal的查询接口来从数据库中读取省
     * 级数据,如果读到了就直接将数据显示在界面上,如果没有读到,就继续调用
     * queryFromServer方法,向服务器请求数据
     */

    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        proviceList = LitePal.findAll(Provice.class);
        if(proviceList.size()>0){
            dataList.clear();
            for(Provice provice : proviceList){
                dataList.add(provice.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel= LEVEL_PROVICE;
        }else{
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel= LEVEL_CITY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+ provinceCode;
            queryFromServer(address,"city");
        }
    }

    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for(County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel= LEVEL_COUNTY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+ provinceCode + "/" +cityCode;
            queryFromServer(address,"county");
        }
    }

    /**
     * queryFromServer方法会调用HttpUtil方法的sendOKHttpRequest方法来向服务器发送请求,响应的数据会回调
     * 到onResponse方法中,然后我在这里去调用Utility的handleProvincesResponse方法来解析和处理服务器返回
     * 的数据,在解析和处理完数据知乎,我们再次调用了queryProvinces方法来重新加载省级数据...由于该方法涉及了
     * UI操作,所以必须要在主线程中进行调用..这里借助了runOnUiThread方法来实现从子线程切换到主线程上...
     * 此时由于数据库已经被写入了数据,所以会直接将数据显示到界面上...
     */

    private void queryFromServer(String address,String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body().string();
                System.out.println("12312313212312");
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressDialog(){


        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }

    }

}
