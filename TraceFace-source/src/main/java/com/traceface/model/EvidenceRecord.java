package com.traceface.model;

/** Complete evidence record stored in evidence_records.json. */
public class EvidenceRecord {
    private EvidencePayload evidence;
    private String sha256;
    private BlockchainInfo blockchain = new BlockchainInfo();

    public EvidenceRecord(EvidencePayload evidence, String sha256) {
        this.evidence = evidence;
        this.sha256 = sha256;
    }

    public EvidencePayload getEvidence()  { return evidence; }
    public String getSha256()             { return sha256; }
    public BlockchainInfo getBlockchain() { return blockchain; }
    public void setBlockchain(BlockchainInfo blockchain) { this.blockchain = blockchain; }
}
