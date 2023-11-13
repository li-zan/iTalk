package pattern.factory;

import android.content.Context;
import android.net.Uri;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;
/**
 * @author lizan
 */
public class DocxFileParser implements FileParser{
    @Override
    public String parseFile(Uri uri, Context context) {
        String content = "@#*#@";
        try {
            InputStream is =  context.getContentResolver().openInputStream(uri);
            XWPFDocument doc = new XWPFDocument(is);
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            content =  extractor.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  content;
    }
}
