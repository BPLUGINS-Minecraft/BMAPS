package b.bplugins.plugin.maps.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ein Knoten im Navigations-Baum. Plattformunabhängig (kein Bukkit/Fabric-Bezug),
 * damit core sowohl von paper als auch später von fabric/forge/velocity genutzt
 * werden kann.
 *
 * Kann entweder eine Kategorie (mit weiteren Kind-Knoten, egal ob
 * Unterkategorien oder Warps) oder ein Warp-Blatt sein.
 *
 * Icons werden als reiner String-Schlüssel gespeichert (z.B. "PAPER",
 * "COMPASS") - jede Plattform übersetzt diesen Schlüssel selbst in ihr
 * natives Item-System (auf Paper z.B. über Material.valueOf(...)).
 */
public final class MenuNode {

    public enum Type {
        CATEGORY,
        WARP
    }

    public static final String DEFAULT_CATEGORY_ICON = "PAPER";
    public static final String DEFAULT_WARP_ICON = "COMPASS";

    private final String id;
    private Long databaseId;
    private String name;
    private String description;
    private String icon;
    private String permission;
    private final Type type;
    private final List<MenuNode> children = new ArrayList<>();

    // Nur relevant für Type.WARP
    private WarpLocation warpLocation;

    private MenuNode(Type type, String name, String description, String icon) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public static MenuNode category(String name, String description, String icon) {
        return new MenuNode(Type.CATEGORY, name, description, icon != null ? icon : DEFAULT_CATEGORY_ICON);
    }

    public static MenuNode warp(String name, String description, String icon, WarpLocation location) {
        MenuNode node = new MenuNode(Type.WARP, name, description, icon != null ? icon : DEFAULT_WARP_ICON);
        node.warpLocation = location;
        return node;
    }

    public MenuNode addChild(MenuNode child) {
        if (this.type != Type.CATEGORY) {
            throw new IllegalStateException("Nur Kategorien können Kinder haben: " + this.name);
        }
        this.children.add(child);
        return this;
    }

    public String getId() {
        return id;
    }

    public Long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Long databaseId) {
        this.databaseId = databaseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * true, wenn dieser Knoten für JEDEN sichtbar ist (keine Berechtigung nötig).
     */
    public boolean isPublic() {
        return permission == null || permission.isBlank();
    }

    public Type getType() {
        return type;
    }

    public List<MenuNode> getChildren() {
        return children;
    }

    public WarpLocation getWarpLocation() {
        return warpLocation;
    }

    public boolean isCategory() {
        return type == Type.CATEGORY;
    }

    public boolean isWarp() {
        return type == Type.WARP;
    }
}