/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;

import model.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author vales
 */


@WebServlet("/api/command")
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getParameter("action");
        String username = req.getParameter("user");

        switch (action) {
            case "createDrive": {
                if (DriveStorage.exists(username)) {
                    resp.getWriter().write("Ya existe un drive para este usuario. No se puede sobrescribir.");
                } else {
                    long quota = Long.parseLong(req.getParameter("quota"));
                    UserDrive drive = new UserDrive(username, quota);
                    sessions.put(username, drive);
                    DriveStorage.save(username, drive);
                    resp.getWriter().write("Drive creado exitosamente.");
                }
                break;
            }
            case "createDir": {
                String dirName = req.getParameter("name");
                UserDrive drive = getDrive(username);
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
                if (drive != null && drive.hasSpaceFor(content.length())) {
                    if (drive.getCurrent().getChild(name) != null) {
                        resp.getWriter().write("Archivo ya existe.");
                        return;
                    }
                    drive.getCurrent().addChild(new FileNode(name, content));
                    DriveStorage.save(username, drive);
                    resp.getWriter().write("Archivo creado");
                } else {
                    resp.getWriter().write("No hay espacio disponible");
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
                    FileSystemNode node = drive.getCurrent().getChild(name);
                    if (node != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Nombre: ").append(node.getName()).append("\n")
                          .append("Tipo: ").append(node.isDirectory() ? "Directorio" : "Archivo").append("\n")
                          .append("Creado: ").append(node.getCreationDate()).append("\n")
                          .append("Modificado: ").append(node.getModifiedDate());
                        if (node instanceof FileNode)
                            sb.append("\nTamaño: ").append(((FileNode) node).getSize()).append(" bytes");
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
                    try {
                        drive.copyTo(source, target);
                        DriveStorage.save(username, drive);
                        resp.getWriter().write("Copiado");
                    } catch (IllegalArgumentException e) {
                        resp.getWriter().write("Error: " + e.getMessage());
                    }
                }
                break;
            }
            case "move": {
                String source = req.getParameter("source");
                String target = req.getParameter("target");
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    try {
                        drive.moveTo(source, target);
                        DriveStorage.save(username, drive);
                        resp.getWriter().write("Movido");
                    } catch (IllegalArgumentException e) {
                        resp.getWriter().write("Error: " + e.getMessage());
                    }
                }
                break;
            }
            case "share": {
                String source = req.getParameter("source");
                String targetUser = req.getParameter("target");

                UserDrive drive = getDrive(username);
                UserDrive targetDrive = getDrive(targetUser);

                if (drive == null || targetDrive == null) {
                    resp.getWriter().write("Uno de los usuarios no tiene drive activo.");
                    break;
                }

                FileSystemNode node = drive.getCurrent().getChild(source);
                if (node == null) {
                    resp.getWriter().write("Elemento a compartir no encontrado.");
                    break;
                }

                if (targetDrive.getShared().getChild(node.getName()) != null) {
                    resp.getWriter().write("Ya existe un archivo/directorio con ese nombre en 'shared'.");
                    break;
                }

                FileSystemNode copia = drive.deepCopy(node);
                targetDrive.getShared().addChild(copia);
                DriveStorage.save(targetUser, targetDrive);

                resp.getWriter().write("Compartido exitosamente a " + targetUser);
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
            case "usage": {
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    long used = drive.getUsedSpace();
                    long quota = drive.hasSpaceFor(0) ? drive.getQuota() : used; // fallback si no hay getter
                    resp.getWriter().write("Espacio usado: " + used + "/" + quota + " bytes");
                }
                break;
            }
            case "download": {
                String name = req.getParameter("name");
                UserDrive drive = getDrive(username);
                if (drive != null) {
                    FileSystemNode node = drive.getCurrent().getChild(name);
                    if (node instanceof FileNode) {
                        resp.setContentType("text/plain");
                        resp.setHeader("Content-Disposition", "attachment;filename=" + name);
                        resp.getWriter().write(((FileNode) node).getContent());
                    } else {
                        resp.setContentType("text/plain");
                        resp.getWriter().write("No es un archivo válido.");
                    }
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