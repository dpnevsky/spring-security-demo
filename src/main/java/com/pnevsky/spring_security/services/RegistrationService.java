package com.pnevsky.spring_security.services;

import com.pnevsky.spring_security.models.Person;
import com.pnevsky.spring_security.dto.RegistrationForm;
import com.pnevsky.spring_security.repositories.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class RegistrationService {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegistrationService(PersonRepository personRepository, PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegistrationForm form){
        Person person = new Person(form.getUsername(), form.getYearOfBirth());
        person.setPassword(passwordEncoder.encode(form.getPassword()));
        person.setRole("ROLE_USER");
        personRepository.save(person);
    }

}
