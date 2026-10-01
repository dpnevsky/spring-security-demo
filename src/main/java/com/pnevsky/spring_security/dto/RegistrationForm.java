package com.pnevsky.spring_security.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "Укажите имя пользователя")
    @Size(min = 3, max = 50, message = "Имя должно содержать от 3 до 50 символов")
    @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Используйте латинские буквы, цифры, точку, дефис или подчёркивание")
    private String username;

    @NotBlank(message = "Укажите пароль")
    @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
    private String password;

    @NotNull(message = "Укажите год рождения")
    @Min(value = 1900, message = "Год рождения должен быть не раньше 1900")
    private Integer yearOfBirth;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(Integer yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
    }
}
