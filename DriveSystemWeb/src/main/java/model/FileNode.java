/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.util.Date;

/**
 *
 * @author vales
 */

public class FileNode extends FileSystemNode {
    private String content;

    public FileNode(String name, String content) {
        super(name);
        this.content = content;
        this.type = "file"; // importante para JSON
        updateModified();
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public long getSize() {
        return content != null ? content.length() : 0;
    }

    public void setContent(String content) {
        this.content = content;
        updateModified();
    }
    
    @Override
    public FileSystemNode deepCopy() {
        return new FileNode(getName(), getContent());
    }

}

