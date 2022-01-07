package one.convey.service;

import java.io.File;
import java.util.Map.Entry;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

@Service
public class ScheduledServiceImpl implements ScheduledService {
	
	static Logger logger = LoggerFactory.getLogger(MainServiceImpl.class);
	
	@Autowired
	MainServiceImpl mainServiceImpl;

	@Override
	@Scheduled(fixedDelay = 5000L)
	public void sessionTimer() {
		logger.info("Checking for sessions to delete...");
		if(!mainServiceImpl.codeTimeMap.isEmpty()) {
			for (Entry<String, Long> codeTime : mainServiceImpl.codeTimeMap.entrySet()) {
				String code = codeTime.getKey();
				Long time = codeTime.getValue();
	
				Long now = System.currentTimeMillis();
	
				// If now is greater then time of code generation + 5 minutes
				if (now > (time + (1000 * 600))) {
					mainServiceImpl.codeTimeMap.remove(code);
					mainServiceImpl.sessionIdCodeMap.values().remove(code);
					mainServiceImpl.codeSecretMap.remove(code);
					mainServiceImpl.codeTextMap.remove(code);
	
					File file = new File(MainServiceImpl.filesDirectory + code);
					FileSystemUtils.deleteRecursively(file);
	
					logger.info("Code " + code + " has expired. All files deleted.");
				}
			}
		}
		
		//Ovdje napravit jos 2 checka, jedan za code entered amount svaka 2 min shemica i onda se cleara lista i jedan za overall requestove u 10 min
	}
	
	@Override
	@Scheduled(fixedDelay = 60000L)
	public void enterInvalidCode() {
		logger.info("Clearing invalid code list...");
		mainServiceImpl.sessionIdGuessesMap.clear();
	}
	
	@Override
	@PreDestroy
	public void onShutdown() {
		logger.info("Shutting down and deleting sessions...");
		if(!mainServiceImpl.codeTimeMap.isEmpty()) {
			for (Entry<String, Long> codeTime : mainServiceImpl.codeTimeMap.entrySet()) {
				String code = codeTime.getKey();

				File file = new File(MainServiceImpl.filesDirectory + code);
				FileSystemUtils.deleteRecursively(file);

				logger.info("Code " + code + " has expired. All files deleted.");
				
			}
		}
	}

}
