package com.example.demo.controller.me;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.me.MisSociosDto;
import com.example.demo.service.MeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping("/socios")
    public BaseResponse<List<MisSociosDto>> misSocios(Authentication auth) {
        System.out.println("ME email=" + auth.getName());
        return meService.misSocios(auth.getName());
    }
}
