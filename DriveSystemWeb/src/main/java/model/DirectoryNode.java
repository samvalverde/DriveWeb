/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.util.*;

/**
 *
 * @author vales
 */

public class DirectoryNode extends FileSystemNode {
    private final List<FileSystemNode> children = new ArrayList<>();

    public DirectoryNode(String name) {
        super(name);
        this.type = "dir"; // para la deserialización
    }


    @Override
    public boolean isDirectory() {
        return true;
    }

    public List<FileSystemNode> getChildren() {
        return children;
    }

    public void addChild(FileSystemNode node) {
        node.setParent(this);
        children.add(node);
    }

    public void removeChild(FileSystemNode node) {
        children.remove(node);
    }

    public FileSystemNode getChild(String name) {
        for (FileSystemNode child : children) {
            if (child.getName().equals(name)) return child;
        }
        return null;
    }

    public long getSize() {
        long size = 0;
        for (FileSystemNode child : children) {
            if (child instanceof FileNode) size += ((FileNode) child).getSize();
            else if (child instanceof DirectoryNode) size += ((DirectoryNode) child).getSize();
        }
        return size;
    }
    
    @Override
    public FileSystemNode deepCopy() {
        DirectoryNode copia = new DirectoryNode(getName());
        for (FileSystemNode child : children) {
            FileSystemNode childCopy = child.deepCopy();
            copia.addChild(childCopy);
        }
        return copia;
    }

}

