package io.smarttangle.blockchain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Created by haijun on 2018/3/7.
 */

public class BCErrorEntity extends Entity {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    private int status;
    @JsonProperty
    private Error error;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public static class Error implements Serializable {
        @JsonProperty
        private String code;
        @JsonProperty
        private String message;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
