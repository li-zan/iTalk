package priv.lz.designapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralBasicParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.baidu.ocr.ui.camera.CameraActivity;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ForwardScope;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
/**
 * @author lizan
 */
public class OCRActivity extends AppCompatActivity {

    ImageView image_view;
    EditText edit_text;
    Button copy_text;
    Button clear_text;
    Button reset_all;

    private static final String TAG = "OCR";

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.image_view:
                    ui();
                    break;
                case R.id.copy_text:
                    copy();
                    break;
                case R.id.clear_text:
                    clear();
                    break;
                case R.id.reset_all:
                    reset();
                    break;
            }
        }
    };

    private void initView() {
        image_view = findViewById(R.id.image_view);
        edit_text = findViewById(R.id.edit_text);
        copy_text = findViewById(R.id.copy_text);
        clear_text = findViewById(R.id.clear_text);
        reset_all = findViewById(R.id.reset_all);

        copy_text.setOnClickListener(listener);
        clear_text.setOnClickListener(listener);
        reset_all.setOnClickListener(listener);
        image_view.setOnClickListener(listener);

        String hintText = "";
        try {
            InputStream is = getResources().openRawResource(R.raw.hint_ocr);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            hintText = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        edit_text.setHint(hintText);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocractivity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("文字识别");
        actionBar.setDisplayHomeAsUpEnabled(true);

        initAccessTokenLicenseFile();
        initView();
        initPermission();

    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate( R.menu.main_menu, menu );
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.setting:  //设置页
                Intent intent = new Intent(this, OCRSettingActivity.class);
                launcher.launch(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {});

    // TTS逻辑
    private static final int REQUEST_CODE_GENERAL_BASIC = 106;
    public void ui() {
        Intent intent = new Intent(OCRActivity.this, CameraActivity.class);
        intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
                FileUtil.getSaveFile(getApplicationContext()).getAbsolutePath());
        intent.putExtra(CameraActivity.KEY_CONTENT_TYPE,
                CameraActivity.CONTENT_TYPE_GENERAL);
        startActivityForResult(intent, REQUEST_CODE_GENERAL_BASIC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 获取调用参数
//        String contentType = data.getStringExtra(CameraActivity.KEY_CONTENT_TYPE);
        // 通过临时文件获取拍摄的图片
        String filePath = FileUtil.getSaveFile(getApplicationContext()).getAbsolutePath();
        if (requestCode == REQUEST_CODE_GENERAL_BASIC && resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            image_view.setImageBitmap(bitmap);
            // 通用文字识别参数设置
            GeneralBasicParams param = new GeneralBasicParams();
            param.setDetectDirection(true);
            param.setImageFile(new File(filePath));
            loadParams(param);
            Log.i(TAG, "param: " +  param.getStringParams());
            // 调用通用文字识别服务
            OCR.getInstance(OCRActivity.this).recognizeGeneralBasic(param, new OnResultListener<GeneralResult>() {
                @Override
                public void onResult(GeneralResult result) {
                    // 调用成功，返回GeneralResult对象
                    StringBuilder sb = new StringBuilder();
                    for (WordSimple wordSimple : result.getWordList()) {
                        // wordSimple不包含位置信息
                        WordSimple word = wordSimple;
                        sb.append(word.getWords());
                        sb.append("\n");
                    }
                    // json格式返回字符串
//                    listener.onResult(result.getJsonRes());
                    Log.w(TAG, "onResult: \n" + sb.toString());
                    edit_text.setText(sb.toString());
                }
                @Override
                public void onError(OCRError error) {
                    // 调用失败，返回OCRError对象
                    edit_text.setText("识别失败！");
                }
            });
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initAccessTokenLicenseFile() {
        OCR.getInstance(getApplicationContext()).initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                String token = accessToken.getAccessToken();
                Log.i(TAG, "鉴权成功 ");
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
                Log.i(TAG, "licens文件鉴权失败："+error.getMessage());
            }
        }, "aip.license", getApplicationContext());
    }

    private void loadParams(GeneralBasicParams params) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String language_type = sp.getString("language_type", "CHN_ENG");
        boolean isAutoDetect = sp.getBoolean("isAutoDetect", false);
        params.setDetectLanguage(isAutoDetect);
        params.setLanguageType(language_type);
    }

    private void initPermission() {
        ArrayList<String> permissionsList = new ArrayList<>();
        permissionsList.add(Manifest.permission.CAMERA);
        permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);

        PermissionX.init(this)
                .permissions(permissionsList)
                .onForwardToSettings(new ForwardToSettingsCallback() {
                    @Override
                    public void onForwardToSettings(ForwardScope scope, List<String> deniedList) {
                        scope.showForwardToSettingsDialog(deniedList, "您需要去应用程序设置当中手动开启权限", "我已明白","取消");
                    }
                })
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                        if (allGranted) {
                            // 所有权限已通过
                        } else {
                            Toast.makeText(OCRActivity.this, "您拒绝了部分权限，相关功能无法使用", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    // sdk调用，无ui模块封装
    public void test() {
        GeneralBasicParams param = new GeneralBasicParams();
        param.setDetectDirection(true);
        param.setImageFile(new File("/storage/emulated/0/Android/data/priv.lz.designapplication/files/TTS/img.png"));

        // 调用通用文字识别服务
        OCR.getInstance(this).recognizeGeneralBasic(param, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult result) {
                // 调用成功，返回GeneralResult对象
                StringBuilder sb = new StringBuilder();
                for (WordSimple wordSimple : result.getWordList()) {
                    // wordSimple不包含位置信息
                    WordSimple word = wordSimple;
                    sb.append(word.getWords());
                    sb.append("\n");
                }
                // json格式返回字符串
//                listener.onResult(result.getJsonRes());
                Log.w(TAG, "onResult: \n" + sb.toString());
            }
            @Override
            public void onError(OCRError error) {
                // 调用失败，返回OCRError对象
            }
        });
    }

    private void copy() {
        String copyStr = edit_text.getText().toString();
        if (copyStr.equals("")) {
            Toast.makeText(this, "内容为空", Toast.LENGTH_SHORT).show();
            return;
        }
        //获取剪贴板管理器
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("Label", copyStr);
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
        Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show();
    }

    private void clear() {
        edit_text.setText("");
    }

    private void reset() {
        clear();
        image_view.setImageResource(R.drawable.default_img);
    }
}