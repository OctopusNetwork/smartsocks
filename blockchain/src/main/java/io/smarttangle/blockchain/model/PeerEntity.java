package io.smarttangle.blockchain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ser.Serializers;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by haijun on 2018/3/7.
 */

public class PeerEntity extends Entity {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    public List<Item> result;

    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;
        @JsonProperty
        private String ip;
        @JsonProperty
        private String address;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

}
