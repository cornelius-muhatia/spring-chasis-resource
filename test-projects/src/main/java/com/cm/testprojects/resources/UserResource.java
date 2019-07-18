package com.cm.testprojects.resources;

import com.cm.projects.spring.resource.chasis.ChasisResource;
import com.cm.projects.spring.resource.chasis.utils.CustomEntry;
import com.cm.projects.spring.resource.chasis.utils.LoggerService;
import com.cm.testprojects.entities.EdittedRecord;
import com.cm.testprojects.entities.Gender;
import com.cm.testprojects.entities.User;
import com.cm.testprojects.repository.UserRepository;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/user")
@Api
public class UserResource extends ChasisResource<User, Long, EdittedRecord> {

    @Autowired
    private UserRepository userRepository;

    public UserResource(LoggerService loggerService, EntityManager entityManager) {
        super(loggerService, entityManager);
    }

    @RequestMapping("/test")
    public ResponseEntity test(@RequestParam("size") int size){

//        log.info("Process large user transactions");
        List<User> users = new ArrayList<>();
        for(int i = 0; i < size; i++){
            users.add(new User(StringUtils.capitalize(RandomStringUtils.random(50,true, false)), new Gender((short)1), new Date()));
//					userRepository.save(new User(StringUtils.capitalize(RandomStringUtils.random(50,true, false)), new Gender((short)1), new Date()));
        }
        userRepository.saveAll(users);
        return ResponseEntity.ok(new CustomEntry<String, String>("message", "Cool"));
    }
}
