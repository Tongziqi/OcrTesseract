package cn.example.ocrtesseract.app;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    private EditText editText;
    public Button buttonGetWords;
    public Button buttonOpenPhotos;
    public Button instrument;
    public ImageView imageView;
    // private static final String TESSBASE_PATH = "/mnt/sdcard/tesseract"; 默认语句库
    private static final String DEFAULT_LANGUAGE = "eng";
    //private static final String IMAGE_PATH = "C/mypic.bmp"; //默认路径
    public String IMAGE_PATH;
    boolean isSelectPicture = false;
    SpannableString mSpan = new SpannableString("1"); //控制textview一些属性
    final int IMAGE_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonGetWords = (Button) findViewById(R.id.getWords);
        buttonOpenPhotos = (Button) findViewById(R.id.openPicture);
        instrument = (Button) findViewById(R.id.insruction);
        editText = (EditText) findViewById(R.id.textView1);
        imageView = (ImageView) findViewById(R.id.imageView);
        final String IMAGE_TYPE = "image/*";
        final TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.init("/mnt/sdcard", DEFAULT_LANGUAGE);


        buttonGetWords.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View sourse) {
                if (isSelectPicture) {
                    //设置要ocr的图片bitmap，要解析的图片地址（注意）
                    baseApi.setImage(getDiskBitmap(IMAGE_PATH));
                    //根据Init的语言，获得ocr后的字符串
                    String getString = baseApi.getUTF8Text();
                    editText.setText(getString);
                    //释放bitmap
                    baseApi.clear();
                } else {
                    Toast.makeText(getApplicationContext(), "请选择一张图片", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonOpenPhotos.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getAlbum = new Intent(Intent.ACTION_GET_CONTENT);
                getAlbum.setType(IMAGE_TYPE);
                startActivityForResult(getAlbum, IMAGE_CODE);
                onActivityResult(RESULT_OK, 100, getAlbum);
            }
        });
        instrument.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setTitle("帮助(版权所有BJUT)").setMessage("怎么使用：\n" +
                        "(1)拍照后选择一张照片。\n" + "(2)点击获得文字。\n" + "(3)对了,目前只识别英文。").setNegativeButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        Bitmap bm;
        ContentResolver resolver = getContentResolver(); //外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口
        if (requestCode != RESULT_OK) { //判断是否完成该意图
            Log.e("TAG", "ERROR");
        }
        if (requestCode == 100) {
            try {
                if (data == null) {
                    //Log.e("TAG", "NULL");
                } else {
                    Uri originalUri = data.getData();        //获得图片的uri
                    bm = MediaStore.Images.Media.getBitmap(resolver, originalUri);
                    Bitmap newBitmap = Narrowpicture(bm, 250, 250); //更改bitmap的大小 合适显示
                    imageView.setImageBitmap(newBitmap);

                    //displayBitmapOnText(bm);//将获得的图片添加到EditView里面去 改变方法 添加到ImageView里面
                    String[] proj = {MediaStore.Images.Media.DATA}; //获取图片的路径

                    //多媒体数据库封装接口
                    Cursor cursor = managedQuery(originalUri, proj, null, null, null);
                    //获得用户选择的图片的索引值
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    //将光标移至开头
                    cursor.moveToFirst();
                    //最后根据索引值获取图片路径
                    String path = cursor.getString(columnIndex);
                    // Log.e("path", path);
                    IMAGE_PATH = path;
                    isSelectPicture = true;
                }
            } catch (IOException e) {
                //Log.e("TAG", e.toString());
            }

        }

    }


    private Bitmap getDiskBitmap(String pathString) {
        Bitmap bitmap = null;
        try {
            File file = new File(pathString);
            if (file.exists()) {
                bitmap = BitmapFactory.decodeFile(pathString);
            }
        } catch (Exception e) {
            //
        }
        return bitmap;
    }

    /**
     * 将获得的图片加载到Text里面去  暂时不用该方法
     *
     * @param bitmap 需要加进去的图片
     */
    private void displayBitmapOnText(Bitmap bitmap) {

        if (bitmap == null) {
            return;
        }
        int start = editText.getSelectionStart();
        mSpan.setSpan(new ImageSpan(bitmap), mSpan.length() - 1, mSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (editText != null) {
            Editable et = editText.getText();
            et.insert(start, mSpan);
            editText.setText(et);
            editText.setSelection(start + mSpan.length());
        }
        editText.setLineSpacing(10f, 1f);
    }

    public static Bitmap Narrowpicture(Bitmap bitmap, int screenWidth, int screenHight) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scale = (float) screenWidth / w;
        float scale2 = (float) screenHight / h;
        matrix.postScale(scale, scale2);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        if (bitmap != null && !bitmap.equals(bmp) && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return bmp;
    }
}

