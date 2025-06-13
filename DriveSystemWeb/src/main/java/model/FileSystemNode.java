/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author vales
 */

public abstract class FileSystemNode {
    protected String name;
    protected transient DirectoryNode parent;
    protected String creationDate;
    protected String modifiedDate;
    public String type; // requerido por RuntimeTypeAdapterFactory

    public FileSystemNode(String name) {
        this.name = name;
        this.creationDate = now();
        this.modifiedDate = this.creationDate;
    }

    public String getName() { return name; }

    public void setParent(DirectoryNode parent) {
        this.parent = parent;
    }

    public DirectoryNode getParent() { return parent; }

    public String getCreationDate() { return creationDate; }

    public String getModifiedDate() { return modifiedDate; }

    public void updateModified() {
        this.modifiedDate = now();
    }

    public abstract boolean isDirectory();

    public abstract long getSize();

    public String getPath() {
        if (parent == null) return "/" + name;
        return parent.getPath() + "/" + name;
    }

    private String now() {
        return new SimpleDateFormat("MMM dd, yyyy, h:mm:ss a").format(new Date());
    }
}
