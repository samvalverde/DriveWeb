/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 *
 * @author vales
 */

public class DriveStorage {
    private static final RuntimeTypeAdapterFactory<FileSystemNode> typeFactory =
    RuntimeTypeAdapterFactory.of(FileSystemNode.class, "type")
        .registerSubtype(FileNode.class, "file")
        .registerSubtype(DirectoryNode.class, "dir");
    
    private static final Gson gson = new GsonBuilder()
    .registerTypeAdapterFactory(typeFactory)
    .setPrettyPrinting()
    .create();
    
    private static Map<String, UserDrive> drives = new HashMap<>();

    public static void save(String username, UserDrive drive) throws IOException {
        File folder = new File("data");
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            System.out.println("¿Carpeta creada? " + created);
        }

        System.out.println("Guardando en archivo: data/" + username + ".json");

        try (FileWriter w = new FileWriter("data/" + username + ".json")) {
            gson.toJson(drive, w);
            System.out.println("Guardado exitosamente.");
        } catch (Exception e) {
            System.out.println("Error al guardar JSON:");
            e.printStackTrace();
        }
    }
    
    public static void cargarDriveUsuario(String username) {
        File jsonFile = new File("data/" + username + ".json");
        if (!jsonFile.exists()) {
            System.out.println("No se encontró el archivo del usuario: " + username);
            return;
        }

        try (Reader reader = new FileReader(jsonFile)) {
            Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(FileSystemNode.class, "type")
                    .registerSubtype(FileNode.class, "file")
                    .registerSubtype(DirectoryNode.class, "directory"))
                .create();
            UserDrive drive = gson.fromJson(reader, UserDrive.class);
            drives.put(username, drive);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void guardarDriveUsuario(String username) {
        UserDrive drive = drives.get(username);
        if (drive == null) return;

        try (Writer writer = new FileWriter("data/" + username + ".json")) {
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(FileSystemNode.class, "type")
                    .registerSubtype(FileNode.class, "file")
                    .registerSubtype(DirectoryNode.class, "directory"))
                .create();
            gson.toJson(drive, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static UserDrive getUserDrive(String username) {
        return drives.get(username);
    }

    public static void saveUserDrive(String username) {
        guardarDriveUsuario(username);
    }

    public static void putUserDrive(String username, UserDrive drive) {
        drives.put(username, drive);
    }
    
    public static UserDrive load(String username) throws IOException {
    try (FileReader r = new FileReader("data/" + username + ".json")) {
        UserDrive drive = gson.fromJson(r, UserDrive.class);
        drive.restoreParents();
        return drive;
    }
}

    public static boolean exists(String username) {
        return new File("data/" + username + ".json").exists();
    }
}

