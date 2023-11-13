package pattern.factory;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
/**
 * @author lizan
 */
public class TxtFileParser implements FileParser{
    @Override
    public String parseFile(Uri uri, Context context) {
        String  content = "@#*#@";
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
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
}
