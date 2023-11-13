package pattern.strategy;

import com.baidu.speech.EventManager;

import java.util.Map;
/**
 * @author lizan
 */
public interface RecognitionStrategy {
    void startRecognition(EventManager asr, Map<String, Object> params);
}
