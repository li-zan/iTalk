package pattern.factory;

import android.content.Context;
import android.net.Uri;

import org.apache.poi.hwpf.extractor.WordExtractor;
/**
 * @author lizan
 */
public class DocFileParser implements  FileParser{
    @Override
    public String parseFile(Uri uri, Context context) {
        String content = "@#*#@";
        try {
            WordExtractor extractor = new WordExtractor(context.getContentResolver().openInputStream(uri));
            content = extractor.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  content;
    }
}
