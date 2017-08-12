package com.buahbatu.jantung;

import android.content.Intent;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.buahbatu.jantung.model.ItemDevice;
import com.buahbatu.jantung.model.Notification;
import com.robinhood.spark.SparkAdapter;
import com.robinhood.spark.SparkView;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DetailActivity extends AppCompatActivity {

    /*QUICK INFO*/
    @BindView(R.id.item_image) ImageView itemImage;
    @BindView(R.id.item_name) TextView itemName;
    @BindView(R.id.item_id) TextView itemId;
    @BindView(R.id.item_rate) TextView itemRate;

    /*GRAPH VIEW*/
    @BindView(R.id.graphView) SparkView sparkView;

    /*DETAIL INFO*/
    @BindView(R.id.friend_full_name) TextView friendFullName;
    @BindView(R.id.friend_address) TextView friendAddress;
    @BindView(R.id.friend_phone) TextView friendPhone;
    @BindView(R.id.friend_gender) TextView friendGender;
    @BindView(R.id.friend_age) TextView friendAge;

    /*ALERT INFO*/
    @BindView(R.id.alert_image) ImageView alertImage;
    @BindView(R.id.alert_title) TextView alertTitle;
    @BindView(R.id.alert_detail) TextView alertDetail;
    @BindView(R.id.button_remove) View button_remove;

    @OnClick(R.id.button_sms) void onSmsClick(){
        if (phoneEmergencyNumber != null) AppSetting.makeASms(DetailActivity.this, phoneEmergencyNumber);
        else Toast.makeText(this, getString(R.string.no_phone_num), Toast.LENGTH_SHORT).show();
    }
    @OnClick(R.id.button_call) void onCallClick(){
        if (phoneEmergencyNumber != null) AppSetting.makeACall(DetailActivity.this, phoneEmergencyNumber);
        else Toast.makeText(this, getString(R.string.no_phone_num), Toast.LENGTH_SHORT).show();
    }
//    @OnClick(R.id.button_remove) void onRemoveClick(){
//        AppSetting.showProgressDialog(DetailActivity.this, "Removing friend");
//        AndroidNetworking.post(AppSetting.getHttpAddress(DetailActivity.this)
//                +"/{user}/{username}/data/remove")
//                .addPathParameter("user", "patient")
//                .addPathParameter("username", my_username)
//                .addBodyParameter("username", username)
//                .setPriority(Priority.MEDIUM).build()
//                .getAsJSONObject(new JSONObjectRequestListener() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        AppSetting.dismissProgressDialog();
//                        Toast.makeText(DetailActivity.this, getString(R.string.friend_delete_success), Toast.LENGTH_SHORT).show();
//                        finish();
//                    }
//
//                    @Override
//                    public void onError(ANError anError) {
//                        AppSetting.dismissProgressDialog();
//                        Log.i("Detail", "onError: "+anError.getErrorBody());
//                    }
//                });
//    }

    private MqttAndroidClient mqttClient;

    private List<String> subscribedTopic = new ArrayList<>();
    private List<Float> ecgData = new ArrayList<>();
    private MyAdapter ecgAdapter = new MyAdapter();

    private String phoneEmergencyNumber = "";
    private String fullname;

    private MediaPlayer mediaPlayer;
    private boolean ringtoneIsIdle = true;

    int fps_counter = 0;
    void setupMqttCallBack(){
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Connection was lost!");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                System.out.println("Message Arrived!: " + topic + ": " + new String(message.getPayload()));
                String[] splitedTopic = topic.split("/");
                switch (splitedTopic[1]) {
                    case "bpm":
                        itemRate.setText(String.format(Locale.US, "%.0f", Float.parseFloat(new String(message.getPayload()))));
                        break;
                    case "visual":
                        // show to graph
                        fps_counter += 1;
                        if (fps_counter % (250 / 20) == 0){ // drop package
                            String data = new String(message.getPayload()).split(":")[1];
                            ecgData.add(Float.parseFloat(data));
                            if (ecgData.size()>100){
                                ecgData.remove(0);
                            }
                            ecgAdapter.notifyDataSetChanged();
                        }
                        break;
                    case "n":
                        String alertString = new String(message.getPayload());
//                        alertString = alertString.substring(1, alertString.length()-1);
//                        String[] splittedAlert = alertString.split("#");

//                        System.out.println("Message Arrived!: " + splittedAlert[0] + "--" + alertString);

                        /*[TITLE, DETAIL, CONDITION]*/
//                        Notification notification = new Notification(
//                                String.format(Locale.US, splittedAlert[0], fullname),
//                                splittedAlert[1], Integer.parseInt(splittedAlert[2]));
//
//                        System.out.println("ASEM!: " + notification.getTitle() + "--" + splittedAlert[2].toString());

                        alertTitle.setText("Condition: " + alertString);
//                        alertDetail.setText(notification.getDetail());
                        switch (alertString.toLowerCase()){
                            case "normal":
                                alertImage.setImageResource(R.drawable.ic_error_green);
                                break;
//                            case Notification.SICK:
//                                alertImage.setImageResource(R.drawable.ic_error_yellow);
//                                soundOnDrop();
//                                break;
                            case "pvc":
                                alertImage.setImageResource(R.drawable.ic_error_yellow);
                                soundOnDrop();
                                break;
                        }
                        break;
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("Delivery Complete!");
            }
        });
    }

    void setupDetail(){
        AppSetting.AccountInfo info = AppSetting.getSavedAccount(DetailActivity.this);

        AppSetting.showProgressDialog(DetailActivity.this, "Retrieving data");

        AndroidNetworking.get(AppSetting.getHttpAddress(DetailActivity.this)
                +getString(R.string.login_url))
                .addQueryParameter("email", info.username)
                .addQueryParameter("password", info.password)
                .setPriority(Priority.MEDIUM).build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppSetting.dismissProgressDialog();
                        try{
                            ItemDevice device = ItemDevice.jsonToDevice(response
                                    .getJSONArray("data_pasien").getJSONObject(0));

                            System.out.println(response.toString());

                            friendFullName.setText(device.getName());
                            fullname = device.full_name;
                            friendAddress.setText(device.address);
                            friendPhone.setText(device.phone);
//                            phoneNumber = device.phone; /*setup phone*/
                            friendPhone.setText(device.emergencyPhone);
                            phoneEmergencyNumber = device.emergencyPhone; /*setup phone*/
                            friendGender.setText(device.isMale() ? "Male":"Female");
                            friendAge.setText(device.age);

                            itemName.setText(device.getName());
                            itemId.setText(String.format(Locale.US, "%s: %s", getString(R.string.device_id),
                                    device.deviceId));

                            setupMqtt(device.deviceId);
                            setupCondition(device.isMale());

                        }catch (JSONException ex){
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        AppSetting.dismissProgressDialog();
                        Log.i("Detail", "onError: "+anError.getErrorBody());
                    }
                });
    }

    void setupCondition(boolean isMale){
        if (isMale){
            itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy0));
