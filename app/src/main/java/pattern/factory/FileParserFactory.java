package pattern.factory;

import pattern.TTSActivity;
/**
 * @author lizan
 */
public class FileParserFactory {
    public FileParser createFileParser(int fileType) {
        switch (fileType) {
            case TTSActivity.TXT:
                return new TxtFileParser();
            case TTSActivity.DOC:
                return new DocFileParser();
            case TTSActivity.DOCX:
                return new DocxFileParser();
            default:
                return null;
        }
    }
}
