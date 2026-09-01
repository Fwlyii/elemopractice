package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.PersonUpdateDTO;
import com.tju.elm_bk.dto.UserSearchDTO;
import com.tju.elm_bk.entity.Person;
import com.tju.elm_bk.vo.PersonVO;
import jakarta.validation.Valid;

import java.util.List;

public interface PersonService {
    Person getPersonByUserId(Long id);
    void addPerson(Person person);

    Person updatePerson(@Valid PersonUpdateDTO updateDTO);

    List<PersonVO> listPersons(Integer status);

    List<PersonVO> searchPersons(UserSearchDTO searchDTO);
}
