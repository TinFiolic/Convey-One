package one.convey;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.FileSystemUtils;

@SpringBootApplication
@EnableScheduling
public class ConveyOneApplication {

	static Logger logger = LoggerFactory.getLogger(ConveyOneApplication.class);

	static ApplicationContext context;

	public static void main(String[] args) {
		context = SpringApplication.run(ConveyOneApplication.class, args);
		
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
	}
}
