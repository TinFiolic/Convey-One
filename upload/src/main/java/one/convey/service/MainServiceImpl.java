package one.convey.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MainServiceImpl implements MainService {

	static String filesDirectory = System.getProperty("user.dir") + "/files/";

	//Host session id and its code
	Map<String, String> sessionIdCodeMap = new HashMap<>();
	
	Map<String, String> codeSecretMap = new HashMap<>();
	Map<String, Long> codeTimeMap = new HashMap<>();
	Map<String, String> codeTextMap = new HashMap<>();
	
	//Number of requests a user has performed in the last 10 minutes
	Map<String, Integer> sessionIdRequestsMap = new HashMap<>();
	
	//Number of guesses of code a user has tried in the last 2 minutes
	Map<String, Integer> sessionIdGuessesMap = new HashMap<>();
	
	//Has a user (session id) ever entered a certain code before (list of codes)
	Map<String, List<String>> sessionIdCodeHistoryMap = new HashMap<>();

	static Logger logger = LoggerFactory.getLogger(MainServiceImpl.class);

	@Override
	public void upload(MultipartFile file, String sessionId) {
		String code = sessionIdCodeMap.get(sessionId);

		if (code == null || code.isEmpty())
			return;

		File codeDirectory = new File(filesDirectory + code);
		if (!codeDirectory.exists())
			codeDirectory.mkdirs();

		// Before saving the file, translate to plain text and encode, then encrypt
		try {
			fileProcessor(Cipher.ENCRYPT_MODE, codeSecretMap.get(code), file.getBytes(), new File(filesDirectory + code + "/" + file.getOriginalFilename() + ".encrypted"), true);
			FileSystemUtils.deleteRecursively(new File(filesDirectory + code + "/" + file.getOriginalFilename() + ".txt"));
		} catch (Exception e) {
			logger.error("Upload failure: " + e.getMessage());
		}
	}

	@Override
	public String generateRandomString(int length) {
		Random random = new Random();
		String code = "";

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
	public Integer userCodeGuessAmount(String sessionId, boolean hasUserTriedAnotherCode) {
		if(!sessionIdGuessesMap.containsKey(sessionId))
			sessionIdGuessesMap.put(sessionId, 0);
		
		if(hasUserTriedAnotherCode == true)
			sessionIdGuessesMap.put(sessionId, sessionIdGuessesMap.get(sessionId) + 1);
		
		return sessionIdGuessesMap.get(sessionId);
	}

	@Override
	public String generateCode(String sessionId) {

		String code = generateRandomString(5);

		// If the map already contains the same code, generate new code
		if (sessionIdCodeMap.containsValue(code))
			generateCode(sessionId);

		sessionIdCodeMap.put(sessionId, code);
		codeSecretMap.put(code, generateRandomString(32));
		codeTimeMap.put(code, System.currentTimeMillis());

		File file = new File(filesDirectory + code);
		if (!file.exists())
			file.mkdirs();

		return code;
	}
	
	@Override
	public void updateText(String sessionId, String text) {
		String code = sessionIdCodeMap.get(sessionId);

		if (code == null || code.isEmpty())
			return;
		
		codeTextMap.put(code, text);
	}
	
	@Override
	public String getText(String code) {
		if(codeTextMap.containsKey(code))
			return codeTextMap.get(code);
		
		return null;
	}

	@Override
	public String codeForSessionIdExists(String sessionId) {
		if (sessionIdCodeMap.containsKey(sessionId))
			return sessionIdCodeMap.get(sessionId);
		else
			return null;
	}
	
	@Override
	public Boolean codeExists(String code) {
		if(sessionIdCodeMap.containsValue(code)) 
			return true;
		else
			return false;
	}
	
	@Override
	public List<String> userEnteredCode(String sessionId, String code, boolean write) {
		List<String> listOfCodes = sessionIdCodeHistoryMap.get(sessionId);
		
		if(listOfCodes == null)
			listOfCodes = new ArrayList<String>();		
		
		if(write) {
			if(listOfCodes.isEmpty()) {
				sessionIdCodeHistoryMap.put(sessionId, Arrays.asList(code));
			} else {
				if(!listOfCodes.contains(code)) {
					listOfCodes.add(code);
					sessionIdCodeHistoryMap.put(sessionId, listOfCodes);
				}
			}
		}
		
		return listOfCodes;
	}

	@Override
	public List<File> getFiles(String code) {
		File folder = new File(filesDirectory + code);
		return listFilesForFolder(folder);
	}

	@Override
	public Map<byte[], String> getFile(int index, String code) {
		File folder = new File(filesDirectory + code);
		File file;
		
		try {
			file = listFilesForFolder(folder).get(index);
		} catch(Exception e) {
			return null;
		}

		Map<byte[], String> byteStringMap = new HashMap<>(); 

		try {
			// Decrypt the file, then decode it and send bytes over for download
			byte[] fileBytes = fileProcessor(Cipher.DECRYPT_MODE, codeSecretMap.get(code),
					Files.readAllBytes(file.toPath()),
					new File(folder + "/" + file.getName().substring(0, file.getName().length() - 10)), false);

			byteStringMap.put(fileBytes,
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
		sessionIdCodeMap.values().remove(code);
		codeSecretMap.remove(code);
		codeTextMap.remove(code);

		File file = new File(filesDirectory + code);
		FileSystemUtils.deleteRecursively(file);

		logger.info("Session for code " + code + " ended. All files deleted.");
	}
	
	@Override
	public String getTimeElapsed(String code) {
		Long time = codeTimeMap.get(code);
		Long now = System.currentTimeMillis();
		
		return String.valueOf(now - time);	
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

}
