package com.example.stream.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseHandler {

  @JsonProperty("success")
  private Boolean isSuccess;
  private Integer code;
  @JsonProperty("message")
  private String errorString;
  private Object data;

  public static ResponseHandler success(Object data) {
    return ResponseHandler.builder()
        .code(null)
        .isSuccess(true)
        .data(data)
        .build();
  }

  public static ResponseHandler error(List<String> errorString, Integer code) {
    return ResponseHandler.builder()
        .code(code)
        .isSuccess(false)
        .errorString(String.join(" ", errorString))
        .build();
  }

  public static ResponseHandler errorString(String error, Integer code) {
    return ResponseHandler.builder()
        .code(code)
        .isSuccess(false)
        .errorString(error)
        .build();
  }
}
