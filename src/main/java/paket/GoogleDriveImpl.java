package paket;

import Data.MyFile;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

//TODO postaviti kao GitHubPackage
//TODO postaviti scope runtime
//TODO skloniti getter
@Getter
public class GoogleDriveImpl extends FileManager{
    private GDrive gDrive;
    private Drive service;
    private static String TYPE_MAP = "src/main/resources/typeMap.json";
    private static Map<String, String> map = new HashMap<>();

    public GoogleDriveImpl(){
        setRootPath("");
        try {
            gDrive = new GDrive();
            service = gDrive.getDrive();
        }catch (Exception e){
            e.printStackTrace();
        }

        Gson gson = new Gson();
        try {
            Reader reader = Files.newBufferedReader(Paths.get(TYPE_MAP));
            map = gson.fromJson(reader, Map.class);
            System.out.println(map);

        }catch (Exception e){
            System.out.println("Greska pri ucitavanju mape");
        }

    }

    @Override
    public boolean createRoot(String path, String name, Configuration configuration)throws IOException {
        if(rootMaking(path,name) == null)
            return false;
        String rootPath;
        if(path.equals(""))
            rootPath = name;
        else
            rootPath = path+"/"+name;

        setRootPath(rootPath);
        setConfiguration(configuration);
        saveConfig(rootPath);
        return true;
    }

