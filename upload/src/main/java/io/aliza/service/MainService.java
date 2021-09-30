package io.aliza.service;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

public interface MainService {
	void upload(MultipartFile file, String ipAddress);

	String generateCode(String ipAddress);

	String codeForIpExists(String ipAddress);

	List<File> getFiles(String code);

	Map<byte[], String> getFile(int index, String code);

	void sessionTimer();

	void deleteFile(int index, String code);

	void endSession(String code);

	String generateString(int length);

	byte[] fileProcessor(int cipherMode, String key, byte[] inputBytes, File outputFile, boolean needOutput);

	List<File> listFilesForFolder(File folder);

	String getIpAddr(HttpServletRequest request);
}
