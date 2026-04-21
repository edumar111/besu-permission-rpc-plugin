package com.example.besu.plugin.blockchain;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NodeKeyLoader {

    private NodeKeyLoader() {}

    public static Credentials load(Path keyFile) throws Exception {
        if (!Files.exists(keyFile)) {
            throw new IllegalStateException("Node key file not found: " + keyFile.toAbsolutePath());
        }
        String hex = Files.readString(keyFile).trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.isEmpty()) {
            throw new IllegalStateException("Node key file is empty: " + keyFile.toAbsolutePath());
        }
        ECKeyPair kp = ECKeyPair.create(new BigInteger(hex, 16));
        return Credentials.create(kp);
    }
}
