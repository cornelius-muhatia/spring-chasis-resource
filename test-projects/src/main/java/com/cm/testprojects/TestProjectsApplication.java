package com.cm.testprojects;

import com.cm.projects.spring.resource.chasis.config.ExceptionTranslator;
import com.cm.projects.spring.resource.chasis.utils.AppConstants;
import com.cm.testprojects.entities.Gender;
import com.cm.testprojects.entities.Status;
import com.cm.testprojects.entities.User;
import com.cm.testprojects.repository.GenderRepository;
import com.cm.testprojects.repository.StatusRepository;
import com.cm.testprojects.repository.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class TestProjectsApplication {
    Logger log = LoggerFactory.getLogger("MAIN RESOURCE");
	public static void main(String[] args) {
		SpringApplication.run(TestProjectsApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandRunner(StatusRepository repository, GenderRepository genderRepo, UserRepository userRepository){

		List<Status> status = new ArrayList<>(){{
			add(new Status((short)1, "New"));
			add(new Status((short)2, "Active"));
			add(new Status((short)3, "Inactive"));
			add(new Status((short)7, "Updated"));
            add(new Status(AppConstants.STATUS_ID_DELETED, "Deleted"));
			add(new Status(AppConstants.STATUS_ID_DEACTIVATE, "Deactivate"));
			add(new Status(AppConstants.STATUS_ID_DEACTIVATED, "Deactivated"));
            add(new Status(AppConstants.STATUS_ID_ACTIVATE, "Activation"));
		}};
		List<Gender> gender = new ArrayList<>(){{
		    add(new Gender((short)1, "Male", new Status((short)2)));
		    add(new Gender((short) 2, "Female", new Status((short)1)));
			add(new Gender((short) 3, "Others", new Status((short)2)));
        }};

//			};

		return (args) -> {
//				repository.saveAll(status);
//				genderRepo.saveAll(gender);

//			Runnable run = () -> {

//			Thread t = new Thread(run);
//			t.start();
		};
	}

}
