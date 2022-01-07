package one.convey.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import one.convey.service.MainService;

@RestController
public class MainController {

	@Autowired
	MainService mainService;

	Logger logger = LoggerFactory.getLogger(MainController.class);

	static Integer maxFileSize = 50 * 1024 * 1024;	// 50MB in bytes
	static Integer maxFileAmount = 20;
	static Integer maxFileNameLength = 50;
	static Integer maxTextLength = 512;
	static Integer maxNumberOfRequests = 100;	// Maximum number of requests per user per 10 minutes
	static Integer maxNumberOfGuesses = 10;	// Maximum number of guesses of code per user per 2 minutes
	
	@GetMapping("/error")
	public ModelAndView errorPage(HttpServletRequest request) {
		ModelAndView modelAndView = new ModelAndView("error");
		return modelAndView;
	}

	@GetMapping("/")
	public ModelAndView mainPageLoad(HttpServletRequest request) {
		ModelAndView modelAndView = new ModelAndView("main");

		String code = mainService.codeForSessionIdExists(request.getSession().getId());

		List<File> files = new ArrayList<>();
		if (code != null)
			files = mainService.getFiles(code);

		if (code == null) {
			modelAndView.addObject("code", "NULL");
		} else {
			modelAndView.addObject("code", code);
			modelAndView.addObject("files", files);
			modelAndView.addObject("text", mainService.getText(code));
			modelAndView.addObject("timeLeft", mainService.getTimeElapsed(code));
		}
		
		modelAndView.addObject("sessionId", request.getSession().getId());
		modelAndView.addObject("isHost", true);
		
		return modelAndView;
	}

	@GetMapping("/join/{joinCode}")
	public ModelAndView filesByCode(@PathVariable String joinCode, HttpServletRequest request) {
		ModelAndView modelAndView = new ModelAndView("main");
		
		Integer numberOfAttempts = mainService.userCodeGuessAmount(request.getSession().getId(), false);
		
		if(numberOfAttempts < maxNumberOfGuesses) {

			if(joinCode != null && !joinCode.isEmpty()) {	
				
				if(!mainService.codeExists(joinCode)) {
					ModelAndView modelAndViewBadCode = new ModelAndView("badcode");
					modelAndViewBadCode.addObject("code", joinCode);
					
					Integer numberOfAttemptsFailed = mainService.userCodeGuessAmount(request.getSession().getId(), true);
	
					if(numberOfAttemptsFailed > maxNumberOfGuesses) {
						logger.info(request.getSession().getId() + " - too many code tries!");
						modelAndViewBadCode.addObject("tooManyTries", true);
					}
					
					return modelAndViewBadCode;
				}
				
				mainService.userEnteredCode(request.getSession().getId(), joinCode, true);
				
				List<File> files = mainService.getFiles(joinCode);
				
				if(files != null && !files.isEmpty())
					modelAndView.addObject("files", files);
				
				modelAndView.addObject("code", joinCode);
				modelAndView.addObject("text", mainService.getText(joinCode));
				modelAndView.addObject("timeLeft", mainService.getTimeElapsed(joinCode));
				
				String code = mainService.codeForSessionIdExists(request.getSession().getId());
				
				if(code != null && !code.isEmpty() && code.equals(joinCode)) 
					modelAndView.addObject("isHost", true);
	
			} else {
				String code = mainService.codeForSessionIdExists(request.getSession().getId());
				
				if(code != null && !code.isEmpty()) {			
					List<File> files = mainService.getFiles(code);
					
					if(files != null && !files.isEmpty())
						modelAndView.addObject("files", files);
					
					modelAndView.addObject("code", code);
					modelAndView.addObject("text", mainService.getText(code));
					modelAndView.addObject("timeLeft", mainService.getTimeElapsed(code));
					
					modelAndView.addObject("isHost", true);
				}
			}
		} else {
			ModelAndView modelAndViewBadCode = new ModelAndView("badcode");
			logger.info(request.getSession().getId() + " - too many code tries!");
			
			modelAndViewBadCode.addObject("code", joinCode);
			modelAndViewBadCode.addObject("tooManyTries", true);
			
			return modelAndViewBadCode;
		}

		return modelAndView;
	}