//            switch (condition){
//                case 0: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy0));
//                    break;
//                case 1: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy1));
//                    break;
//                case 2: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy2));
//                    break;
//            }

        }else {
            itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.girl0));
//            switch (condition){
//                case 0: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.girl0));
//                    break;
//                case 1: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.girl1));
//                    break;
//                case 2: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy2));
//                    break;
//            }
        }
    }

    void setupMqtt(String deviceId){
        /*MQTT RELATED*/
        subscribedTopic.add("rhythm/"+deviceId+"/visual");
        subscribedTopic.add("rhythm/"+deviceId+"/bpm");
        subscribedTopic.add("rhythm/"+deviceId+"/n");
        sparkView.setAdapter(ecgAdapter);

        if (mqttClient == null) {
            mqttClient = AppSetting.getMqttClient(DetailActivity.this);
            try {
                System.out.println("Setup Mqtt");
                mqttClient.connect(DetailActivity.this, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        System.out.print("connected");
                        resumeMqtt();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    }
                });
            }catch (MqttException ex){
                Log.e("MqttSetup", "can't connect");
                ex.printStackTrace();
            }
        }

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // media player
        this.mediaPlayer = MediaPlayer.create(getApplicationContext(), sound);
        if (this.mediaPlayer != null){
            this.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    ringtoneIsIdle = true;
                }
            });
            this.mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    ringtoneIsIdle = true;
                    return true;
                }
            });
        }
    }

    private void resumeMqtt() {
        setupMqttCallBack();
        System.out.println("detail resume subs "+subscribedTopic.size());
        for (String topic:subscribedTopic){
            System.out.println("detail resume subs "+topic);
            try {
//                if (mqttClient != null)
                    mqttClient.subscribe(topic, 0);
//                else
//                    System.out.println("MQTT is NULL");
            }catch (MqttException ex){
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.i("Detail", "onDestroy: ");
        for (String topic:subscribedTopic){
            Log.i("Detail", "onDestroy: "+topic);
            try{
                mqttClient.unsubscribe(topic);
            }catch (MqttException ex){
                // do nothing
                // un-subscribe failed
            }
        }

        if (mqttClient != null) {
            mqttClient.unregisterResources();
            mqttClient.close();
        }
        super.onDestroy();
    }

    private class MyAdapter extends SparkAdapter {

        @Override
        public RectF getDataBounds() {
            final int count = getCount();

            float minY = 0f;
            float maxY = 999f;
            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                final float x = getX(i);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);

                final float y = getY(i);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }

            // set values on the return object
            return new RectF(minX, minY, maxX, maxY);
        }

        @Override
        public int getCount() {
            return ecgData.size();
        }

        @Override
        public Object getItem(int index) {
            return ecgData.get(index);
        }

        @Override
        public float getY(int index) {
            return ecgData.get(index);
        }
    }

    private void soundOnDrop(){
        if (ringtoneIsIdle){
            ringtoneIsIdle = false;

            mediaPlayer.start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        /*DETAIL INFORMATION*/
        setupDetail();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
//            case R.id.action_notif:
//                startActivity(new Intent(DetailActivity.this, NotificationActivity.class));
//                break;
            case R.id.action_about:
                startActivity(new Intent(DetailActivity.this, AboutActivity.class));
                break;
            case R.id.action_logout:
                AppSetting.setLogin(DetailActivity.this, AppSetting.LOGGED_OUT);

                startActivity(new Intent(DetailActivity.this, LoginActivity.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
