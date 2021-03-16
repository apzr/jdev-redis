package com.dev.redis.demo.cache.dict;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictService {

    @Autowired
    DictRepo dictRepo;

    public List<Dict> top(Integer limit) {
        return dictRepo.top(limit);
    }

    public Dict getById(Integer id) {
        return dictRepo.findOne(id);
    }

}