	@PostMapping("/upload/{code}")
	public ModelMap upload(@PathVariable String code, @RequestParam("file") MultipartFile file,
			HttpServletRequest request) throws IOException {
		
		ModelMap map = new ModelMap();
		if (file.getBytes().length > maxFileSize) {
			logger.info(code + " - uploaded file too big!");
			map.addAttribute("type", "fail");
			map.addAttribute("message", file.getOriginalFilename() + " file size is too big (" + file.getBytes().length/1024/1024 + " MB)!");
			return map;
		}

		String codeFromSessionId = mainService.codeForSessionIdExists(request.getSession().getId());

		if (codeFromSessionId != null && !codeFromSessionId.isEmpty()) {
			if (code.equals(codeFromSessionId)) {
				
				// Check is max upload amount is exceeded in the session
				List<File> uploadedFiles = mainService.getFiles(code);
				
				if(file != null) {
					Long fileSizes = Long.valueOf(file.getBytes().length); // We start with the file currently being uploaded
					for(File fileIterator : uploadedFiles) {
						fileSizes += fileIterator.length();
					}
					
					if(fileSizes > maxFileSize) {
						logger.info(code + " -  total file sizes exceed session limit!");
						map.addAttribute("type", "fail");
						map.addAttribute("message", "Total size of uploaded files is too big (" + fileSizes/1024/1024 + " MB)! Maximum allowed is " + maxFileSize/1024/1024 + ".");
						return map;
					}
					
					// Include the file currently being uploaded into the equation
					if(uploadedFiles.size() + 1 > maxFileAmount) {
						logger.info(code + " - too many total files in session!");
						map.addAttribute("type", "fail");
						map.addAttribute("message", "Total number of uploaded files is too big (" + uploadedFiles.size() + ")! Maximum allowed is " + maxFileAmount + ".");
						return map;
					}
					
					if(file.getOriginalFilename().length() > maxFileNameLength) {
						logger.info(code + " - file name is too long!");
						map.addAttribute("type", "fail");
						map.addAttribute("message", "Name of uploaded file is too long (" + file.getOriginalFilename().length() + " characters)! Maximum allowed is " + maxFileNameLength + ".");
						return map;
					}
					
					if(file.getOriginalFilename().contains("'")) {
						logger.info(code + " - file name contains forbidden character (')!");
						map.addAttribute("type", "fail");
						map.addAttribute("message", "Name of the uploaded file contains special character (') (" + file.getOriginalFilename() + ")! Rename your file!");
						return map;
					}
				}
				
				mainService.upload(file, request.getSession().getId());
				logger.info(code + " - successfully uploaded a file.");
				map.addAttribute("type", "success");
				map.addAttribute("message", "Successfully uploaded a file!");
				
				if(mainService.getFiles(code) != null && !mainService.getFiles(code).isEmpty()) {		
					List<String> fileNames = new ArrayList();
					List<Long> fileSizes = new ArrayList();
					
					for(File fileIterator : mainService.getFiles(code)) {
						fileNames.add(fileIterator.getName().substring(0, fileIterator.getName().length() - 10));
						fileSizes.add(Long.valueOf(Files.readAllBytes(fileIterator.toPath()).length));
					}
					
					map.addAttribute("fileNames", fileNames);
					map.addAttribute("fileSizes", fileSizes);
				}
				
				return map;
			} else {
				logger.info(code + " - no authority to upload to this session!");
				map.addAttribute("type", "fail");
				map.addAttribute("message", "You do not have authority to upload files to this session!");
				return map;
			}
		} else {
			logger.info(code + " - uploading to an invalid session!");
			map.addAttribute("type", "fail");
			map.addAttribute("message", "Invalid session!");
			return map;
		}
	}

	@GetMapping("/code")
	public String generateCode(HttpServletRequest request) {		
		request.getSession().setMaxInactiveInterval(600);
		String codeFromSessionId = mainService.codeForSessionIdExists(request.getSession().getId());
		
		if(codeFromSessionId == null || codeFromSessionId.isEmpty()) {
			String code = mainService.generateCode(request.getSession().getId());
			
			mainService.userEnteredCode(request.getSession().getId(), code, true);
			
			logger.info(code + " - generation a new session!");
			return code;
		} else {
			logger.info(codeFromSessionId + " - still an active session, therefore a new session cannot be created!");
			return "";
		}
	}
	
	
	@PostMapping("/text/update/{code}")
	public ModelMap editText(@PathVariable String code, @RequestParam("text") String text, HttpServletRequest request) throws IOException {
		
		ModelMap map = new ModelMap();
		
		if (text.length() > maxTextLength) {
			logger.info(code + " - text is too long!");
			map.addAttribute("type", "fail");
			map.addAttribute("message", "Text has too many characters " + text.length() + "! (" + maxTextLength + " characters are permitted)");
			return map;
		}

		String codeFromSessionId = mainService.codeForSessionIdExists(request.getSession().getId());

		if (codeFromSessionId != null && !codeFromSessionId.isEmpty()) {
			if (code.equals(codeFromSessionId)) {
				
				text = text.replaceAll("'", "\"");
				mainService.updateText(request.getSession().getId(), text);
				
				logger.info(code + " - successfully updated the text.");
				map.addAttribute("type", "success");
				map.addAttribute("message", "Successfully updated your text!");
				
				return map;
			} else {
				logger.info(code + " - no authority to update text in this session!");
				map.addAttribute("type", "fail");
				map.addAttribute("message", "You do not have authority to update the text in this session!");
				return map;
			}
		} else {
			logger.info(code + " - updating text in an invalid session!");
			map.addAttribute("type", "fail");
			map.addAttribute("message", "Invalid session!");
			return map;
		}
	}
	
