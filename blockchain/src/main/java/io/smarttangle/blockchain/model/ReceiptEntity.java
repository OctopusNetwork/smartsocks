package io.smarttangle.blockchain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Created by haijun on 2018/3/8.
 */

public class ReceiptEntity extends Entity {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    public Content result;

    public Content getResult() {
        return result;
    }

    public void setResult(Content result) {
        this.result = result;
    }

    public static class Content implements Serializable {

        private static final long serialVersionUID = 1L;
        @JsonProperty
        public String blockHash;
        @JsonProperty
        public String blockNumber;
        @JsonProperty
        public String to;
        @JsonProperty
        public String from;
        @JsonProperty
        public String contractAddress;
        @JsonProperty
        public String cumulativeGasUsed;
        @JsonProperty
        public String gasUsed;
        @JsonIgnore
        public List<String> logs;
        @JsonIgnore
        public String logsBloom;
        @JsonProperty
        public String root;
        @JsonProperty
        public String transactionHash;
        @JsonProperty
        public String transactionIndex;

        public String getBlockHash() {
            return blockHash;
        }

        public void setBlockHash(String blockHash) {
            this.blockHash = blockHash;
        }

        public String getBlockNumber() {
            return blockNumber;
        }

        public void setBlockNumber(String blockNumber) {
            this.blockNumber = blockNumber;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getContractAddress() {
            return contractAddress;
        }

        public void setContractAddress(String contractAddress) {
            this.contractAddress = contractAddress;
        }

        public String getCumulativeGasUsed() {
            return cumulativeGasUsed;
        }

        public void setCumulativeGasUsed(String cumulativeGasUsed) {
            this.cumulativeGasUsed = cumulativeGasUsed;
        }

        public String getGasUsed() {
            return gasUsed;
        }

        public void setGasUsed(String gasUsed) {
            this.gasUsed = gasUsed;
        }

        public List<String> getLogs() {
            return logs;
        }

        public void setLogs(List<String> logs) {
            this.logs = logs;
        }

        public String getLogsBloom() {
            return logsBloom;
        }

        public void setLogsBloom(String logsBloom) {
            this.logsBloom = logsBloom;
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }
}
