package com.farahmand.duet;

import android.content.Context;
import android.widget.Toast;

public class Helper {
    public static void toast(Context context,String message){
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
    }
}
