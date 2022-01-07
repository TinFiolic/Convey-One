package one.convey.service;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface MainService {
	void upload(MultipartFile file, String sessionId);

	String generateCode(String sessionId);

	String codeForSessionIdExists(String sessionId);

	List<File> getFiles(String code);

	Map<byte[], String> getFile(int index, String code);

	void deleteFile(int index, String code);

	void endSession(String code);

	byte[] fileProcessor(int cipherMode, String key, byte[] inputBytes, File outputFile, boolean needOutput);

	List<File> listFilesForFolder(File folder);

	void updateText(String sessionId, String text);

	String getText(String code);

	String getTimeElapsed(String code);

	Boolean codeExists(String code);

	String generateRandomString(int length);

	Integer numberOfCodeGuessesMade(String sessionId, boolean write);

	List<String> userEnteredCode(String sessionId, String code, boolean write);

	Integer numberOfRequestsMade(String sessionId, boolean write);
}
