package com.example.besu.plugin.blockchain;

import com.example.besu.plugin.PermissionEvent;
import com.example.besu.plugin.PermissionEventCapture;
import com.example.besu.plugin.PermissionEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockchainReporter implements PermissionEventListener {

    private static final Logger LOG = LogManager.getLogger(BlockchainReporter.class);

    private final ContractTxSender sender;
    private final PermissionEventCapture eventCapture;
    private final BlockingQueue<TxJob> queue = new LinkedBlockingQueue<>();
    private volatile Thread worker;
    private volatile boolean running;

    public BlockchainReporter(ContractTxSender sender, PermissionEventCapture eventCapture) {
        this.sender = sender;
        this.eventCapture = eventCapture;
    }

    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::runLoop, "blockchain-reporter");
        worker.setDaemon(true);
        worker.start();
        String msg = "[BLOCKCHAIN] Reporter started. Contract: " + sender.getContractAddress()
                + " | ChainId: " + sender.getChainId()
                + " | Sender: " + sender.getSenderAddress();
        eventCapture.logLine(msg);
        LOG.info(msg);
    }

    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    @Override
    public void onPermissionChanged(PermissionEvent event) {
        switch (event.getEventType()) {
            case ADD_ACCOUNTS    -> event.getAddresses().forEach(a -> enqueueAccount(a, true));
            case REMOVE_ACCOUNTS -> event.getAddresses().forEach(a -> enqueueAccount(a, false));
            case ADD_NODES       -> event.getAddresses().forEach(e -> enqueueEnode(e, true));
            case REMOVE_NODES    -> event.getAddresses().forEach(e -> enqueueEnode(e, false));
        }
    }

    private void enqueueAccount(String address, boolean added) {
        String method = added ? "reportAccountAdded" : "reportAccountRemoved";
        Function fn = new Function(
                method,
                Collections.<Type>singletonList(new Address(address)),
                Collections.emptyList());
        queue.offer(new TxJob(method, address, fn));
    }

    private void enqueueEnode(String enode, boolean added) {
        String method = added ? "reportEnodeAdded" : "reportEnodeRemoved";
        Function fn = new Function(
                method,
                Collections.<Type>singletonList(new Utf8String(enode)),
                Collections.emptyList());
        queue.offer(new TxJob(method, enode, fn));
    }

    private void runLoop() {
        while (running) {
            TxJob job;
            try {
                job = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                String encoded = FunctionEncoder.encode(job.function);
                String txHash = sender.sendTx(encoded);
                String okMsg = "[BLOCKCHAIN] " + job.method + "(" + truncate(job.arg) + ") -> " + txHash;
                eventCapture.logLine(okMsg);
                LOG.info(okMsg);
            } catch (Exception e) {
                String msg = "[BLOCKCHAIN ERROR] " + job.method + "(" + truncate(job.arg) + "): " + e.getMessage();
                eventCapture.logLine(msg);
                LOG.error(msg);
            }
        }
    }

    private static String truncate(String s) {
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }

    private static final class TxJob {
        final String method;
        final String arg;
        final Function function;

        TxJob(String method, String arg, Function function) {
            this.method = method;
            this.arg = arg;
            this.function = function;
        }
    }
}
