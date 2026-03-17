package com.example.demo.config;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseResponse<T> {
    private String mensaje;
    private int status;
    private T data;

    public static <T> BaseResponse<T> ok(String msg, T data){
        return BaseResponse.<T>builder().mensaje(msg).status(200).data(data).build();
    }
    public static <T> BaseResponse<T> created(String msg, T data){
        return BaseResponse.<T>builder().mensaje(msg).status(201).data(data).build();
    }
    public static <T> BaseResponse<T> bad(String msg){
        return BaseResponse.<T>builder().mensaje(msg).status(400).data(null).build();
    }
}
