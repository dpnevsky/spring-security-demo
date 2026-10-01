package com.pnevsky.spring_security.util;

import com.pnevsky.spring_security.dto.RegistrationForm;
import com.pnevsky.spring_security.repositories.PersonRepository;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.nio.charset.StandardCharsets;
import java.time.Year;

@Component
public class PersonValidator implements Validator {

    private final PersonRepository personRepository;

    public PersonValidator(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return RegistrationForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegistrationForm person = (RegistrationForm) target;

        if (!errors.hasFieldErrors("username")
                && personRepository.findByUsername(person.getUsername()).isPresent()) {
            errors.rejectValue("username", "username.duplicate", "Это имя пользователя уже занято");
        }
        if (person.getYearOfBirth() != null && person.getYearOfBirth() > Year.now().getValue()) {
            errors.rejectValue("yearOfBirth", "yearOfBirth.future", "Год рождения не может быть в будущем");
        }
        if (person.getPassword() != null
                && person.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.rejectValue("password", "password.tooLong", "Пароль должен занимать не более 72 байт UTF-8");
        }
    }
}
