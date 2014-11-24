package cn.example.ocrtesseract.app;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    private EditText editText;
    public Button buttonGetWords;
    public Button buttonOpenPhotos;
    public Button instrument;
    public ImageView imageView;
    private static final String TESSBASE_PATH = "/mnt/sdcard"; //默认语句库
    private static final String DEFAULT_LANGUAGE = "chi_sim";
    public String IMAGE_PATH;
    boolean isSelectPicture = false;
    private static final int IMAGE_CODE = 100;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    Uri fileUri = null;

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
        baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);//这个是在真机上的地址 baseApi.init("/storage/emulated/0/", "eng");  //模拟器太变态了

        buttonGetWords.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View sourse) {
                if (isSelectPicture) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("需要很长时间呢~").setPositiveButton("我不怕",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //设置要ocr的图片bitmap，要解析的图片地址（注意）
                                    baseApi.setImage(getDiskBitmap(IMAGE_PATH));
                                    //根据Init的语言，获得ocr后的字符串
                                     String getString = baseApi.getUTF8Text();
                                    editText.setText(getString);
                                    //释放bitmap
                                    baseApi.clear();
                                }
                            }).setNegativeButton("那算了吧,再见", null).show();
                } else {
                    Toast.makeText(getApplicationContext(), "首先，你要选择一张图片", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonOpenPhotos.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setTitle("请选择从哪儿打开").setNeutralButton("图库", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent getAlbum = new Intent(Intent.ACTION_GET_CONTENT);
                        getAlbum.setType(IMAGE_TYPE);
                        startActivityForResult(getAlbum, IMAGE_CODE);
                        //onActivityResult(RESULT_OK, 100, getAlbum);
                    }
                }).setPositiveButton("照相机", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // create a file to save the image
                        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                        // 此处这句intent的值设置关系到后面的onActivityResult中会进入那个分支，即关系到data是否为null，如果此处指定，则后来的data为null
                        // set the image file name
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                    }
                }).show();
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
        if (requestCode == IMAGE_CODE) {
            try {
                if (data == null) {
                    Toast.makeText(MainActivity.this, "没有获得数据", Toast.LENGTH_LONG).show();
                    //Log.e("TAG", "NULL");
                } else {
                    Uri originalUri = data.getData();        //获得图片的uri
                    bm = MediaStore.Images.Media.getBitmap(resolver, originalUri);
                    Bitmap newBitmap = Narrowpicture(bm, 250, 250); //更改bitmap的大小 合适显示
                    imageView.setImageBitmap(Tools.bitmap2Gray(newBitmap));
                    //displayBitmapOnText(bm);//将获得的图片添加到EditView里面去 改变方法 添加到ImageView里面
                    String[] proj = {MediaStore.Images.Media.DATA}; //获取图片的路径

                    //多媒体数据库封装接口
                    Cursor cursor = getContentResolver().query(originalUri, proj, null, null, null);
                    //获得用户选择的图片的索引值
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    //将光标移至开头
                    cursor.moveToFirst();
                    //最后根据索引值获取图片路径
                    String path = cursor.getString(columnIndex);
                    Log.e("path", path);
                    IMAGE_PATH = path;
                    isSelectPicture = true;
                }
            } catch (IOException e) {
                //Log.e("TAG", e.toString());
            }

        }
        if (CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE == requestCode) {

            if (data != null) {
                //检查是否照片存到内置图库
                Toast.makeText(MainActivity.this, "没有获得数据", Toast.LENGTH_LONG).show();
                if (data.hasExtra("data")) {
                    Bitmap thumbnail = data.getParcelableExtra("data");
                    imageView.setImageBitmap(thumbnail);
                }
            } else {
                //存到自己新建图库 然后调整大小
                BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
                factoryOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(fileUri.getPath(), factoryOptions);
                factoryOptions.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(), factoryOptions);
                String photoPath = String.valueOf(fileUri);
                String photoDeletePath = "file://";
                IMAGE_PATH = photoPath.replaceAll(photoDeletePath, "");
                Log.e("path", String.valueOf(IMAGE_PATH));
                Bitmap newBitmap = Narrowpicture(bitmap, 250, 250); //更改bitmap的大小 合适显示
                imageView.setImageBitmap(newBitmap);
                isSelectPicture = true;
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
            Log.e("Exception", String.valueOf(e));
        }
        return bitmap;
    }

    public static Bitmap Narrowpicture(Bitmap bitmap, int screenWidth, int screenHight) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scale = (float) screenWidth / w;
        float scale2 = (float) screenHight / h;
        matrix.postScale(scale, scale2);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        if (!bitmap.equals(bmp) && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return bmp;
    }

    /**
     * Create a file Uri for saving an image
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = null;
        try {
            mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create the storage directory if it does not exist
        assert mediaStorageDir != null;
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }
        return mediaFile;
    }
}





