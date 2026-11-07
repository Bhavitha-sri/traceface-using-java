package com.traceface.model;

import com.google.gson.annotations.SerializedName;

/** Blockchain part of an evidence record. */
public class BlockchainInfo {
    public static final String NOT_RECORDED = "NOT_RECORDED";
    public static final String SUBMITTING = "SUBMITTING";
    public static final String CONFIRMING = "CONFIRMING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String FAILED = "FAILED";
    public static final String VERIFICATION_FAILED = "VERIFICATION_FAILED";

    private String status = NOT_RECORDED;
    @SerializedName("transaction_hash")
    private String transactionHash;
    @SerializedName("block_number")
    private Long blockNumber;
    @SerializedName("on_chain_sha256")
    private String onChainSha256;
    private Boolean verified;
    private String error;

    public BlockchainInfo() { }

    public BlockchainInfo(String status, String transactionHash) {
        this.status = status;
        this.transactionHash = transactionHash;
    }

    public String getStatus()          { return status; }
    public String getTransactionHash() { return transactionHash; }
    public Long getBlockNumber()       { return blockNumber; }
    public String getOnChainSha256()   { return onChainSha256; }
    public boolean isVerified()        { return Boolean.TRUE.equals(verified); }
    public String getError()           { return error; }

    public BlockchainInfo withBlockNumber(Long value)  { this.blockNumber = value; return this; }
    public BlockchainInfo withOnChainSha256(String v)  { this.onChainSha256 = v; return this; }
    public BlockchainInfo withVerified(boolean value)  { this.verified = value; return this; }
    public BlockchainInfo withError(String value)      { this.error = value; return this; }
}
