package paket;

import Data.MyException;
import Data.MyFile;
import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

//TODO postaviti kao GitHubPackage
//TODO postaviti scope runtime
//TODO skloniti getter
public class GoogleDriveImpl extends FileManager{
    private GDrive gDrive;
    private Drive service;
    private static final String TYPE_MAP = "src/main/resources/typeMap.json";
    private static Map<String, String> map = new HashMap<>();

    static {
        RepoManager.registerManager(new GoogleDriveImpl());
    }


    public GoogleDriveImpl(){
        this.rootPath = "";
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
        }catch (Exception e){
            System.out.println("Greska pri ucitavanju mape");
        }

    }

    @Override
    public boolean createRoot(String path, String name, Configuration configuration)throws MyException {
        if(rootMaking(path,name) == null)
            return false;
        String rootPath;
        if(path.equals(""))
            rootPath = name;
        else
            rootPath = path+"/"+name;

        this.rootPath = rootPath;
        this.configuration = configuration;
        File file = getFilebyPath(rootPath);
        return true;
    }

    @Override
    protected boolean checkConfig(String parentPath, String ext, long size) throws MyException{

        try {
            File parent = getFilebyPath(parentPath);
            String parentID = parent.getId();
            String query = "'" + parentID + "' in parents";
            List<File> children = service.files().list().setQ(query).setSpaces("drive")
                    .setFields("files(id, name, size)")
                    .execute().getFiles();


            if(configuration.getFile_n().containsKey(parentID)){
                int max_files = configuration.getFile_n().get(parentID);
                if(1 + children.size() > max_files){
                    throw new MyException("Max number of files in a " + parentPath + " is: " + configuration.getFile_n().get(parentID));
                }
            }

            if(currSize + size > configuration.getSize()){
                throw new MyException("There is not enough space in folder! Free: "+ (configuration.getSize() - currSize) + ", Your file: " + size);
            }
            if(ext.equals(""))
                return true;
            else
                return !configuration.getExcludedExt().contains(ext.toLowerCase());

        } catch (IOException e) {
            throw new MyException(e.getMessage());
        }

    }


    @Override
    public boolean mkdir(String path, String name)throws MyException{
        String realFolderId = null;
        path = getFullUnixPath(path);
        if(isValidPath(path)){
            File par =  getFilebyPath(path);
            realFolderId = par.getId();
            if(!isNameValid(realFolderId, name, "application/vnd.google-apps.folder")){
                throw new MyException("Name: " + name + " is not valid!");
            }
            if(!checkConfig(path, "", 0)){
                return false;
            }
        }else{
            throw new MyException("Path: " + path + " is not valid!");
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
            throw new MyException(e.getMessage());
        }
    }

    @Override
    public boolean delete(String path) throws MyException{
        path = getFullUnixPath(path);
        if(!isValidPath(path)){
            return false;
        }
        try {
            File file = getFilebyPath(path);

            long delSize = 0L;
            if(file.getSize() != null)
                delSize = file.getSize();

            String fileID = file.getId();
            service.files().delete(file.getId()).execute();

            currSize -= delSize;
            // removing constraints since we don`t have that folder anymore
            configuration.getFile_n().remove(fileID);

        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
        return true;
    }

    @Override
    public boolean move(String oldPath, String newPath) throws MyException{
        oldPath = getFullUnixPath(oldPath);
        newPath = getFullUnixPath(newPath);

        if(!isValidPath(oldPath)){
            return false;
        }
        if(!isValidPath(newPath)){
            return false;
        }

        File destination = getFilebyPath(newPath);
        if(!destination.getMimeType().equals("application/vnd.google-apps.folder")){
            throw new MyException("Invalid destination file path: " + newPath);
        }
        File sourceFile = getFilebyPath(oldPath);
        String oldParentID = sourceFile.getParents().get(0);
        String newParentID = destination.getId();
        String ext = getExtension(oldPath);

        if(!checkConfig(newPath, ext, sourceFile.getSize())){
            return false;
        }

        try {
            service.files().update(sourceFile.getId(), null)
                    .setRemoveParents(oldParentID)
                    .setAddParents(newParentID)
                    .execute();

        }catch (Exception e){
            throw new MyException(e.getMessage());
        }

        return true;
    }

    @Override
    public boolean download(String item, String destination) throws MyException {
        item = getFullUnixPath(item);
        if(!isValidPath(item))
            return false;
        File file = getFilebyPath(item);
        destination = destination + "/" + item.substring(item.lastIndexOf("/")+1);
        return downloadFile(file.getId(), destination);
    }

    @Override
    public boolean upload(String item, String destination) throws MyException {
        try {
            java.io.File javaFile = new java.io.File(item);
            destination = getFullUnixPath(destination);
            if(!isValidPath(destination))
                return false;

            File parent = getFilebyPath(destination);

            String ext = getExtension(javaFile.getName());
            String type = map.get(ext);
            if(!isNameValid(parent.getId(), javaFile.getName(), type)){
                return false;
            }


            long size = Files.size(Paths.get(javaFile.getPath()));
            if(!checkConfig(destination, ext, size)){
                return false;
            }

            uploadFile(parent.getId(), javaFile);
            currSize += size;
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
        return true;
    }

    @Override
    public boolean rename(String path, String newName) throws MyException {
        path = getFullUnixPath(path);
        if(!isValidPath(path))
            return false;

        File file = getFilebyPath(path);

        if(!isNameValid(file.getParents().get(0),newName,file.getMimeType()))
            return false;
        try {
            File newFile = new File();
            newFile.setName(newName);
            service.files().update(file.getId(), newFile)
                    .execute();
            System.out.println(file.getName());

        }catch (Exception e){
            throw new MyException(e.getMessage());
        }

        return true;
    }

    @Override
//    TODO dodati na zatvaranju
    public void saveConfig() throws MyException {
        try(FileWriter writer = new FileWriter("src/main/resources/config.json")) {
            Gson gson = new Gson();
            gson.toJson(configuration, writer);
            writer.close();
            java.io.File javaFile = new java.io.File("src/main/resources/config.json");
            String rootID = getFilebyPath(rootPath).getId();
            uploadFile(rootID, javaFile);
            javaFile.delete();
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
    }

    @Override
    public List<MyFile> searchDir(String path) throws MyException{
        path = getFullUnixPath(path);
        List<MyFile> myFiles = new ArrayList<>();
        if(!isValidPath(path))
            return myFiles;

        File parent = getFilebyPath(path);
        try {
            List<File> gList = service.files().list()
                    .setQ("'" + parent.getId() + "'" + " in parents and mimeType != 'application/vnd.google-apps.folder'")
                    .setFields("files(id, name, mimeType, createdTime, modifiedTime, size)")
                    .execute().getFiles();

            convertToMyFiles(myFiles, gList);
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }

        return myFiles;
    }

    @Override
    public List<MyFile> searchSubDir(String path) throws MyException{

            List<MyFile> myFiles = new ArrayList<>();
        try {
            path = getFullUnixPath(path);
            String parentID = getFilebyPath(path).getId();
            List<File> folders;
            folders = service.files().list()
                    .setQ( "'" + parentID + "'" + " in parents and mimeType = 'application/vnd.google-apps.folder'")
                    .setFields("files(id, name, mimeType, parents)")
                    .execute().getFiles();
            for(File dir : folders)
                myFiles.addAll(getRecursiveFiles(dir,""));
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
        return myFiles;
    }

    @Override
    public boolean existName(String path, String name) throws MyException {
        path = getFullUnixPath(path);
        if(!isValidPath(path))
            return false;

        File folder = getFilebyPath(path);
        try {
         return !service.files().list()
                .setQ("name = " + "'" + name + "' and '" + folder.getId() + "' in parents")
                .execute().getFiles().isEmpty();
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
    }

    @Override
    public List<MyFile> filterByExt(String path, String ext) throws MyException{
        path = getFullUnixPath(path);
        if(!isValidPath(path)){
            return new ArrayList<MyFile>();
        }
        String parentID = getFilebyPath(path).getId();
        if(ext.charAt(0) != '.')
            ext = "." + ext;
        return internalsearchSubstring(path, ext);
    }


    @Override
    public List<MyFile> searchSubstring(String substr)throws MyException{
        return internalsearchSubstring(rootPath, substr);
    }


    @Override
    public List<String> getParentPath(String name) throws MyException {
        return internalsearchParents(rootPath, name);
    }

    @Override
    public List<MyFile> filterByPeriod(String path, LocalDateTime startDate, LocalDateTime endDate, boolean modified) throws MyException{
        List<MyFile> myFiles = new ArrayList<>();
        path = getFullUnixPath(path);
        if(!isValidPath(path))
            return myFiles;

        File parent = getFilebyPath(path);
        try {
            List <File> files = service.files().list()
                        .setQ("'" + parent.getId() + "' in parents and mimeType != 'application/vnd.google-apps.folder'")
                        .setFields("files(id, name, mimeType, createdTime, modifiedTime, size)")
                        .execute().getFiles();

            List<MyFile> temp = new ArrayList<>();
            convertToMyFiles(temp, files);

            if(modified){
                for(MyFile f : temp){
                    if(f.getLastModified().isAfter(startDate) && f.getLastModified().isBefore(endDate))
                        myFiles.add(f);
                }
            }else{
                for(MyFile f : temp){
                    if(f.getTimeCreated().isAfter(startDate) && f.getTimeCreated().isBefore(endDate))
                        myFiles.add(f);
                }
            }

        }catch (Exception e){
            throw new MyException(e.getMessage());
        }

        return myFiles;
    }

    static void std_out(File f){
        System.out.println("Name: " + f.getName() + "   Id: " + f.getId() + "   Parents: " + f.getParents() + " Type: " + f.getMimeType() + "   CreatedTime: " + f.getCreatedTime() + " Size: " + f.getSize());
        System.out.println("-----------------");

    }

    boolean isValidPath(String path) throws MyException{
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
                if(i < folders.length - 1)
                    previous = getGoogleSubFileByName(previous, folders[i], "folder").get(0).getId();
                else previous = getGoogleSubFileByName(previous, folders[i], "else").get(0).getId();
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            throw new MyException("Invalid path: " + path + "\n" + e.getMessage());
        }
    }

    private List<MyFile> getRecursiveFiles(File dir, String condition)throws MyException{
        List<MyFile> myFiles = new ArrayList<>();
        try {
            myFiles.addAll(privateSearchDir(dir.getId(), condition));
            List<File> folders = service.files().list()
                    .setQ( "'" + dir.getId() + "'" + " in parents and mimeType = 'application/vnd.google-apps.folder'")
                    .execute().getFiles();
            for(File folder : folders)
                myFiles.addAll(getRecursiveFiles(folder, condition));
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
        return myFiles;
    }

    private List<MyFile> privateSearchDir(String parentID, String substr) {
        List<MyFile> myFiles = new ArrayList<>();
        try {
            List<File> gList = new ArrayList<>();
            gList = service.files().list()
                    .setQ(substr + "'" + parentID + "'" + " in parents and mimeType != 'application/vnd.google-apps.folder'")
                    .setFields("files(id, name, mimeType, createdTime, modifiedTime, size)")
                    .execute().getFiles();

            convertToMyFiles(myFiles, gList);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Error during privateSearchDir!");
            return myFiles;
        }
        return myFiles;
    }

    private void convertToMyFiles(List<MyFile> myFiles, List<File> gList) throws MyException{
        for(File gFile : gList){

            LocalDateTime modified = convertToLocalDateTime(gFile.getModifiedTime());
            LocalDateTime created = convertToLocalDateTime(gFile.getCreatedTime());
            MyFile myFile = new MyFile(getDFSpath(gFile.getId()),gFile.getName(),gFile.getSize(),modified, created, getExtension(gFile.getName()));
            myFiles.add(myFile);
        }
    }


    // com.google.api.services.drive.model.File
    private List<File> getGoogleSubFileByName(String googleFolderIdParent, String subFileName, String typeQuery)
            throws IOException {

        String type = "";
        if(typeQuery.equalsIgnoreCase("file"))
            type = " and mimeType != 'application/vnd.google-apps.folder'";
        if(typeQuery.equalsIgnoreCase("folder"))
            type = " and mimeType = 'application/vnd.google-apps.folder'";

        String pageToken = null;
        List<File> list = new ArrayList<File>();

        String query = null;
        if (googleFolderIdParent == null) {
            query = " name = '" + subFileName + "' " //
                    + type
                    + " and 'root' in parents";
        } else {
            query = " name = '" + subFileName + "' " //
                    + type
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
    private File getFilebyPath(String path){
        try {

            if (path == null || path.equals(""))
                return service.files().get("root").execute();
            String[] folders = path.split("/");
            File previous = service.files().get("root").setFields("name, id, size").execute();
            for (int i = 0; i < folders.length; i++) {
                if(i < folders.length - 1)
                    previous = getGoogleSubFileByName(previous.getId(), folders[i], "folder").get(0);
                else previous = getGoogleSubFileByName(previous.getId(), folders[i], "else").get(0);
            }
            return previous;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private LocalDateTime convertToLocalDateTime(DateTime dateTime)throws MyException{
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            return LocalDateTime.parse(dateTime.toStringRfc3339(), f);
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
    }

    private boolean isNameValid(String googleFolderIdParent, String name, String type) throws MyException{

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
            throw new MyException(e.getMessage() + '\n' + "Name: " + name + " is not valid!");
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
    private File rootMaking(String path, String name) throws MyException {
        String realFolderId = null;
        if(isValidPath(path)){
            realFolderId = getFilebyPath(path).getId();
            if(!isNameValid(realFolderId, name, "application/vnd.google-apps.folder")){
                throw new MyException("Name: " + name + " is not valid!");
            }
        }else{
            throw new MyException("Path: " + path + " is not valid!");
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
            throw new MyException(e.getMessage());
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

    private File getFolderByID(String id) throws MyException{
        try {
        return service.files().get(id)
                .setFields("id, name, mimeType, parents, createdTime, size")
                .execute();
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
    }

    private String getDFSpath(String fileID)throws MyException{
        String currID = fileID;
        File rootFile = getFilebyPath(rootPath);
        String rootID = rootFile.getId();
        String path = "";

        while(!currID.equals(rootID)){
            File currFile = getFolderByID(currID);
            path  = "/" + currFile.getName() + path;
            currID = currFile.getParents().get(0);
        }
        return rootPath + path;
    }

    @Override
    protected String getFullPath(String path) {
        File f = getFilebyPath(getFullUnixPath(path));

        return f.getId();
    }

    private String getFullUnixPath(String path){

        path = path.replace("\\", "/");
        path = path.replace("//", "/");
        if(path.length() >= 1 && path.charAt(path.length()-1) == '/')
            path = path.substring(0, path.length()-1);
        if(path.length() >= 1 && path.charAt(0) == '/')
            path = path.substring(1);

        if(path.equals("") || path.equals(rootPath))
            return rootPath;
        else{
            String pth = rootPath+"/"+path;
//            path = path.replace("//", "/");
            if(pth.charAt(pth.length()-1) == '/')
                pth = pth.substring(0, pth.length()-1);
            if(pth.charAt(0) == '/')
                pth = pth.substring(1);
            return pth;
        }
    }

    private List<MyFile> internalsearchSubstring(String path, String substr)throws MyException{
        List<MyFile> myFiles = new ArrayList<>();
        try {
            File rootFile = getFilebyPath(path);
            List<File> files = service.files().list()
                    .setQ("name contains '" + substr + "' and mimeType != 'application/vnd.google-apps.folder' and '" + rootFile.getId() + "' in parents")
                    .setFields("files(id, name, mimeType, createdTime, modifiedTime, size, parents)")
                    .execute().getFiles();

            if(files != null && !rootFile.isEmpty())
                convertToMyFiles(myFiles,files);

            List<File> folders = service.files().list()
                    .setQ( "'" + rootFile.getId() + "'" + " in parents and mimeType = 'application/vnd.google-apps.folder'")
                    .setFields("files(id, name, mimeType, parents)")
                    .execute().getFiles();

            String condition = "name contains '" + substr + "' and ";

            for(File dir : folders)
                myFiles.addAll(getRecursiveFiles(dir, condition));

        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
        return myFiles;
    }

    private List<String> internalsearchParents(String path, String name)throws MyException{
        List<String> parents = new ArrayList<>();
        try {
            File rootFile = getFilebyPath(path);
            List<File> files = service.files().list()
                    .setQ("name = '" + name + "' and mimeType != 'application/vnd.google-apps.folder' and '" + rootFile.getId() + "' in parents")
                    .setFields("files(id, name)")
                    .execute().getFiles();

            if(files != null && !files.isEmpty())
                parents.add(path);

            List<File> folders = service.files().list()
                    .setQ( "'" + rootFile.getId() + "'" + " in parents and mimeType = 'application/vnd.google-apps.folder'")
                    .setFields("files(id, name, mimeType, parents)")
                    .execute().getFiles();

            for(File dir : folders)
                parents.addAll(internalsearchParents(getDFSpath(dir.getId()),name));
        }catch (Exception e){
            throw new MyException(e.getMessage());
        }
        return parents;
    }
}
