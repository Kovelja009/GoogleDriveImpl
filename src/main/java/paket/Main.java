package paket;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
//    private static Map<String, String> map = new HashMap<>();
//    private static GDrive gDrive;
//    private static String TYPE_MAP = "src/main/resources/typeMap.json";
////
////    static {
////        try {
////            gDrive = new GDrive();
////        } catch (IOException e) {
////            throw new RuntimeException(e);
////        }
////    }
//
//
//    static boolean existParent(String fileId, Drive service){
//        try {   // return if it`s folder
//            return service.files().get(fileId).execute().getMimeType().equals("application/vnd.google-apps.folder");
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    boolean validParent(String parentID, String name, Drive service){
//        if(existParent(parentID, service))
//            return false;
//        return true;
//    }
//
//    // com.google.api.services.drive.model.File
//    public static final List<File> getGoogleSubFolderByName(String googleFolderIdParent, String subFolderName,Drive driveService)
//            throws IOException {
//
//        String pageToken = null;
//        List<File> list = new ArrayList<File>();
//
//        String query = null;
//        if (googleFolderIdParent == null) {
//            query = " name = '" + subFolderName + "' " //
//                    + " and mimeType = 'application/vnd.google-apps.folder' " //
//                    + " and 'root' in parents";
//        } else {
//            query = " name = '" + subFolderName + "' " //
//                    + " and mimeType = 'application/vnd.google-apps.folder' " //
//                    + " and '" + googleFolderIdParent + "' in parents";
//        }
//
//        do {
//            FileList result = driveService.files().list().setQ(query).setSpaces("drive") //
//                    .setFields("nextPageToken, files(id, name, mimeType, parents, createdTime)")//
//                    .setPageToken(pageToken).execute();
//            list.addAll(result.getFiles());
//            pageToken = result.getNextPageToken();
//        } while (pageToken != null);
//        //
//        return list;
//    }
//
//
//
//    boolean checkPath(String previousId, String name, Drive service){
//        try {   // ukoliko ga pronadje u parentu vraca true
//            return !getGoogleSubFolderByName(previousId, name, service).isEmpty();
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    //TODO koristi ovaj
//    static boolean isValidPath(String path, Drive service){
//        if (path == null || path.equals("") || path.toLowerCase().equals("root"))
//            return true;
//        try {
//            String[] folders = path.split("/");
//            String previous = null;
//            for(int i = 0; i < folders.length; i++){
////                if(!checkPath(previous, folders[i], service))
////                    return false;
//                previous = getGoogleSubFolderByName(previous, folders[i], service).get(0).getId();
//            }
//                return true;
//        }catch (Exception e){
//            System.out.println("Invalid path: " + path);
//            return false;
//        }
//    }
//    // if correct returns file, else null
//    static File getFolderbyPath(String path, Drive service){
//        try {
//            if (path == null || path.equals("") || path.toLowerCase().equals("root"))
//                return service.files().get("root").execute();
//            String[] folders = path.split("/");
//            File previous = service.files().get("root").execute();
//            for (String folder : folders) {
//                previous = getGoogleSubFolderByName(previous.getId(), folder, service).get(0);
//            }
//            return previous;
//        }catch (Exception e){
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    static boolean isNameValid(String googleFolderIdParent, String name, Drive service, String type){
//
//        String mimeType = " and mimeType = '";
//        if(type == null)
//            mimeType = "";
//        else
//            mimeType += type + "' ";
//
//
//        try {
//
//            String pageToken = null;
//            List<File> list = new ArrayList<File>();
//            String query = null;
//                query = " name = '" + name + "' " //
////                        + " and mimeType = '" + MimeType + "' " //
//                        + mimeType
//                        + " and '" + googleFolderIdParent + "' in parents";
//            do {
//                FileList result = service.files().list().setQ(query).setSpaces("drive") //
//                        .setPageToken(pageToken).execute();
//                list.addAll(result.getFiles());
//                pageToken = result.getNextPageToken();
//            } while (pageToken != null);
//            return list.isEmpty();
//        }catch (Exception e){
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    // returns folder or null
//    static File makeFolder(String path, String name, Drive service){
//
//        String realFolderId = null;
//        if(isValidPath(path,service)){
//            realFolderId = getFolderbyPath(path,service).getId();
//            if(!isNameValid(realFolderId, name, service,"application/vnd.google-apps.folder")){
//                System.out.println("Name: " + name + " is not valid!");
//                return null;
//            }
//        }else{
//            System.out.println("Path: " + path + " is not valid!");
//            return null;
//        }
//        try {
//            File fileMetadata = new File();
//            fileMetadata.setName(name);
//            fileMetadata.setMimeType("application/vnd.google-apps.folder");
//            fileMetadata.setParents(List.of(realFolderId));
//            File file = service.files().create(fileMetadata)
//                        .setFields("id, name, parents, mimeType, createdTime")
//                        .execute();
//            ispis(file);
//            System.out.println("-----------------");
//            return file;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//    //TODO make locally -> calls uploadFile -> delete localy
//    static void saveConfig(String rootID, Configuration configuration){
//        try(FileWriter writer = new FileWriter("src/main/resources/config.json")) {
//        Gson gson = new Gson();
//        gson.toJson(configuration, writer);
//        writer.close();
//        java.io.File javaFile = new java.io.File("src/main/resources/config.json");
//        uploadFile(rootID, javaFile);
//        javaFile.delete();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    public static void loadJson(){
//
//        Gson gson = new Gson();
//        try {
//            Reader reader = Files.newBufferedReader(Paths.get(TYPE_MAP));
//            map = gson.fromJson(reader, Map.class);
//
//            System.out.println(map);
//
//        }catch (Exception e){
//            System.out.println("Greska pri ucitavanju mape");
//        }
//    }
//
//
//
//    //////////////
////    Uploading file
//
//
//    public static File uploadFile(String parentID, java.io.File file){
//        File driveFile = null;
//        try {
//            File fileMetadata = new File();
//            fileMetadata.setName(file.getName());
//            fileMetadata.setParents(List.of(parentID));
//
//            FileContent fileContent = new FileContent(map.get(file.getName().substring(file.getName().lastIndexOf("."))), file);
//            driveFile = gDrive.getDrive().files().create(fileMetadata, fileContent)
//                    .setFields("id, name, parents, mimeType, createdTime")
//                    .execute();
//            ispis(driveFile);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return driveFile;
//    }
//
//    public static void upload(String path, String destination)throws IOException{
//        java.io.File javaFile = null;
//        try {
//            javaFile = new java.io.File(path);
//
//        }catch (Exception e){
//            System.out.println("Ne postojeci file!");
//            return;
//        }
//
//        File gFile = getFolderbyPath(destination, gDrive.getDrive());
//        if(gFile == null){
//            System.out.println("Greska na drive path-u");
//            return;
//        }
//        String parentID = gFile.getId();
//        String name = javaFile.getName();
//        if(!isNameValid(parentID, name, gDrive.getDrive(),map.get(name.substring(name.lastIndexOf("."))))){
//            System.out.println("Nevalidno imee!");
//            return;
//        }
//        uploadFile(parentID, javaFile);
//
//    }




    /////////////

    static void ispis(File f){
        System.out.println("Name: " + f.getName() + "   Id: " + f.getId() + "   Parents: " + f.getParents() + " Type: " + f.getMimeType() + "   CreatedTime: " + f.getCreatedTime());
    }

    public static void main(String[] args) {
        try {
            GoogleDriveImpl gdg = new GoogleDriveImpl();
            System.out.println(gdg.createRoot("", "ROot", new Configuration()));

            //TODO sacuvaj liniju !!!
//            System.out.println(service.files().get("root").execute().getId());

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
