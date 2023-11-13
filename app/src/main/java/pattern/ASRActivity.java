package pattern;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.baidu.aip.asrwakeup3.core.util.AuthUtil;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ForwardScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pattern.strategy.FileRecognitionStrategy;
import pattern.strategy.RecognitionStrategy;
import pattern.strategy.SoundRecognitionStrategy;
import priv.lz.designapplication.ASRSettingActivity;
import priv.lz.designapplication.FileUtil;
import priv.lz.designapplication.R;
/**
 * @author lizan
 */
public class ASRActivity extends AppCompatActivity {
    Button pick_file;
    Button clear_text;
    Button sound_record;
    Button copy_text;
    TextView text_view;

    public static String TAG = "ASR";

    public static final String M4A = "m4a";
    public static final String MP3 = "mp3";

    View.OnClickListener button_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.pick_file:
                    // 选择文件
                    fileLauncher.launch("audio/*");
                    break;
                case R.id.clear_text:
                    // 清空文本
                    text_view.setText("");
                    break;
                case R.id.sound_record:
                    // 录音
                    setRecognitionStrategy(new SoundRecognitionStrategy());
                    startRecognition(asr, params);
                    break;
                case R.id.copy_text:
                    // 复制文本
                    copyStr(text_view.getText().toString());
                    break;
            }
        }
    };

    public void initView() {
        pick_file = findViewById(R.id.pick_file);
        clear_text = findViewById(R.id.clear_text);
        sound_record = findViewById(R.id.sound_record);
        copy_text = findViewById(R.id.copy_text);
        text_view = findViewById(R.id.text_view);

        pick_file.setOnClickListener(button_listener);
        clear_text.setOnClickListener(button_listener);
        sound_record.setOnClickListener(button_listener);
        copy_text.setOnClickListener(button_listener);

        String hintText = "";
        try {
            InputStream is = getResources().openRawResource(R.raw.hint_asr);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            hintText = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        text_view.setHint(hintText);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asractivity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("语音识别");
        actionBar.setDisplayHomeAsUpEnabled(true);

        initView();
        initPermission();
        initAsr();

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
                Intent intent = new Intent(this, ASRSettingActivity.class);
                launcher.launch(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {});


    private void initPermission() {
        ArrayList<String> permissionsList = new ArrayList<>();
        permissionsList.add(Manifest.permission.RECORD_AUDIO);
        permissionsList.add(Manifest.permission.READ_MEDIA_AUDIO);
//        permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

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
                            Toast.makeText(ASRActivity.this, "您拒绝了部分权限，相关功能无法使用", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void copyStr(String copyStr) {
        if (copyStr.equals("")) {
            Toast.makeText(ASRActivity.this, "内容为空", Toast.LENGTH_SHORT).show();
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

    private String parseParams(String  params) {
        String[] paramsArr = params.split("\"");
        return paramsArr[3];
    }

    // ASR 逻辑结构

    EventListener yourListener = new EventListener() {
        @Override
        public void onEvent(String name, String params, byte [] data, int offset, int length) {

            if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)) {
                // 引擎就绪，可以说话，一般在收到此事件后通过UI通知用户可以说话了
                Toast.makeText(ASRActivity.this, "开始识别",  Toast.LENGTH_SHORT).show();
            }
            if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
                // 一句话的临时结果，最终结果及语义结果
                if (params == null || params.isEmpty()) {
                    return;
                }
                if (params.contains("\"partial_result\"")) {
                    // 一句话的临时识别结果
                    text_view.setText(parseParams(params));
                } else if (params.contains("\"final_result\"")) {
                    // 一句话的最终识别结果
                    Toast.makeText(ASRActivity.this, "识别结束", Toast.LENGTH_SHORT).show();
                    text_view.setText(parseParams(params));
                }
            }
        }
    };

    private EventManager asr;
    public Map<String, Object> params = AuthUtil.getParam();

    private void initAsr() {
        asr = EventManagerFactory.create(this, "asr");
        // 基于sdk集成1.3 注册自己的输出事件类
        asr.registerListener(yourListener); //  EventListener 中 onEvent方法

        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(SpeechConstant.PID, 1537); // 中文输入法模型，有逗号
        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800);
    }

    private RecognitionStrategy recognitionStrategy;

    // 在初始化方法中根据用户选择设置识别策略
    private void setRecognitionStrategy(RecognitionStrategy strategy) {
        this.recognitionStrategy = strategy;
    }

    // 响应按钮点击事件，根据用户选择调用相应策略
    private void startRecognition(EventManager asr, Map<String, Object> params) {
        loadParams();
        recognitionStrategy.startRecognition(asr, params);
    }

    // 识别音频文件
    ActivityResultLauncher<String> fileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    Log.i(TAG, "uri: " + uri);
                    cacheFile(uri);
                    setRecognitionStrategy(new FileRecognitionStrategy());
                    startRecognition(asr, params);
                }
            }
    );

    private boolean checkType(Uri uri) {
        DocumentFile file = DocumentFile.fromSingleUri(this, uri);
        String fileName = file.getName();
        String type = fileName.substring(fileName.lastIndexOf(".") + 1);
        Log.i(TAG, "type: " + type);
        return (type.equals(M4A) || type.equals(MP3));
    }

    private void cacheFile(Uri uri) {
        if (!checkType(uri)) {
            Toast.makeText(this, "文件类型不支持", Toast.LENGTH_SHORT).show();
            return;
        }
        // 实现用户选择文件缓存到data目录,并转换pcm格式
        FileUtil.cacheFile(this, uri);
    }

    private void loadParams() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String pid = sp.getString("pid", "1537");
        String vad_endpoint_timeout = sp.getString("vad_endpoint_timeout", "800");
        params.put(SpeechConstant.PID, pid); // 中文输入法模型，有逗号
        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, vad_endpoint_timeout);
        Log.i(TAG, "##############pid: " + params.get(SpeechConstant.PID));
        Log.i(TAG, "##############vad_endpoint_timeout: " + params.get(SpeechConstant.VAD_ENDPOINT_TIMEOUT));
    }

    private void stop() {
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 基于SDK集成4.2 发送取消事件
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);

        // 基于SDK集成5.2 退出事件管理器
        // 必须与registerListener成对出现，否则可能造成内存泄露
        asr.unregisterListener(yourListener);
    }

}
