package ir.mobile369.staff;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    // On WordPress create a page containing [mobile369_pos] with the slug "pos".
    private static final String BASE_URL = "https://mobile369.ir/pos/";
    private static final String HOME_HOST = "mobile369.ir";
    private static final int CAMERA_REQUEST = 100;
    private static final int PICK_FILE_REQUEST = 101;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingCamera;
    private TextView status;

    private boolean trusted(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = uri.getHost();
        return HOME_HOST.equalsIgnoreCase(host) || ("www." + HOME_HOST).equalsIgnoreCase(host);
    }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10,22,48));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setBackgroundColor(Color.rgb(12,30,63));
        status = new TextView(this); status.setText("MOBILE369 • صندوق کارکنان"); status.setTextColor(Color.WHITE); status.setTextSize(14); status.setPadding(14, 12, 4, 12);
        bar.addView(status,new LinearLayout.LayoutParams(0,-2,1));
        addButton(bar,"⟳",()->webView.reload());
        addButton(bar,"چاپ",this::printCurrentPage);
        addButton(bar,"خانه",()->webView.loadUrl(BASE_URL));
        root.addView(bar);
        webView = new WebView(this); root.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false); s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMediaPlaybackRequiresUserGesture(true); s.setSupportZoom(false);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(webView,false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                Uri u=req.getUrl(); if(trusted(u)) return false;
                if ("https".equalsIgnoreCase(u.getScheme()) || "mailto".equalsIgnoreCase(u.getScheme()) || "tel".equalsIgnoreCase(u.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW,u)); } catch(ActivityNotFoundException e) { Toast.makeText(MainActivity.this,"برنامه بازکننده پیدا نشد",Toast.LENGTH_SHORT).show(); }
                }
                return true;
            }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler handler, SslError error) {
                handler.cancel(); status.setText("خطای گواهی امنیتی سایت");
            }
            @Override public void onPageFinished(WebView v,String url) { status.setText("MOBILE369 • صندوق کارکنان"); }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(()->{
                    if (!trusted(request.getOrigin())) { request.deny(); return; }
                    boolean video=false; for(String item:request.getResources()) if(PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(item)) video=true;
                    if(!video || request.getResources().length!=1){ request.deny(); return; }
                    if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    else { pendingCamera=request; requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST); }
                });
            }
            @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> callback,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null); fileCallback=callback;
                Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/json","text/csv"});
                try { startActivityForResult(intent,PICK_FILE_REQUEST); } catch(Exception e){ fileCallback.onReceiveValue(null);fileCallback=null; }
                return true;
            }
        });
        if(savedInstanceState==null) webView.loadUrl(BASE_URL); else webView.restoreState(savedInstanceState);
    }
    private void addButton(LinearLayout bar,String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(32,82,164)));
        b.setOnClickListener(v->action.run());bar.addView(b,new LinearLayout.LayoutParams(-2,54));
    }
    private void printCurrentPage(){
        PrintManager manager=(PrintManager)getSystemService(Context.PRINT_SERVICE);
        if(manager!=null)manager.print("Mobile369-POS",webView.createPrintDocumentAdapter("Mobile369"),new PrintAttributes.Builder().build());
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==CAMERA_REQUEST && pendingCamera!=null){
            if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)pendingCamera.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            else pendingCamera.deny();pendingCamera=null;
        }
    }
    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);
        if(code==PICK_FILE_REQUEST&&fileCallback!=null){
            Uri[] files=result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null;
            fileCallback.onReceiveValue(files);fileCallback=null;
        }
    }
    @Override public void onSaveInstanceState(Bundle state){ webView.saveState(state);super.onSaveInstanceState(state); }
    @Override public void onBackPressed(){ if(webView.canGoBack())webView.goBack();else new AlertDialog.Builder(this).setMessage("از صندوق خارج شوید؟").setNegativeButton("لغو",null).setPositiveButton("خروج",(d,w)->finish()).show(); }
    @Override public void onDestroy(){if(pendingCamera!=null){pendingCamera.deny();pendingCamera=null;}if(webView!=null){webView.destroy();}super.onDestroy();}
}
