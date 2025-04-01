package com.tencent.cloud.asr.rn.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.util.LinkedList;
import java.util.List;

public class PermissionsUtils {

  static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

  public static void checkPermissions(Activity activity) {
    List<String> permissions = new LinkedList<>();
    addPermission(activity,permissions, Manifest.permission.RECORD_AUDIO);
    addPermission(activity,permissions, Manifest.permission.INTERNET);

    if (!permissions.isEmpty()) {
      ActivityCompat.requestPermissions(activity, permissions.toArray(new String[permissions.size()]),
        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
    }
  }

  private static void addPermission(Activity activity, List<String> permissionList, String permission) {
    if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(permission);
    }
  }
}
