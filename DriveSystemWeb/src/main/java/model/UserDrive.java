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
    private long espacioTotal;
    private DirectoryNode directorioRaiz;
    private DirectoryNode carpetaCompartidos;
    private DirectoryNode directorioActual;

    public UserDrive(String username, long quota) {
        this.username = username;
        this.quota = quota;
        this.root = new DirectoryNode("root");
        this.shared = new DirectoryNode("shared");
        this.current = root;
    }

    public String getCurrentPath() {
    return current.getPath();
    }

    public synchronized DirectoryNode getCurrent() {
        return current;
    }

    public synchronized boolean changeDirectory(String name) {
        if ("..".equals(name) && current.getParent() != null) {
            current = current.getParent();
            return true;
        } else if ("shared".equals(name)) {
            current = shared;
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

    public long calcularEspacioUsado() {
        return calcularEspacioUsadoRecursivo(directorioRaiz);
    }

    private long calcularEspacioUsadoRecursivo(DirectoryNode dir) {
        long total = 0;
        for (FileSystemNode node : dir.getChildren()) {
            if (node instanceof FileNode) {
                total += ((FileNode) node).getSize();
            } else if (node instanceof DirectoryNode) {
                total += calcularEspacioUsadoRecursivo((DirectoryNode) node);
            }
        }
        return total;
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
        assignParents(shared, root);
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
    
    public boolean loadFile(String fileName, String content) {
        long fileSize = content.getBytes().length;
        long espacioDisponible = espacioTotal - calcularEspacioUsado();

        if (fileSize > espacioDisponible) {
            return false; // No hay espacio suficiente
        }

        String nombreSinExtension = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1) : "";

        for (FileSystemNode node : directorioActual.getChildren()) {
            if (node instanceof FileNode && node.getName().equals(nombreSinExtension)) {
                return false; // Ya existe
            }
        }

        FileNode nuevoArchivo = new FileNode(nombreSinExtension, extension);
        nuevoArchivo.setContent(content);
        nuevoArchivo.setParent(directorioActual);
        directorioActual.addChild(nuevoArchivo);
        return true;
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
    
    public boolean copiarArchivo(String nombreArchivo, String rutaDestino) {
        FileNode archivo = buscarArchivo(nombreArchivo, directorioActual);
        if (archivo == null) return false;

        DirectoryNode destino = buscarDirectorioPorRuta(rutaDestino);
        if (destino == null) return false;

        // Verificar duplicado
        for (FileSystemNode nodo : destino.getChildren()) {
            if (nodo instanceof FileNode && nodo.getName().equals(archivo.getName())) {
                return false;
            }
        }

        long tamaño = archivo.getContent().getBytes().length;
        long espacioDisponible = espacioTotal - calcularEspacioUsado();
        if (tamaño > espacioDisponible) return false;

        // Crear copia
        FileNode copia = new FileNode(archivo.getName(), archivo.getExtension());
        copia.setContent(archivo.getContent());
        copia.setParent(destino);
        destino.addChild(copia);
        return true;
    }

    public boolean moverNodo(String nombreNodo, String rutaDestino) {
        FileSystemNode nodo = buscarNodo(nombreNodo, directorioActual);
        if (nodo == null) return false;

        DirectoryNode destino = buscarDirectorioPorRuta(rutaDestino);
        if (destino == null || destino == nodo) return false;

        // Evitar duplicado
        for (FileSystemNode hijo : destino.getChildren()) {
            if (hijo.getName().equals(nodo.getName())) {
                return false;
            }
        }

        // Eliminar del padre actual
        if (nodo.getParent() != null) {
            nodo.getParent().removeChild(nodo);
        }

        // Mover
        nodo.setParent(destino);
        destino.addChild(nodo);
        return true;
    }

    private FileNode buscarArchivo(String nombre, DirectoryNode dir) {
        for (FileSystemNode nodo : dir.getChildren()) {
            if (nodo instanceof FileNode && nodo.getName().equals(nombre)) {
                return (FileNode) nodo;
            }
        }
        return null;
    }

    private FileSystemNode buscarNodo(String nombre, DirectoryNode dir) {
        for (FileSystemNode nodo : dir.getChildren()) {
            if (nodo.getName().equals(nombre)) {
                return nodo;
            }
        }
        return null;
    }

    private DirectoryNode buscarDirectorioPorRuta(String ruta) {
        String[] partes = ruta.split("/");
        DirectoryNode actual = directorioRaiz;
        for (String parte : partes) {
            if (parte.isEmpty()) continue;
            boolean encontrado = false;
            for (FileSystemNode nodo : actual.getChildren()) {
                if (nodo instanceof DirectoryNode && nodo.getName().equals(parte)) {
                    actual = (DirectoryNode) nodo;
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) return null;
        }
        return actual;
    }
    
    public synchronized long getQuota() { return quota; }
    public DirectoryNode getRoot() { return root; }
    public DirectoryNode getShared() { return shared; }
    public String getUsername() { return username; }
}

