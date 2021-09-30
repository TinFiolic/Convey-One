package io.aliza.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import io.aliza.service.MainService;

@RestController
public class MainController {

	@Autowired
	MainService mainService;

	Logger logger = LoggerFactory.getLogger(MainController.class);

	static Integer maxFileSize = 20971520;	// 20MB in bytes
	static Integer maxFileAmount = 20;
	static Integer maxFileNameLength = 30;

	@GetMapping("/")
	public ModelAndView mainPageLoad(HttpServletRequest request) {
		ModelAndView modelAndView = new ModelAndView("main");

		String code = mainService.codeForIpExists(mainService.getIpAddr(request));

		List<File> files = new ArrayList<>();
		if (code != null)
			files = mainService.getFiles(code);

		if (code == null) {
			modelAndView.addObject("code", "NULL");
		} else {
			modelAndView.addObject("code", code);
			modelAndView.addObject("files", files);
		}


		modelAndView.addObject("ip", mainService.getIpAddr(request));
		modelAndView.addObject("fromLink", false);
		return modelAndView;
	}

	@GetMapping("/join/{joinCode}")
	public ModelAndView filesByCode(@PathVariable String joinCode, HttpServletRequest request) {
		ModelAndView modelAndView = new ModelAndView("main");

		if(joinCode != null && !joinCode.isEmpty()) {
			List<File> files = mainService.getFiles(joinCode);
			
			if(files != null && !files.isEmpty())
				modelAndView.addObject("files", files);
			
			modelAndView.addObject("code", joinCode);
			
			String code = mainService.codeForIpExists(mainService.getIpAddr(request));
			
			if(code != null && !code.isEmpty() && code.equals(joinCode)) {
				modelAndView.addObject("isHost", true);
			}
		} else {
			String code = mainService.codeForIpExists(mainService.getIpAddr(request));
			
			if(code != null && !code.isEmpty()) {			
				List<File> files = mainService.getFiles(code);
				
				if(files != null && !files.isEmpty())
					modelAndView.addObject("files", files);
				
				modelAndView.addObject("code", code);
				
				modelAndView.addObject("isHost", true);
			}
		}

		modelAndView.addObject("fromLink", true);
		return modelAndView;
	}

	@PostMapping("/upload/{code}")
	public Integer upload(@PathVariable String code, @RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		if (file.getSize() > maxFileSize) {
			logger.info(code + " - uploaded file too big!");
			return 3;
		}

		String codeFromIp = mainService.codeForIpExists(mainService.getIpAddr(request));

		if (codeFromIp != null && !codeFromIp.isEmpty()) {
			if (code.equals(codeFromIp)) {
				
				// Check is max upload amount is exceeded in the session
				List<File> uploadedFiles = mainService.getFiles(code);
				
				if(uploadedFiles != null && !uploadedFiles.isEmpty()) {
					Long fileSizes = file.getSize(); // We start with the file currently being uploaded
					for(File fileIterator : uploadedFiles) {
						fileSizes += fileIterator.length();
					}
					
					if(fileSizes > maxFileSize) {
						logger.info(code + " -  total file sizes exceed session limit!");
						return 4;
					}
					
					// Include the file currently being uploaded into the equation
					if(uploadedFiles.size() + 1 > maxFileAmount) {
						logger.info(code + " - too many total files in session!");
						return 5;
					}
					
					if(file.getName().length() > maxFileNameLength) {
						logger.info(code + " - file name is too long!");
						return 6;
					}
				}
				
				mainService.upload(file, mainService.getIpAddr(request));
				logger.info(code + " - successfully uploaded a file.");
				return 0;
			} else {
				logger.info(code + " - no authority to upload to this session!");
				return 1;
			}
		} else {
			logger.info(code + " - uploading to an invalid session!");
			return 2;
		}
	}

	@GetMapping("/code")
	public String generateCode(HttpServletRequest request) {
		String codeFromIp = mainService.codeForIpExists(mainService.getIpAddr(request));
		
		if(codeFromIp == null || codeFromIp.isEmpty()) {
			String code = mainService.generateCode(mainService.getIpAddr(request));
			logger.info(code + " - generation a new session!");
			return code;
		} else {
			logger.info(codeFromIp + " - still an active session, therefore a new session cannot be created!");
			return "";
		}
	}

	@GetMapping("/download/{code}/{index}")
	public void download(@PathVariable String code, @PathVariable int index, HttpServletResponse response)
			throws IOException {

		Map<byte[], String> byteStringMap = mainService.getFile(index, code);

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
	}

	@GetMapping("/delete/{code}/{index}")
	public Integer delete(@PathVariable int index, @PathVariable String code, HttpServletResponse response,
			HttpServletRequest request) throws IOException {

		String codeFromIp = mainService.codeForIpExists(mainService.getIpAddr(request));

		if (codeFromIp != null && !codeFromIp.isEmpty()) {
			if (code.equals(codeFromIp)) {
				mainService.deleteFile(index, code);
				logger.info(code + " - successfully deleted a file.");
				return 0;
			} else {
				logger.info(code + " - no authority to delete this file!");
				return 1;
			}
		} else {
			logger.info(code + " - accessing an invalid session!");
			return 2;
		}
	}

	@GetMapping("/endSession/{code}")
	public Integer endSession(@PathVariable String code, HttpServletResponse response, HttpServletRequest request)
			throws IOException {

		String codeFromIp = mainService.codeForIpExists(mainService.getIpAddr(request));

		if (codeFromIp != null && !codeFromIp.isEmpty()) {
			if (code.equals(codeFromIp)) {
				mainService.endSession(code);
				logger.info(code + " - successfully ended a session.");
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