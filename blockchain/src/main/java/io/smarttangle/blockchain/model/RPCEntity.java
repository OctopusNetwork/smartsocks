package io.smarttangle.blockchain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by haijun on 2018/3/6.
 */

public class RPCEntity extends Entity {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    public String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
