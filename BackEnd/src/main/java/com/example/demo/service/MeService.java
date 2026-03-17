package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.me.MisSociosDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MeService {
    BaseResponse<List<MisSociosDto>> misSocios(String email);
}
