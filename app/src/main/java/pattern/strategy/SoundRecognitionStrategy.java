package pattern.strategy;

import com.baidu.speech.EventManager;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONObject;

import java.util.Map;
/**
 * @author lizan
 */
public class SoundRecognitionStrategy implements RecognitionStrategy{
    @Override
    public void startRecognition(EventManager asr, Map<String, Object> params) {
        params.remove(SpeechConstant.IN_FILE);
        String event = SpeechConstant.ASR_START;
        String json = new JSONObject(params).toString();
        asr.send(event, json, null, 0, 0);
    }
}
