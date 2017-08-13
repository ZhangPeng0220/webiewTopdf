package com.zhangpeng.pdf;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ProgressDialog progressDialog;
    UserWebView webview;
    File file;
    Bitmap bitmap;
    PopupWindow popupWindow;
    String url = "http://mp.weixin.qq.com/s/ePJ2GnvLLnoBkg5ajbbe6A";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setProgressBarVisibility(true);
        setContentView(R.layout.content_main);
        progressDialog = new ProgressDialog(this,R.style.AppTheme_Dark_Dialog);
        webview = (UserWebView) findViewById(R.id.webview);
        initWeb();
        initPop();
    }
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButton:
                popupWindow.showAsDropDown(v,-200,-1);
                break;
            case R.id.create:
                file = new File(Environment.getExternalStorageDirectory().getPath()+ "/file_name.pdf");
                if(file.exists()&file.length()>0){
                    Toast.makeText(MainActivity.this,"文件已生成",Toast.LENGTH_SHORT).show();
                    popupWindow.dismiss();
                }else{
                    progressDialog.setIndeterminate(true);
                    progressDialog.setMessage("正在生成pdf文件...");
                    progressDialog.show();
                    new android.os.Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    bitmap = Bitmap.createBitmap(webview.getPageWidth(), webview.getPageHeight(), Bitmap.Config.RGB_565);
                                    Canvas canvas = new Canvas(bitmap);
                                    webview.draw(canvas);
                                    cut();
                                    if(file.exists()&file.length()>0){
                                        Toast.makeText(MainActivity.this,"文件已生成",Toast.LENGTH_SHORT).show();
                                    }else{
                                        Toast.makeText(MainActivity.this,"文件生成失败",Toast.LENGTH_SHORT).show();
                                    }
                                    popupWindow.dismiss();
                                    progressDialog.dismiss();
                                }
                            },3000);
                }

                break;
            case R.id.QQ:
                sendtoQQ();
                break;
            case R.id.weixin:
                sendtoWX();
                break;
            case R.id.email:
                sendemail();
                break;
            default:
                break;
        }
    }

    public void initWeb(){
        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }
        });
        webview.getSettings().setJavaScriptEnabled(true);
        if (url.indexOf("http") == -1) {
            url = "http://" + url;
        }
        webview.loadUrl(url);
    }
    public void initPop(){
        popupWindow = new PopupWindow(this);//初始化popwindow
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);//设置宽度
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);//设置高度
        popupWindow.setContentView(LayoutInflater.from(this).inflate(R.layout.popwindow, null));//设置内容
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));//背景颜色
        popupWindow.setOutsideTouchable(false);// setOutsideTouchable则表示PopupWindow内容区域外的区域是否响应点击事件，
        popupWindow.setFocusable(true);//设置焦点
    }
    public void cut(){
        Document document = new Document(PageSize.A4,  0, 0, 0,0);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(Environment.getExternalStorageDirectory().getPath()
                    + "/file_name.pdf"));//通过书写器（Writer）可以将文档写入磁盘中
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        document.open();
        float PDFBitmapRatio = (float) bitmap.getHeight() / (float) bitmap.getWidth();//是否需要分页
        if (PDFBitmapRatio <= 1.4) {///不需要分页

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到stream中
            byte[] byteArray = stream.toByteArray();
            try {
                Image image = Image.getInstance(byteArray);//生成image实例
                image.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());//适应A4纸
                document.add(image);//将图片放入磁盘
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {//需要分页
            float BitmapHeightPerPage = (float) bitmap.getWidth() * 1.4f;//每一页的高度
            int pages = (int) Math.ceil(bitmap.getHeight() / BitmapHeightPerPage);//向上取整
            System.out.println("pages:" + pages);
            Bitmap sub_bitmap;
            for (int i = 0; i < pages; i++) {
                if (i == pages - 1) {//最后一页需要处理一下
                    /**
                     * can not use default setting, or pdf reader cannot read the exported pdf
                     */
                    sub_bitmap = Bitmap.createBitmap(bitmap, 0, (int) BitmapHeightPerPage * i, bitmap.getWidth(), (int) (bitmap.getHeight() - (BitmapHeightPerPage * (pages - 1))));
                    /*
                    第一个参数是数据源，第二个是x偏移量，第三个是y偏移量，第四个是截取宽度，第五个是截取高度
                     */
                } else {
                    sub_bitmap = Bitmap.createBitmap(bitmap, 0, (int) BitmapHeightPerPage * i, bitmap.getWidth(), (int) BitmapHeightPerPage);
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                sub_bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到stream中
                byte[] byteArray = stream.toByteArray();
                try {
                    Image image = Image.getInstance(byteArray);
                    image.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
                    document.add(image);
                } catch (DocumentException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sub_bitmap.recycle();//垃圾回收
            }
        }
        document.close();
    }
   public void sendemail(){
       File file = new File(Environment.getExternalStorageDirectory()+ "/file_name.pdf"); //附件文件地址
       Intent intent = new Intent(Intent.ACTION_SEND,Uri.parse("mailto:test@test.com"));
       intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "FxMarginTrading@feib.com.tw" });
       intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file)); //添加附件，附件为file对象
       intent.setType("application/octet-stream"); //其他的均使用流当做二进制数据来发送
       startActivity(Intent.createChooser(intent, "Select email application.")); //调用系统的mail客户端进行发送

   }
    public void sendtoQQ(){
        Intent share = new Intent(Intent.ACTION_SEND);
        ComponentName component = new ComponentName("com.tencent.mobileqq","com.tencent.mobileqq.activity.JumpActivity");
        share.setComponent(component);
        File file = new File(Environment.getExternalStorageDirectory()+ "/file_name.pdf");
        System.out.println("file " + file.exists());
        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        share.setType("*/*");
        startActivity(share);
    }
    public void sendtoWX(){
        Intent share = new Intent(Intent.ACTION_SEND);
        ComponentName component = new ComponentName("com.tencent.mm","com.tencent.mm.ui.tools.ShareImgUI");
        share.setComponent(component);
        File file = new File(Environment.getExternalStorageDirectory()+ "/file_name.pdf");
        System.out.println("file " + file.exists());
        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        share.setType("*/*");
        startActivity(share);
    }

}
