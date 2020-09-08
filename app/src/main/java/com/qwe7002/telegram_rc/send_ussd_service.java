package com.qwe7002.telegram_rc;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.paperdb.Paper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.O)
public class send_ussd_service extends Service {
    private static Map<Character, Integer> getNineKeyMap() {
        return new HashMap<Character, Integer>() {
            {
                put('A', 2);
                put('B', 2);
                put('C', 2);
                put('D', 3);
                put('E', 3);
                put('F', 3);
                put('G', 4);
                put('H', 4);
                put('I', 4);
                put('J', 5);
                put('K', 5);
                put('L', 5);
                put('M', 6);
                put('N', 6);
                put('O', 6);
                put('P', 7);
                put('Q', 7);
                put('R', 7);
                put('S', 7);
                put('T', 8);
                put('U', 8);
                put('V', 8);
                put('W', 9);
                put('X', 9);
                put('Y', 9);
                put('Z', 9);
            }
        };
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(@NotNull Intent intent, int flags, int startId) {
        String TAG = "send_ussd_service";
        Context context = getApplicationContext();
        Paper.init(context);
        String notification_name = context.getString(R.string.ussd_code_running);
        Notification.Builder notification;
        NotificationChannel channel = new NotificationChannel(notification_name, notification_name,
                NotificationManager.IMPORTANCE_MIN);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);
        notification = new Notification.Builder(context, notification_name).setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_stat)
                .setOngoing(true)
                .setTicker(context.getString(R.string.app_name))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(notification_name);
        startForeground(public_func.SEND_USSD_SERVCE_NOTIFY_ID, notification.build());

        Handler handler = new Handler();
        String ussd = intent.getStringExtra("ussd");
        int sub_id = intent.getIntExtra("sub_id", -1);

        assert ussd != null;
        ussd = ussd.toUpperCase();
        StringBuilder ussd_sb = new StringBuilder();
        final Map<Character, Integer> nine_key_map = getNineKeyMap();
        char[] ussd_char_array = ussd.toCharArray();
        for (char c : ussd_char_array) {
            if (Character.isUpperCase(c)) {
                ussd_sb.append(nine_key_map.get(c));
            } else {
                ussd_sb.append(c);
            }
        }
        ussd = ussd_sb.toString();
        Log.d(TAG, "ussd: " + ussd);
        Log.d(TAG, "subid: " + sub_id);

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        assert telephonyManager != null;

        if (sub_id != -1) {
            telephonyManager = telephonyManager.createForSubscriptionId(sub_id);
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences("data", MODE_PRIVATE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "send_ussd: No permission.");
        }

        String bot_token = sharedPreferences.getString("bot_token", "");
        String chat_id = sharedPreferences.getString("chat_id", "");
        String request_uri = public_func.get_url(bot_token, "sendMessage");
        message_json request_body = new message_json();
        request_body.chat_id = chat_id;
        request_body.text = context.getString(R.string.send_ussd_head) + "\n" + context.getString(R.string.ussd_code_running);
        String request_body_raw = new Gson().toJson(request_body);
        RequestBody body = RequestBody.create(request_body_raw, public_func.JSON);
        OkHttpClient okhttp_client = public_func.get_okhttp_obj(sharedPreferences.getBoolean("doh_switch", true), Paper.book().read("proxy_config", new proxy_config()));
        Request request = new Request.Builder().url(request_uri).method("POST", body).build();
        Call call = okhttp_client.newCall(request);
        String finalUssd = ussd;
        TelephonyManager finalTelephonyManager = telephonyManager;
        new Thread(() -> {
            String message_id_string = "-1";
            try {
                Response response = call.execute();
                message_id_string = public_func.get_message_id(Objects.requireNonNull(response.body()).string());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                finalTelephonyManager.sendUssdRequest(finalUssd, new ussd_request_callback(context, sharedPreferences, Long.parseLong(message_id_string)), handler);
            }
            stopSelf();
        }).start();


        return START_NOT_STICKY;
    }
}
