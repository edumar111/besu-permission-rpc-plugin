package com.example.besu.plugin;

import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Wraps an existing RPC method function to intercept permission calls.
 * Used with RpcEndpointService.registerRPCEndpoint().
 */
public class PermissionRpcInterceptor implements Function<PluginRpcRequest, Object> {

    private final PermissionEventCapture eventCapture;
    private final NodeInfoProvider nodeInfoProvider;
    private final String methodName;
    private final Function<PluginRpcRequest, Object> delegateMethod;

    public PermissionRpcInterceptor(
            PermissionEventCapture eventCapture,
            NodeInfoProvider nodeInfoProvider,
            String methodName,
            Function<PluginRpcRequest, Object> delegateMethod) {
        this.eventCapture = eventCapture;
        this.nodeInfoProvider = nodeInfoProvider;
        this.methodName = methodName;
        this.delegateMethod = delegateMethod;
    }

    @Override
    public Object apply(PluginRpcRequest request) {
        try {
            System.out.println("[RPC] Interceptando: " + methodName);

            Object result = delegateMethod.apply(request);

            captureEvent(request);

            return result;
        } catch (Exception e) {
            System.err.println("[ERROR] En interceptor RPC: " + e.getMessage());
            return delegateMethod.apply(request);
        }
    }

    private void captureEvent(PluginRpcRequest request) {
        Object[] params = request.getParams();
        if (params == null || params.length == 0) return;

        String enode = nodeInfoProvider.getNodeEnode();
        List<String> items = extractItemsFromParams(params);

        if ("perm_addAccountsToAllowlist".equals(methodName)) {
            eventCapture.captureAccountPermissionEvent(enode, items);
        } else if ("perm_addNodesToAllowlist".equals(methodName)) {
            eventCapture.captureNodePermissionEvent(enode, items);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractItemsFromParams(Object[] params) {
        Object first = params[0];
        if (first instanceof String[]) return Arrays.asList((String[]) first);
        if (first instanceof List<?>) return (List<String>) first;
        if (first instanceof java.util.Collection<?>) return new java.util.ArrayList<>((java.util.Collection<String>) first);
        return List.of();
    }

    public String getMethodName() {
        return methodName;
    }
}
