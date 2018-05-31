package com.uilib.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.uilib.R;

/**
 * Created by Mikiller on 2018/5/1.
 */

public class ToastUtils {

    public static void makeToast(Context context, int resId, int txtId){
        View view = LayoutInflater.from(context).inflate(R.layout.layout_toast, null);
        ((ImageView)view.findViewById(R.id.iv_toast)).setImageResource(resId);
        ((TextView)view.findViewById(R.id.tv_toast)).setText(txtId);
        Toast toast = new Toast(context);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void makeToast(Context context, int txtId){
        makeToast(context, context.getString(txtId));
    }

    public static void makeToast(Context context, String txt){
        Toast.makeText(context, txt, Toast.LENGTH_SHORT).show();
    }
}
