package com.smg.audioeditor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikiller.mkglidelib.imageloader.GlideImageLoader;
import com.smg.audioeditor.R;
import com.smg.audioeditor.base.BaseActivity;
import com.uilib.mxgallery.utils.GalleryMediaUtils;

import java.io.File;
import java.util.List;

import butterknife.BindView;

public class WelcomeActivity extends BaseActivity {
    @BindView(R.id.hello)
    TextView tv_hello;
    private ImageView iv_preview;
    private Button btn_gallery;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

    }

    @Override
    protected void initView() {
        tv_hello.setText(stringFromJNI());


        iv_preview = (ImageView) findViewById(R.id.iv_preview);
        btn_gallery = (Button) findViewById(R.id.btn_gallery);
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, GalleryActivity.class);
                startActivityForResult(intent, 1);
            }
        });
    }
    public native String stringFromJNI();

    @Override
    protected void initData() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK)
            return;
        switch(requestCode){
            case 1:
                File tmpFile = (File) data.getSerializableExtra(GalleryMediaUtils.TMP_FILE);
                List<File> fileList = (List<File>) data.getSerializableExtra(GalleryMediaUtils.THUMB_LIST);
                if(tmpFile != null){
                    GlideImageLoader.getInstance().loadLocalImage(this, GalleryMediaUtils.getInstance().getFileUri(tmpFile),R.mipmap.placeholder, iv_preview);
                }else if(fileList != null && fileList.size() > 0)
                    GlideImageLoader.getInstance().loadLocalImage(this, GalleryMediaUtils.getInstance().getFileUri(fileList.get(0)),R.mipmap.placeholder, iv_preview);
                break;
        }
    }
}
