package com.saadiftkhar.scanidcard;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.saadiftkhar.scanidcard.camera.ScanIDCard;
import com.saadiftkhar.scanidcard.utils.FileUtils;

public class MainActivity extends AppCompatActivity {
    private ImageView mIvFront;
    private ImageView mIvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvFront = (ImageView) findViewById(R.id.iv_front);
        mIvBack = (ImageView) findViewById(R.id.iv_back);
    }

    public void front(View view) {
        ScanIDCard.create(this).openCamera(ScanIDCard.TYPE_IDCARD_FRONT);
    }


    public void back(View view) {
        ScanIDCard.create(this).openCamera(ScanIDCard.TYPE_IDCARD_BACK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == ScanIDCard.RESULT_CODE) {

            final String path = ScanIDCard.getImagePath(data);
            if (!TextUtils.isEmpty(path)) {
                if (requestCode == ScanIDCard.TYPE_IDCARD_FRONT) { //身份证正面
                    mIvFront.setImageBitmap(BitmapFactory.decodeFile(path));
                } else if (requestCode == ScanIDCard.TYPE_IDCARD_BACK) {  //身份证反面
                    mIvBack.setImageBitmap(BitmapFactory.decodeFile(path));
                }

                FileUtils.clearCache(this);
            }
        }
    }
}
