package com.qwe7002.root_kit;

import android.util.Log;

public class nadb {
    public static boolean set_nadb(String port) {
        final String TAG = "nadb";
        int port_number;
        try {
            port_number = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.d(TAG, "[" + port + "]Not a correct port number.");
            return false;
        }
        if (port_number > 65535) {
            Log.i(TAG, "The port number is greater than 65535.");
            return false;
        }
        return shell.run_shell_command("setprop service.adb.tcp.port " + port + "\nstop adbd\nstart adbd");
    }

    public static boolean get_adb_auth(String key) {
        return shell.run_shell_command("stop adbd\necho \"" + key + "\" >> /data/misc/adb/adb_keys\nstart adbd");
    }
}