	@GetMapping("/download/{code}/{index}")
	public ModelMap download(@PathVariable String code, @PathVariable int index, HttpServletResponse response, HttpServletRequest request)
			throws IOException {

		ModelMap map = new ModelMap();
		
		List<String> listOfUserCodes = mainService.userEnteredCode(request.getSession().getId(), null, false);
		Map<byte[], String> byteStringMap = mainService.getFile(index, code);
		
		if(!listOfUserCodes.contains(code) || byteStringMap == null) {
			map.addAttribute("type", "fail");
			map.addAttribute("message", "Invalid session!");
			return map;
		}

		byte[] file = null;
		String fileName = null;

		for (byte[] key : byteStringMap.keySet()) {
			file = key;
			fileName = byteStringMap.get(key);
		}

		response.setContentType("application/octet-stream");

		String headerKey = "Content-Disposition";
		String headerValue = "attachment; filename=" + fileName;

		response.setHeader(headerKey, headerValue);

		ServletOutputStream outputStream = response.getOutputStream();

		BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(file));

		byte[] buffer = new byte[8192]; // 8Kb buffer
		int bytesRead = -1;

		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		inputStream.close();
		outputStream.close();
		
		logger.info(code + " - a file is being downloaded.");
		map.addAttribute("type", "success");
		map.addAttribute("message", "File is being downloaded...");
		return map;
	}

	@GetMapping("/delete/{code}/{index}")
	public ModelMap delete(@PathVariable int index, @PathVariable String code, HttpServletResponse response,
			HttpServletRequest request) throws IOException {
	
		ModelMap map = new ModelMap();

		String codeFromSessionId = mainService.codeForSessionIdExists(request.getSession().getId());

		if (codeFromSessionId != null && !codeFromSessionId.isEmpty()) {
			if (code.equals(codeFromSessionId)) {
				
				mainService.deleteFile(index, code);
				
				if(mainService.getFiles(code) != null && !mainService.getFiles(code).isEmpty()) {		
					List<String> fileNames = new ArrayList();
					List<Long> fileSizes = new ArrayList();
					
					for(File file : mainService.getFiles(code)) {
						fileNames.add(file.getName().substring(0, file.getName().length() - 10));
						fileSizes.add(Long.valueOf(Files.readAllBytes(file.toPath()).length));
					}
					
					map.addAttribute("fileNames", fileNames);
					map.addAttribute("fileSizes", fileSizes);
				}
				
				logger.info(code + " - successfully deleted a file.");
				map.addAttribute("type", "success");
				map.addAttribute("message", "Successfully deleted a file!");
				return map;
			} else {
				logger.info(code + " - no authority to delete this file!");
				map.addAttribute("type", "fail");
				map.addAttribute("message", "You do not have the authority to delete this file!");
				return map;
			}
		} else {
			logger.info(code + " - accessing an invalid session!");
			map.addAttribute("type", "fail");
			map.addAttribute("message", "Invalid session!");
			return map;
		}
	}

	@GetMapping("/endSession/{code}")
	public Integer endSession(@PathVariable String code, HttpServletResponse response, HttpServletRequest request)
			throws IOException {

		String codeFromSessionId = mainService.codeForSessionIdExists(request.getSession().getId());

		if (codeFromSessionId != null && !codeFromSessionId.isEmpty()) {
			if (code.equals(codeFromSessionId)) {
				mainService.endSession(code);
				logger.info(code + " - successfully ended a session.");
				request.getSession().invalidate();
				return 0;
			} else {
				logger.info(code + " - no authority to end this session!");
				return 1;
			}
		} else {
			logger.info(code + " - accessing an invalid session!");
			return 2;
		}
	}
}