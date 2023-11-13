package pattern.factory;

import android.content.Context;
import android.net.Uri;
/**
 * @author lizan
 */
public interface FileParser {
    String parseFile(Uri uri, Context context);
}
