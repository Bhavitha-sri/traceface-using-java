package com.traceface.service;

import com.traceface.config.AppConfig;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Stores the SHA-256 evidence fingerprint on the Ethereum Sepolia test network.
 * A zero-value transaction is sent from the wallet to itself; the payload
 * "TRACEFACE|SHA256:&lt;hash&gt;" is written in the transaction data field.
 * The connection is created lazily so the application can start without blockchain settings.
 */
public class BlockchainService {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private Web3j web3;
    private Credentials credentials;
    private long chainId;

    // ------------------------------------------------------------------
    // Connect to Sepolia
    // ------------------------------------------------------------------

    private synchronized void connect() throws Exception {
        if (web3 != null) {
            return;
        }
        String rpcUrl = AppConfig.require("SEPOLIA_RPC_URL");
        String privateKey = AppConfig.require("SEPOLIA_PRIVATE_KEY");
        String chain = AppConfig.get("SEPOLIA_CHAIN_ID");
        chainId = (chain == null) ? 11155111L : Long.parseLong(chain);

        Web3j client = Web3j.build(new HttpService(rpcUrl));
        client.web3ClientVersion().send();          // throws if the node cannot be reached

        this.credentials = Credentials.create(privateKey);
        this.web3 = client;
    }

    public String getWalletAddress() throws Exception {
        connect();
        return credentials.getAddress();
    }

    // ------------------------------------------------------------------
    // Record SHA-256 hash on the blockchain
    // ------------------------------------------------------------------

    public String recordHashOnChain(String sha256Hash) throws Exception {
        connect();

        String hash = sha256Hash == null ? "" : sha256Hash.trim().toLowerCase();
        if (!SHA256.matcher(hash).matches()) {
            throw new IllegalArgumentException("SHA-256 hash must contain exactly 64 hexadecimal characters.");
        }

        String wallet = credentials.getAddress();
        String data = Numeric.toHexString((AppConfig.BLOCKCHAIN_PREFIX + hash).getBytes(StandardCharsets.UTF_8));

        // Nonce (including pending transactions)
        BigInteger nonce = web3.ethGetTransactionCount(wallet, DefaultBlockParameterName.PENDING)
                .send().getTransactionCount();

        // EIP-1559 fees
        EthBlock.Block latest = web3.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                .send().getBlock();
        BigInteger baseFee = latest.getBaseFeePerGas();
        BigInteger maxPriorityFee;
        BigInteger maxFee;
        if (baseFee == null) {
            BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();
            maxPriorityFee = gasPrice;
            maxFee = gasPrice;
        } else {
            maxPriorityFee = Convert.toWei("1", Convert.Unit.GWEI).toBigInteger();
            maxFee = baseFee.multiply(BigInteger.TWO).add(maxPriorityFee);
        }

        // Estimate gas
        Transaction estimate = Transaction.createFunctionCallTransaction(
                wallet, nonce, null, null, wallet, BigInteger.ZERO, data);
        EthEstimateGas gasResponse = web3.ethEstimateGas(estimate).send();
        if (gasResponse.hasError()) {
            throw new IllegalStateException("Gas estimation failed: " + gasResponse.getError().getMessage());
        }
        BigInteger gasLimit = gasResponse.getAmountUsed().add(BigInteger.valueOf(5000));

        // Safety check: enough Sepolia ETH for the fee?
        BigInteger balance = web3.ethGetBalance(wallet, DefaultBlockParameterName.LATEST).send().getBalance();
        if (balance.compareTo(gasLimit.multiply(maxFee)) < 0) {
            throw new IllegalStateException("Insufficient Sepolia ETH for transaction fee.");
        }

        // Build + sign locally (zero ETH, sent back to the same wallet)
        RawTransaction raw = RawTransaction.createTransaction(
                chainId, nonce, gasLimit, wallet, BigInteger.ZERO, data, maxPriorityFee, maxFee);
        byte[] signed = TransactionEncoder.signMessage(raw, chainId, credentials);

        // Broadcast
        EthSendTransaction sent = web3.ethSendRawTransaction(Numeric.toHexString(signed)).send();
        if (sent.hasError()) {
            throw new IllegalStateException("Transaction rejected: " + sent.getError().getMessage());
        }
        return sent.getTransactionHash();
    }

    // ------------------------------------------------------------------
    // Wait for confirmation (max 180 seconds)
    // ------------------------------------------------------------------

    public TransactionReceipt waitForConfirmation(String transactionHash) throws Exception {
        connect();
        long deadline = System.currentTimeMillis() + 180_000L;

        while (System.currentTimeMillis() < deadline) {
            Optional<TransactionReceipt> receipt =
                    web3.ethGetTransactionReceipt(transactionHash).send().getTransactionReceipt();
            if (receipt.isPresent()) {
                return receipt.get();
            }
            Thread.sleep(3000);
        }
        throw new TimeoutException("Transaction was not confirmed within 180 seconds.");
    }

    // ------------------------------------------------------------------
    // Read the SHA-256 back from the blockchain
    // ------------------------------------------------------------------

    public String readHashFromTransaction(String transactionHash) throws Exception {
        connect();

        org.web3j.protocol.core.methods.response.Transaction tx =
                web3.ethGetTransactionByHash(transactionHash).send().getTransaction()
                        .orElseThrow(() -> new IllegalStateException("Transaction not found."));

        String payload = new String(Numeric.hexStringToByteArray(tx.getInput()), StandardCharsets.UTF_8);
        if (!payload.startsWith(AppConfig.BLOCKCHAIN_PREFIX)) {
            throw new IllegalArgumentException("This transaction is not a TraceFace evidence transaction.");
        }
        return payload.substring(AppConfig.BLOCKCHAIN_PREFIX.length());
    }

    // ------------------------------------------------------------------
    // Verify SHA-256 against the blockchain
    // ------------------------------------------------------------------

    public static final class Verification {
        public final boolean valid;
        public final String expectedHash;
        public final String blockchainHash;

        Verification(boolean valid, String expectedHash, String blockchainHash) {
            this.valid = valid;
            this.expectedHash = expectedHash;
            this.blockchainHash = blockchainHash;
        }
    }

    public Verification verifyHash(String transactionHash, String expectedHash) throws Exception {
        String expected = expectedHash.trim().toLowerCase();
        String onChain = readHashFromTransaction(transactionHash);
        return new Verification(onChain.equals(expected), expected, onChain);
    }
}
