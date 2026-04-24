package com.example.besu.plugin.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

public class ContractTxSender {

    private final String rpcUrl;
    private final long chainId;
    private final BigInteger gasPrice;
    private final BigInteger gasLimit;
    private final String contractAddress;
    private final Credentials credentials;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger rpcId = new AtomicInteger(1);

    public ContractTxSender(String rpcUrl, long chainId, BigInteger gasPrice, BigInteger gasLimit,
                            String contractAddress, Credentials credentials) {
        this.rpcUrl = rpcUrl;
        this.chainId = chainId;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.contractAddress = contractAddress;
        this.credentials = credentials;
    }

    public String getSenderAddress() {
        return credentials.getAddress();
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public long getChainId() {
        return chainId;
    }

    public String sendTx(String encodedFunction) throws Exception {
        BigInteger nonce = getPendingNonce();
        RawTransaction tx = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress, BigInteger.ZERO, encodedFunction);
        byte[] signed = TransactionEncoder.signMessage(tx, chainId, credentials);
        return sendRawTransaction(Numeric.toHexString(signed));
    }

    private BigInteger getPendingNonce() throws Exception {
        String params = "[\"" + credentials.getAddress() + "\",\"pending\"]";
        JsonNode root = mapper.readTree(rpcCall("eth_getTransactionCount", params));
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new RuntimeException("eth_getTransactionCount error: " + error);
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            throw new RuntimeException("eth_getTransactionCount empty result");
        }
        return Numeric.decodeQuantity(result.asText());
    }

    private String sendRawTransaction(String signedHex) throws Exception {
        String params = "[\"" + signedHex + "\"]";
        JsonNode root = mapper.readTree(rpcCall("eth_sendRawTransaction", params));
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new RuntimeException("eth_sendRawTransaction error: " + error);
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            throw new RuntimeException("eth_sendRawTransaction empty result");
        }
        return result.asText();
    }

    private String rpcCall(String method, String paramsJson) throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"method\":\"" + method
                + "\",\"params\":" + paramsJson + ",\"id\":" + rpcId.getAndIncrement() + "}";
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(rpcUrl);
            post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
            return client.execute(post, r -> EntityUtils.toString(r.getEntity()));
        }
    }
}