    @Override
    protected boolean checkConfig(String parentID, String ext, long size) {
        try {
            String query = "'" + parentID + "' in parents";
            List<File> children = service.files().list().setQ(query).setSpaces("drive")
                    .setFields("files(id, name, size)")
                    .execute().getFiles();

            long sum = 0L;
            for(File file : children){
                if(file.getSize() != null)
                    sum+= file.getSize();
            }
            if(1 + children.size() > getConfiguration().getFile_n()){
                System.out.println("Max number of files in a folder is: " + getConfiguration().getFile_n());
                return false;
            }
            if(sum + size > getConfiguration().getSize()){
                System.out.println("There is not enough space in folder! Free: "+ (getConfiguration().getSize() - sum) + ", Your file: " + size);
                return false;
            }
            if(ext.equals(""))
                return true;
            else
                return !getConfiguration().getExcludedExt().contains(ext.toLowerCase());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean mkdir(String path, String name) {
        String realFolderId = null;
        path = getFullPath(path);
        if(isValidPath(path)){
            File par =  getFolderbyPath(path);
            realFolderId = par.getId();
            if(!isNameValid(realFolderId, name, "application/vnd.google-apps.folder")){
                System.out.println("Name: " + name + " is not valid!");
                return false;
            }
            if(!checkConfig(par.getId(), "", 0)){
                System.out.println("Please check config before trying to make: " + name);
                return false;
            }
        }else{
            System.out.println("Path: " + path + " is not valid!");
            return false;
        }
        try {
            File fileMetadata = new File();
            fileMetadata.setName(name);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            fileMetadata.setParents(List.of(realFolderId));

            File file = service.files().create(fileMetadata)
                    .setFields("id, name, parents, mimeType, createdTime, size")
                    .execute();
            std_out(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(String path) {
        path = getFullPath(path);
        if(!isValidPath(path)){
            System.out.println("Invalid path: " + path);
            return false;
        }
        try {
            File file = getFolderbyPath(path);
            service.files().delete(file.getId()).execute();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean move(String oldPath, String newPath) {
        oldPath = getFullPath(oldPath);
        newPath = getFullPath(newPath);

        if(!isValidPath(oldPath)){
            return false;
        }
        if(!isValidPath(newPath)){
            return false;
        }

        File destination = getFolderbyPath(newPath);
        if(!destination.getMimeType().equals("application/vnd.google-apps.folder")){
            System.out.println("Invalid destination file path: " + newPath);
            return false;
        }
        File sourceFile = getFolderbyPath(oldPath);
        String oldParentID = sourceFile.getParents().get(0);
        String newParentID = destination.getId();

        try {
            service.files().update(sourceFile.getId(), null)
                    .setRemoveParents(oldParentID)
                    .setAddParents(newParentID)
                    .execute();

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean download(String item, String destination) {
        item = getFullPath(item);
        if(!isValidPath(item))
            return false;
        File file = getFolderbyPath(item);
        destination = destination + "/" + item.substring(item.lastIndexOf("/")+1);
        return downloadFile(file.getId(), destination);
    }

    @Override
    public boolean upload(String item, String destination) {
        try {
            java.io.File javaFile = new java.io.File(item);
            destination = getFullPath(destination);
            if(!isValidPath(destination))
                return false;

            File parent = getFolderbyPath(destination);

            String ext = getExtension(javaFile.getName());
            String type = map.get(ext);
            if(!isNameValid(parent.getId(), javaFile.getName(), type)){
                System.out.println("Name is not valid!");
                return false;
            }

            if(!checkConfig(parent.getId(), ext, Files.size(Paths.get(javaFile.getPath())))){
                System.out.println("Please check config!File tried to upload: " + javaFile.getPath());
                return false;
            }

            uploadFile(parent.getId(), javaFile);

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean rename(String path, String newName) {
        path = getFullPath(path);
        if(!isValidPath(path))
            return false;

        File file = getFolderbyPath(path);

        if(!isNameValid(file.getParents().get(0),newName,file.getMimeType()))
            return false;
        try {
            File newFile = new File();
            newFile.setName(newName);
            service.files().update(file.getId(), newFile)
                    .execute();
            System.out.println(file.getName());
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
//    TODO obrisati iz repo-a ako postoji i onda upload novu
//    TODO dodati na zatvaranju
    public void saveConfig(String path) {
        try(FileWriter writer = new FileWriter("src/main/resources/config.json")) {
            Gson gson = new Gson();
            gson.toJson(getConfiguration(), writer);
            writer.close();
            java.io.File javaFile = new java.io.File("src/main/resources/config.json");
            String rootID = getFolderbyPath(path).getId();
            uploadFile(rootID, javaFile);
            javaFile.delete();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public List<MyFile> searchDir(String s) {
        return null;
    }

    @Override
    public List<MyFile> searchSubDir(String s) {
        return null;
    }

    @Override
    public List<MyFile> filterByExt(String s, String s1) {
        return null;
    }

    @Override
    public List<MyFile> searchSubstring(String s) {
        return null;
    }

    @Override
    public boolean existName(String s, String s1) {
        return false;
    }

    @Override
    public List<String> getParentPath(String s) {
        return null;
    }

    @Override
    public List<MyFile> sortBy(List<MyFile> list, Metadata metadata) {
        return null;
    }

    @Override
    public List<String> filterData(List<MyFile> list, List<Metadata> list1) {
        return null;
    }

    @Override
    public List<MyFile> filterByPeriod(String s, LocalDateTime date, LocalDateTime date1, boolean b) {
        return null;
    }

    static void std_out(File f){
        System.out.println("Name: " + f.getName() + "   Id: " + f.getId() + "   Parents: " + f.getParents() + " Type: " + f.getMimeType() + "   CreatedTime: " + f.getCreatedTime() + " Size: " + f.getSize());
        System.out.println("-----------------");

    }

    boolean isValidPath(String path){
        if (path == null || path.equals(""))
            return true;
        try {
            String[] folders = path.split("/");
            String previous = null;
            for(int i = 0; i < folders.length; i++){
                if(i == folders.length-1 && folders[i].equals(""))
                    return true;
                if(folders[i].equals(""))
                    return false;
                previous = getGoogleSubFileByName(previous, folders[i]).get(0).getId();
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Invalid path: " + path);
            return false;
        }
    }

    // com.google.api.services.drive.model.File
    private List<File> getGoogleSubFileByName(String googleFolderIdParent, String subFileName)
            throws IOException {

        String pageToken = null;
        List<File> list = new ArrayList<File>();

        String query = null;
        if (googleFolderIdParent == null) {
            query = " name = '" + subFileName + "' " //
//                    + " and mimeType = 'application/vnd.google-apps.folder' " //
                    + " and 'root' in parents";
        } else {
            query = " name = '" + subFileName + "' " //
//                    + " and mimeType = 'application/vnd.google-apps.folder' " //
                    + " and '" + googleFolderIdParent + "' in parents";
        }

        do {
            FileList result = service.files().list().setQ(query).setSpaces("drive") //
                    .setFields("nextPageToken, files(id, name, mimeType, parents, createdTime, size)")//
                    .setPageToken(pageToken).execute();
            list.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        //
        return list;
    }

    // if correct returns file, else null
    File getFolderbyPath(String path){
        try {

            if (path == null || path.equals(""))
                return service.files().get("root").execute();
            String[] folders = path.split("/");
            File previous = service.files().get("root").setFields("name, id, size").execute();
            for (String folder : folders) {
                previous = getGoogleSubFileByName(previous.getId(), folder).get(0);
            }
            return previous;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private boolean isNameValid(String googleFolderIdParent, String name, String type){

        String mimeType = " and mimeType = '";
        if(type == null)
            mimeType = "";
        else
            mimeType += type + "' ";


        try {
            String pageToken = null;
            List<File> list = new ArrayList<File>();
            String query = null;
            query = " name = '" + name + "' " //
                    + mimeType
                    + " and '" + googleFolderIdParent + "' in parents";
            do {
                FileList result = service.files().list().setQ(query).setSpaces("drive") //
                        .setFields("files(name, id, size)")
                        .setPageToken(pageToken).execute();
                list.addAll(result.getFiles());
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
            return list.isEmpty();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Name: " + name + " is not valid!");
            return false;
        }
    }

    private File uploadFile(String parentID, java.io.File file){
        File driveFile = null;
        try {
            File fileMetadata = new File();
            fileMetadata.setName(file.getName());
            fileMetadata.setParents(List.of(parentID));

            FileContent fileContent = new FileContent(map.get(getExtension(file.getName())), file);
            driveFile = service.files().create(fileMetadata, fileContent)
                    .setFields("id, name, parents, mimeType, createdTime, size")
                    .execute();
            std_out(driveFile);
        }catch (Exception e){
            e.printStackTrace();
        }
        return driveFile;
    }

    //returns File if successful or null if not
    private File rootMaking(String path, String name){
        String realFolderId = null;
        if(isValidPath(path)){
            realFolderId = getFolderbyPath(path).getId();
            if(!isNameValid(realFolderId, name, "application/vnd.google-apps.folder")){
                System.out.println("Name: " + name + " is not valid!");
                return null;
            }
        }else{
            System.out.println("Path: " + path + " is not valid!");
            return null;
        }
        try {
            File fileMetadata = new File();
            fileMetadata.setName(name);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            fileMetadata.setParents(List.of(realFolderId));
            File file = service.files().create(fileMetadata)
                        .setFields("id, name, parents, mimeType, createdTime")
                        .execute();
            std_out(file);
            System.out.println("-----------------");
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private boolean downloadFile(String fileID, String OSpath){
        try {
            FileOutputStream os = new FileOutputStream(OSpath);
            service.files().get(fileID)
                    .executeMediaAndDownloadTo(os);
            os.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getExtension(String name){
        return name.substring(name.lastIndexOf(".") + 1);
    }

    private File getFolderByID(String id){
        try {
        return service.files().get(id).execute();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private String getFullPath(String path){
        if(path.equals(""))
            return getRootPath();
        else{
            String pth = getRootPath()+"/"+path;
            if(pth.charAt(pth.length()-1) == '/')
                pth = pth.substring(0, pth.length()-1);
            if(pth.charAt(0) == '/')
                pth = pth.substring(1);
            return pth;
        }
    }
}
