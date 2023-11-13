package pattern.strategy;

import com.baidu.speech.EventManager;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONObject;

import java.util.Map;

/**
 * @author lizan
 */
public class FileRecognitionStrategy implements RecognitionStrategy{
    @Override
    public void startRecognition(EventManager asr, Map<String, Object> params) {


        String event = SpeechConstant.ASR_START;

        params.put(SpeechConstant.IN_FILE,
                "#priv.lz.designapplication.FileUtil.getMyFileInputStream()");
        String json = new JSONObject(params).toString();
        asr.send(event, json, null, 0, 0);
    }
}
