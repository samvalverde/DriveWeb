/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import model.DirectoryNode;
import model.DriveStorage;
import model.FileNode;
import model.FileSystemNode;
import model.UserDrive;

/**
 *
 * @author vales
 */


@WebServlet("/api/command")
@MultipartConfig
public class CommandServlet extends HttpServlet {
    private final Map<String, UserDrive> sessions = new HashMap<>();

    private synchronized UserDrive getDrive(String username) throws IOException {
        if (sessions.containsKey(username)) return sessions.get(username);
        if (DriveStorage.exists(username)) {
            UserDrive d = DriveStorage.load(username);
            sessions.put(username, d);
            return d;
        }
        return null;
    }

    private String obtenerExtension(String nombre) {
        int punto = nombre.lastIndexOf(".");
        return punto != -1 ? nombre.substring(punto + 1) : "(sin extensión)";
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String action = req.getParameter("action");
        String username = req.getParameter("user").toLowerCase();

        switch (action) {
            case "login": {
                username = username.toLowerCase();
                if (DriveStorage.exists(username)) {
                    resp.getWriter().write("OK");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Usuario no registrado");
                }
                break;
            }
            case "createDrive": {
                if (DriveStorage.exists(username)) {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT); // 409
                    resp.getWriter().write("⚠️ Ya existe un drive para este usuario.");
                    return;
                }

                long quota = Long.parseLong(req.getParameter("quota"));
                UserDrive drive = new UserDrive(username.toLowerCase(), quota);
                sessions.put(username.toLowerCase(), drive);
                DriveStorage.save(username.toLowerCase(), drive);
                resp.getWriter().write("Drive creado exitosamente.");
                break;
            }
            case "createDir": {
                String dirName = req.getParameter("name");
                UserDrive drive = getDrive(username);

                if ("shared".equalsIgnoreCase(dirName)) {
                    resp.getWriter().write("El nombre 'shared' esta reservado.");
                    return;
                }

                if (drive != null) {
                    if (drive.getCurrent().getChild(dirName) != null) {
                        resp.getWriter().write("Ya existe un archivo/directorio con ese nombre.");
                        return;
                    }
                    drive.getCurrent().addChild(new DirectoryNode(dirName));
                    DriveStorage.save(username, drive);
                    resp.getWriter().write("Directorio creado");
                }
                break;
            }
            case "createFile": {
                String name = req.getParameter("name");
                String content = req.getParameter("content");

                UserDrive drive = getDrive(username);
                if (drive != null) {

                    if (name == null || name.trim().isEmpty()) {
                        resp.getWriter().write("El nombre del archivo es obligatorio.");
                        return;
                    }

                    // Solo permitir .txt (sin distinguir mayúsculas/minúsculas)
                    if (!name.toLowerCase().endsWith(".txt")) {
                        resp.getWriter().write("Solo se permiten archivos con extensión .txt");
                        return;
                    }

                    // Validar que no exista ya un archivo o directorio con ese nombre
                    if (drive.getCurrent().getChild(name) != null) {
                        resp.getWriter().write("Ya existe un archivo o directorio con ese nombre.");
                        return;
                    }

                    // Validar espacio
                    if (!drive.hasSpaceFor(content != null ? content.length() : 0)) {
                        resp.getWriter().write("No hay espacio suficiente para crear el archivo.");
                        return;
                    }

                    // Crear archivo (si no hay contenido, ponerlo vacío)
                    drive.getCurrent().addChild(new FileNode(name, content != null ? content : ""));
                    DriveStorage.save(username, drive);
                    resp.getWriter().write("Archivo creado exitosamente.");
                }
                break;
            }
            case "viewFile": {
                String name = req.getParameter("name");
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    FileSystemNode node = drive.getCurrent().getChild(name);
                    if (node instanceof FileNode) {
                        resp.getWriter().write(((FileNode) node).getContent());
                    } else {
                        resp.getWriter().write("No es un archivo válido.");
                    }
                }
                break;
            }
            case "listDir": {
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    StringBuilder sb = new StringBuilder();
                    for (FileSystemNode node : drive.getCurrent().getChildren()) {
                        sb.append((node instanceof DirectoryNode ? "[DIR] " : "[FILE] "))
                          .append(node.getName()).append("\n");
                    }
                    // Mostrar la carpeta compartida si estamos en root
                    if (drive.getCurrent() == drive.getRoot() && drive.getRoot().getChild("shared") == null) {
                        sb.append("[DIR] shared\n");
                    }

                    resp.getWriter().write(sb.toString());
                }
                break;
            }
            case "delete": {
                String name = req.getParameter("name");
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    FileSystemNode node = drive.getCurrent().getChild(name);
                    if (node != null) {
                        drive.getCurrent().removeChild(node);
                        DriveStorage.save(username, drive);
                        resp.getWriter().write("Eliminado");
                    } else {
                        resp.getWriter().write("No se encontró el elemento");
                    }
                }
                break;
            }
            case "cd": {
                String name = req.getParameter("name");
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    boolean changed = drive.changeDirectory(name);
                    if (changed) {
                        DriveStorage.save(username, drive); 
                        resp.getWriter().write("Directorio cambiado a: " + drive.getCurrent().getName());
                    } else {
                        resp.getWriter().write("El directorio '" + name + "' no existe en el directorio actual.");
                    }
                }
                break;
            }
            case "properties": {
                String name = req.getParameter("name");
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    FileSystemNode node;

                    if ("shared".equalsIgnoreCase(name) && drive.getCurrent() == drive.getRoot()) {
                        node = drive.getShared(); // acceso especial
                    } else {
                        node = drive.getCurrent().getChild(name);
                    }

                    if (node != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Nombre: ").append(node.getName()).append("\n")
                          .append("Tipo: ").append(node.isDirectory() ? "Directorio" : "Archivo").append("\n")
                          .append("Creado: ").append(node.getCreationDate()).append("\n")
                          .append("Modificado: ").append(node.getModifiedDate());

                        if (node instanceof FileNode)
                            sb.append("\nTamaño: ").append(((FileNode) node).getSize()).append(" bytes");
                        else if (node instanceof DirectoryNode)
                            sb.append("\nTamaño: ").append(((DirectoryNode) node).getSize()).append(" bytes");

                        resp.getWriter().write(sb.toString());
                    } else {
                        resp.getWriter().write("No encontrado");
                    }
                }
                break;
            }
            case "copy": {
                String source = req.getParameter("source");
                String target = req.getParameter("target");
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    boolean ok = drive.copiar(source, target);
                    if (ok) {
                        DriveStorage.save(username, drive);
                        resp.getWriter().write("Copia realizada con éxito.");
                    } else {
                        resp.getWriter().write("Error al copiar: ruta incorrecta o duplicado.");
                    }
                }
                break;
            }
            case "move": {
                String source = req.getParameter("source");
                String target = req.getParameter("target");
                UserDrive drive = getDrive(username);

                if (drive != null) {
                    // Evita mover la carpeta 'shared' (insensible a mayúsculas/minúsculas)
                    if (source.equalsIgnoreCase("shared") || source.equalsIgnoreCase("/shared")) {
                        resp.getWriter().write("Error al mover: la carpeta 'shared' no se puede mover.");
                        break;
                    }
                    
                    // Validar espacio antes de mover si es archivo
                    FileSystemNode origen = drive.getNodeByPath(source);
                    if (origen instanceof FileNode) {
                        long size = ((FileNode) origen).getSize();
                        if (!drive.hasSpaceFor(size)) {
                            resp.getWriter().write("Error al mover: no hay suficiente espacio disponible.");
                            break;
                        }
                    }
                    
                    boolean ok = drive.mover(source, target);
                    if (ok) {
                        DriveStorage.save(username, drive);
                        resp.getWriter().write("Movimiento realizado con éxito.");
                    } else {
                        resp.getWriter().write("Error al mover: ruta incorrecta o duplicado.");
                    }
                }
                break;
            }
            case "load": {
                try {
                    Part filePart = req.getPart("file");
                    String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

                    // Validar que sea un archivo de texto
                    if (!fileName.toLowerCase().endsWith(".txt")) {
                        resp.getWriter().write("Solo se permiten archivos de texto (.txt)");
                        return;
                    }

                    // Leer el contenido del archivo
                    InputStream fileContent = filePart.getInputStream();
                    String content = new BufferedReader(new InputStreamReader(fileContent))
                            .lines().collect(Collectors.joining("\n"));

                    // Obtener el drive del usuario
                    UserDrive drive = getDrive(username);
                    if (drive == null) {
                        resp.getWriter().write("Usuario no tiene un drive cargado.");
                        return;
                    }

                    // Validar duplicado y espacio
                    if (drive.getCurrent().getChild(fileName) != null) {
                        resp.getWriter().write("Ya existe un archivo o directorio con ese nombre.");
                        return;
                    }

                    if (!drive.hasSpaceFor(content.length())) {
                        resp.getWriter().write("No hay espacio suficiente para guardar el archivo.");
                        return;
                    }

                    // Crear y guardar el archivo
                    drive.getCurrent().addChild(new FileNode(fileName, content));
                    DriveStorage.save(username, drive);
                    resp.getWriter().write("Archivo de texto cargado exitosamente.");

                } catch (Exception e) {
                    e.printStackTrace();
                    resp.getWriter().write("Error al cargar el archivo.");
                }
                break;
            }
            case "share": {
                String name = req.getParameter("name");
                String fromUser = req.getParameter("user").toLowerCase();
                String targetUser = req.getParameter("target").toLowerCase();

                UserDrive fromDrive = getDrive(fromUser);
                UserDrive toDrive = getDrive(targetUser);

                if (fromDrive != null && toDrive != null) {
                    FileSystemNode node = fromDrive.getCurrent().getChild(name);
                    if (node != null) {
                        FileSystemNode copia = node.deepCopy();
                        toDrive.getShared().addChild(copia);

                        DriveStorage.save(targetUser, toDrive);
                        resp.getWriter().write("Compartido con éxito");
                    } else {
                        resp.getWriter().write("No se encontró el archivo o carpeta para compartir.");
                    }
                } else {
                    resp.getWriter().write("Usuario de origen o destino no válido.");
                }
                break;
            }
            case "editFile": {
                String name = req.getParameter("name");
                String newContent = req.getParameter("content");

                UserDrive drive = getDrive(username);
                if (drive != null) {
                    FileSystemNode node = drive.getCurrent().getChild(name);
                    if (node instanceof FileNode) {
                        ((FileNode) node).setContent(newContent);
                        DriveStorage.save(username, drive);
                        resp.getWriter().write("Archivo actualizado.");
                    } else {
                        resp.getWriter().write("El archivo no existe o no es un archivo válido.");
                    }
                }
                break;
            }
            case "pwd": {
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    resp.getWriter().write(drive.getCurrentPath());
                } else {
                    resp.getWriter().write("/");
                }
                break;
            }
            
            case "usage": {
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    long used = drive.getUsedSpace();
                    long quota = drive.hasSpaceFor(0) ? drive.getQuota() : used; // fallback si no hay getter
                    resp.getWriter().write("Espacio usado: " + used + "/" + quota + " bytes");
                }
                break;
            }
            default: {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Comando no válido.");
            }
        }
    }
}