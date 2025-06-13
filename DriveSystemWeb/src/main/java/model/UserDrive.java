/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author vales
 */

public class UserDrive {
    private String username;
    private long quota;
    private DirectoryNode root;
    private DirectoryNode shared;
    private DirectoryNode current;

    public UserDrive(String username, long quota) {
        this.username = username;
        this.quota = quota;
        this.root = new DirectoryNode("root");
        this.shared = new DirectoryNode("shared");
        this.current = root;
    }

    public synchronized DirectoryNode getCurrent() {
        return current;
    }

    public synchronized boolean changeDirectory(String name) {
        if ("..".equals(name) && current.getParent() != null) {
            current = current.getParent();
            return true;
        } else {
            FileSystemNode node = current.getChild(name);
            if (node instanceof DirectoryNode) {
                current = (DirectoryNode) node;
                return true;
            }
        }
        return false; // no cambió
    }

    public synchronized void resetToRoot() {
        current = root;
    }

    public synchronized long getUsedSpace() {
        return root.getSize() + shared.getSize();
    }

    public synchronized boolean hasSpaceFor(long size) {
        return getUsedSpace() + size <= quota;
    }

    public synchronized boolean nameExists(String name) {
        return current.getChild(name) != null;
    }

    public synchronized void copyTo(String source, String target) {
        FileSystemNode src = current.getChild(source);
        FileSystemNode tgt = current.getChild(target);

        if (src == null) throw new IllegalArgumentException("Elemento de origen no encontrado.");
        if (!(tgt instanceof DirectoryNode)) throw new IllegalArgumentException("Destino no es un directorio.");
        if (((DirectoryNode) tgt).getChild(src.getName()) != null)
            throw new IllegalArgumentException("Ya existe un archivo/directorio con ese nombre en el destino.");

        ((DirectoryNode) tgt).addChild(deepCopy(src));
    }
    
    public FileSystemNode deepCopy(FileSystemNode node) {
        if (node instanceof FileNode) {
            return new FileNode(node.getName(), ((FileNode) node).getContent());
        } else if (node instanceof DirectoryNode) {
            DirectoryNode copyDir = new DirectoryNode(node.getName());
            for (FileSystemNode child : ((DirectoryNode) node).getChildren()) {
                copyDir.addChild(deepCopy(child));
            }
            return copyDir;
        }
        return null;
    }

    public synchronized void moveTo(String source, String target) {
        FileSystemNode src = current.getChild(source);
        FileSystemNode tgt = current.getChild(target);

        if (src == null) throw new IllegalArgumentException("Elemento de origen no encontrado.");
        if (!(tgt instanceof DirectoryNode)) throw new IllegalArgumentException("Destino no es un directorio.");
        if (((DirectoryNode) tgt).getChild(src.getName()) != null)
            throw new IllegalArgumentException("Ya existe un archivo/directorio con ese nombre en el destino.");

        current.removeChild(src);
        ((DirectoryNode) tgt).addChild(src);
    }


    public void restoreParents() {
        assignParents(root, null);
        assignParents(shared, null);
        if (current != null) {
            String currentPath = current.getPath();
            DirectoryNode found = findNodeByPath(currentPath, root);
            if (found != null) current = found;
            else current = root; // fallback
        }
    }

    private void assignParents(DirectoryNode dir, DirectoryNode parent) {
        dir.setParent(parent);
        for (FileSystemNode child : dir.getChildren()) {
            if (child instanceof DirectoryNode) {
                assignParents((DirectoryNode) child, dir);
            } else {
                child.setParent(dir);
            }
        }
    }

    private DirectoryNode findNodeByPath(String path, DirectoryNode dir) {
        if (dir.getPath().equals(path)) return dir;
        for (FileSystemNode child : dir.getChildren()) {
            if (child instanceof DirectoryNode) {
                DirectoryNode result = findNodeByPath(path, (DirectoryNode) child);
                if (result != null) return result;
            }
        }
        return null;
    }
    public synchronized long getQuota() { return quota; }
    public DirectoryNode getRoot() { return root; }
    public DirectoryNode getShared() { return shared; }
    public String getUsername() { return username; }
}

