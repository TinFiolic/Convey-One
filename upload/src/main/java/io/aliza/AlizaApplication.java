package io.aliza;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.util.FileSystemUtils;

import io.aliza.service.MainService;

@SpringBootApplication
public class AlizaApplication {

	static Logger logger = LoggerFactory.getLogger(AlizaApplication.class);

	@Autowired
	private static MainService service;

	static ApplicationContext context;

	public static void main(String[] args) {
		context = SpringApplication.run(AlizaApplication.class, args);

		service = context.getBean(MainService.class);

		// Delete files on startup
		File file = new File(System.getProperty("user.dir") + "/files");
		try {
			logger.info("Deleting all folders at startup.");
			FileSystemUtils.deleteRecursively(file);

			if (!file.exists())
				file.mkdirs();

		} catch (Exception e) {
			logger.error("Could not delete folders at startup." + e.getMessage());
		}

		// Background timer for sessions on a separate thread
		new Thread(() -> {
			while (true) {
				service.sessionTimer();
				try {
					Thread.sleep(10000); // Check every 10 seconds
				} catch (InterruptedException e) {
					logger.error("Timer error: " + e.getMessage());
				}
			}
		}).start();
	}

}
