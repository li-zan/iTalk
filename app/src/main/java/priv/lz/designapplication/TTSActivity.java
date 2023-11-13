package priv.lz.designapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.baidu.tts.sample.control.InitConfig;
import com.baidu.tts.sample.listener.FileSaveListener;
import com.baidu.tts.sample.listener.UiMessageListener;
import com.baidu.tts.sample.util.Auth;
import com.baidu.tts.sample.util.AutoCheck;
import com.baidu.tts.sample.util.FileUtil;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ForwardScope;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @author lizan
 */
public class TTSActivity extends AppCompatActivity {
    Button pick_file;
    Button clear_text;
    Button play_button;
    Button cancel_button;
    EditText edit_text;

    public static String TAG = "TTS";

    public static final int TXT = 1;
    public static final int DOC = 2;
    public static final int DOCX = 3;

    View.OnClickListener button_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.pick_file:
                    // 选择文件
//                    fileLauncher.launch("application/msword");
                    fileLauncher.launch("text/plain");
                    break;
                case R.id.clear_text:
                    // 清空文本
                    edit_text.setText("");
                    break;
                case R.id.play_button:
                    // 播放
                    speak();
                    break;
                case R.id.cancel_button:
                    // 取消
                    stop();
                    break;
            }
        }
    };

    ActivityResultLauncher<String> fileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    int type = checkType(uri);
                    if (type == -1) {
                        Toast.makeText(TTSActivity.this, "文件类型不支持", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    parseFile(uri,  type);
                }
            }
    );


    public int checkType(Uri uri) {
        DocumentFile file = DocumentFile.fromSingleUri(TTSActivity.this, uri);
        String fileName = file.getName();
        String type = fileName.substring(fileName.lastIndexOf(".") + 1);
        Log.i(TAG, "type: " + type);
        if (type.equals("txt")) {
            return TXT;
        } else if (type.equals("doc")) {
            return DOC;
        } else if (type.equals("docx")) {
            return DOCX;
        }
        return -1;
    }

    public void initView() {
        pick_file = findViewById(R.id.pick_file);
        clear_text = findViewById(R.id.clear_text);
        play_button = findViewById(R.id.play_button);
        cancel_button = findViewById(R.id.cancel_button);
        edit_text = findViewById(R.id.edit_text);

        pick_file.setOnClickListener(button_listener);
        clear_text.setOnClickListener(button_listener);
        play_button.setOnClickListener(button_listener);
        cancel_button.setOnClickListener(button_listener);

        String hintText = "";
        try {
            InputStream is = getResources().openRawResource(R.raw.hint_tts);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            hintText = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        edit_text.setHint(hintText);

        mainHandler = new Handler() {
            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.obj != null) {
                    Log.i(TAG, "handleMessage: "+msg.obj.toString());
                }
            }

        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ttsactivity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("语音合成");
        actionBar.setDisplayHomeAsUpEnabled(true);

        initView();
        initPermission();
        initTTS();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                Intent intent = new Intent(this, TTSSettingActivity.class);
                launcher.launch(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {});


    private void initPermission() {
        ArrayList<String> permissionsList = new ArrayList<>();
//        permissionsList.add(Manifest.permission.READ_MEDIA_AUDIO);

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
                            Toast.makeText(TTSActivity.this, "您拒绝了部分权限，相关功能无法使用", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //TTS引擎
    protected String appId;
    protected String appKey;
    protected String secretKey;
    protected SpeechSynthesizer mSpeechSynthesizer;
    protected Handler mainHandler;
    SpeechSynthesizerListener listener_noSave;
    SpeechSynthesizerListener listener_save;
    private String voice;
    private boolean isSave;

    private void initTTS() {
        appId = Auth.getInstance(this).getAppId();
        appKey = Auth.getInstance(this).getAppKey();
        secretKey = Auth.getInstance(this).getSecretKey();

        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(this);
        // 存储目录
        String tmpDir = FileUtil.createTmpDir(this);
        Log.w(TAG, "Dir: " +  tmpDir);

        listener_noSave = new UiMessageListener(mainHandler);
        listener_save = new FileSaveListener(mainHandler, tmpDir);

        mSpeechSynthesizer.setSpeechSynthesizerListener(listener_noSave);
        mSpeechSynthesizer.setAppId(appId);
        mSpeechSynthesizer.setApiKey(appKey, secretKey);

        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声  3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置合成的音量，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");


        mSpeechSynthesizer.initTts(TtsMode.ONLINE);
    }

    private void speak() {

        loadParams();

        if (mSpeechSynthesizer == null) {
            return;
        }
        if (isSave) {mSpeechSynthesizer.setSpeechSynthesizerListener(listener_save);}
        else {mSpeechSynthesizer.setSpeechSynthesizerListener(listener_noSave);}

        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, voice);
        String text = edit_text.getText().toString();
        if (text.equals("")) {
            Toast.makeText(this, "请输入要合成的文本", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "开始合成", Toast.LENGTH_SHORT).show();
        mSpeechSynthesizer.speak(text);
        if (isSave) {
            new AlertDialog.Builder(TTSActivity.this)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle("合成完毕")
                    .setMessage("自定义位置保存音频")
                    .setPositiveButton("选择位置",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveFile();
                        }
                    })
                    .setNegativeButton("取消保存", null)
                    .show();
        }

    }

    private void saveFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/mpeg"); //.mp3 MIME类型
        String tempFileName = priv.lz.designapplication.FileUtil.getTempFileName();
        intent.putExtra(Intent.EXTRA_TITLE, tempFileName);
        savaFileLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> savaFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    String PCMPath = "/storage/emulated/0/Android/data/priv.lz.designapplication/files/TTS/output.pcm";
                    String MP3Path = priv.lz.designapplication.FileUtil.convertPCMToMP3InDataFile(PCMPath);
                    // 获取返回的数据
                    Intent data = result.getData();
                    Uri uri = data.getData();
                    priv.lz.designapplication.FileUtil.dataFileCopyToPublicStorage(
                            TTSActivity.this, MP3Path, uri);
                    String dirName = priv.lz.designapplication.FileUtil
                                    .getDirName(TTSActivity.this, uri);
                    Toast.makeText(TTSActivity.this, "已保存至：" + dirName, Toast.LENGTH_LONG).show();
                }
            }
    );


    private void loadParams() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        voice = sp.getString("voice", "0");
        isSave = sp.getBoolean("isSave", false);
        Log.i(TAG, "##############sharedpreferences voice: " +  voice);
        Log.i(TAG, "##############sharedpreferences isSave: " +  isSave);
    }


    private void stop() {
        mSpeechSynthesizer.stop();
        Toast.makeText(this, "停止合成引擎", Toast.LENGTH_SHORT).show();
    }

    public void parseFile(Uri uri, int type) {
        Toast.makeText(this, "正在解析文档", Toast.LENGTH_SHORT).show();
        edit_text.setText("");
        String content = "@#*#@";
        if (type == TXT) content = loadFile_txt(uri);
        else if (type == DOC) content = loadFile_doc(uri);
        else if (type == DOCX) content = loadFile_docx(uri);
        if (content.equals("@#*#@")) {
            Toast.makeText(this, "解析文档失败", Toast.LENGTH_SHORT).show();
            return;
        }
        edit_text.setText(content);
    }

    @Override
    protected void onPause() {
        if (mSpeechSynthesizer != null) {
            mSpeechSynthesizer.stop();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mSpeechSynthesizer != null) {
            mSpeechSynthesizer.stop();
            mSpeechSynthesizer.release();
            mSpeechSynthesizer = null;
        }
        super.onDestroy();
    }

    public String  loadFile_txt(Uri uri) {
        String  content = "@#*#@";
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }

            content = total.toString();

        }catch (Exception e) {
            e.printStackTrace();
        }

        return content;
    }

    public String  loadFile_doc(Uri uri) {
        String content = "@#*#@";
        try {
            WordExtractor extractor = new WordExtractor(getContentResolver().openInputStream(uri));
            content = extractor.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  content;
    }

    public String  loadFile_docx(Uri uri) {
        String content = "@#*#@";
        try {
            InputStream is =  getContentResolver().openInputStream(uri);
            XWPFDocument doc = new XWPFDocument(is);
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            content =  extractor.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  content;
    }


}