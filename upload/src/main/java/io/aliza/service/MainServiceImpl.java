package io.aliza.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MainServiceImpl implements MainService {

	static String filesDirectory = System.getProperty("user.dir") + "/files/";

	Map<String, String> ipAddressCodeMap = new HashMap<>();
	Map<String, String> codeSecretMap = new HashMap<>();
	Map<String, Long> codeTimeMap = new HashMap<>();

	static Logger logger = LoggerFactory.getLogger(MainServiceImpl.class);

	@Override
	public void upload(MultipartFile file, String ipAddress) {
		String code = ipAddressCodeMap.get(ipAddress);

		if (code == null || code.isEmpty())
			return;

		File codeDirectory = new File(filesDirectory + code);
		if (!codeDirectory.exists())
			codeDirectory.mkdirs();

		// Before saving the file, translate to plain text and encode, then encrypt
		try {
			byte[] fileBytes = Base64.encodeBase64(file.getBytes(), true);
			fileProcessor(Cipher.ENCRYPT_MODE, codeSecretMap.get(code), fileBytes,
					new File(filesDirectory + code + "/" + file.getOriginalFilename() + ".encrypted"), true);
			FileSystemUtils
					.deleteRecursively(new File(filesDirectory + code + "/" + file.getOriginalFilename() + ".txt"));
		} catch (Exception e) {
			logger.error("Upload failure: " + e.getMessage());
		}
	}

	@Override
	public String generateString(int length) {
		Random random = new Random();
		String code = "";

		// Generate a random 5-digit alphanumeric code
		for (int i = 0; i < length; i++) {
			int randomChoose = random.nextInt(4 - 1) + 1;

			if (randomChoose == 1) {
				int randomNumber = random.nextInt(58 - 48) + 48;
				code = code.concat(String.valueOf(((char) randomNumber)));
			}

			if (randomChoose == 2) {
				int randomUppercase = random.nextInt(91 - 65) + 65;
				code = code.concat(String.valueOf(((char) randomUppercase)));
			}

			if (randomChoose == 3) {
				int randomLowercase = random.nextInt(123 - 97) + 97;
				code = code.concat(String.valueOf(((char) randomLowercase)));
			}
		}

		return code;
	}

	@Override
	public String generateCode(String ipAddress) {

		String code = generateString(5);

		// If the map already contains the same code, generate new code
		if (ipAddressCodeMap.containsValue(code))
			generateCode(ipAddress);

		ipAddressCodeMap.put(ipAddress, code);
		codeSecretMap.put(code, generateString(32));
		codeTimeMap.put(code, System.currentTimeMillis());

		File file = new File(filesDirectory + code);
		if (!file.exists())
			file.mkdirs();

		return code;
	}

	@Override
	public String codeForIpExists(String ipAddress) {
		if (ipAddressCodeMap.containsKey(ipAddress))
			return ipAddressCodeMap.get(ipAddress);
		else
			return null;
	}

	@Override
	public List<File> getFiles(String code) {
		File folder = new File(filesDirectory + code);
		return listFilesForFolder(folder);
	}

	@Override
	public Map<byte[], String> getFile(int index, String code) {
		File folder = new File(filesDirectory + code);
		File file = listFilesForFolder(folder).get(index);

		Map<byte[], String> byteStringMap = new HashMap<>();

		try {
			// Decrypt the file, then decode it and send bytes over for download
			byte[] fileBytes = fileProcessor(Cipher.DECRYPT_MODE, codeSecretMap.get(code),
					Files.readAllBytes(file.toPath()),
					new File(folder + "/" + file.getName().substring(0, file.getName().length() - 10)), false);

			byteStringMap.put(Base64.decodeBase64(fileBytes),
					file.getName().substring(0, file.getName().length() - 10));

			return byteStringMap;
		} catch (Exception e) {
			logger.error("Cannot find file: " + e.getMessage());
		}

		return null;
	}

	@Override
	public void deleteFile(int index, String code) {
		File folder = new File(filesDirectory + code);
		File file = listFilesForFolder(folder).get(index);

		try {
			FileSystemUtils.deleteRecursively(file);
			logger.info("Deleting file: " + file.getName());
		} catch (Exception e) {
			logger.info("Failed to delete file: " + e.getMessage());
		}
	}

	@Override
	public List<File> listFilesForFolder(final File folder) {
		List<File> files = new ArrayList<>();

		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				files.add(fileEntry);
			}
		}

		return files;
	}

	@Override
	public void endSession(String code) {
		codeTimeMap.remove(code);
		ipAddressCodeMap.values().remove(code);
		codeSecretMap.remove(code);

		File file = new File(filesDirectory + code);
		FileSystemUtils.deleteRecursively(file);

		logger.info("Session for code " + code + " ended. All files deleted.");
	}

	@Override
	public void sessionTimer() {
		for (Entry<String, Long> codeTime : codeTimeMap.entrySet()) {
			String code = codeTime.getKey();
			Long time = codeTime.getValue();

			Long now = System.currentTimeMillis();

			// If now is greater then time of code generation + 5 minutes
			if (now > (time + (1000 * 300))) {
				codeTimeMap.remove(code);
				ipAddressCodeMap.values().remove(code);
				codeSecretMap.remove(code);

				File file = new File(filesDirectory + code);
				FileSystemUtils.deleteRecursively(file);

				logger.info("Code " + code + " has expired. All files deleted.");
			}
		}
	}

	@Override
	public byte[] fileProcessor(int cipherMode, String key, byte[] inputBytes, File outputFile, boolean needOutput) {
		byte[] outputBytes = null;

		try {
			Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(cipherMode, secretKey);

			outputBytes = cipher.doFinal(inputBytes);

			if (needOutput == true) {
				FileOutputStream outputStream = new FileOutputStream(outputFile);
				outputStream.write(outputBytes);

				outputStream.close();
			}

		} catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException
				| IllegalBlockSizeException | IOException e) {
			logger.error(e.getMessage());
		}
		return outputBytes;
	}
	
	@Override
	public String getIpAddr(HttpServletRequest request) {  
		
	    String ip = request.getHeader("x-forwarded-for");  
	    
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = request.getHeader("Proxy-Client-IP");  
	    }  
	    
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = request.getHeader("WL-Proxy-Client-IP");  
	    }  
	    
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
	        ip = request.getRemoteAddr();  
	    }  
	    
	    return ip;  
	}  

}
