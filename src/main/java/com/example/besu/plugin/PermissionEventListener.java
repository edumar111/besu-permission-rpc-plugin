package com.example.besu.plugin;

/**
 * Interfaz para escuchar cambios en permisos
 */
@FunctionalInterface
public interface PermissionEventListener {
    /**
     * Se invoca cuando cambia un permiso
     * @param event El evento capturado
     */
    void onPermissionChanged(PermissionEvent event);
}
