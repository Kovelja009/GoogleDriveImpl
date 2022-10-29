package paket;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    static boolean existParent(String fileId, Drive service){
        try {   // return if it`s folder
            return service.files().get(fileId).execute().getMimeType().equals("application/vnd.google-apps.folder");
        }catch (Exception e){
            return false;
        }
    }

    boolean validParent(String parentID, String name, Drive service){
        if(existParent(parentID, service))
            return false;
        return true;
    }

    // com.google.api.services.drive.model.File
    public static final List<File> getGoogleSubFolderByName(String googleFolderIdParent, String subFolderName,Drive driveService)
            throws IOException {

        String pageToken = null;
        List<File> list = new ArrayList<File>();

        String query = null;
        if (googleFolderIdParent == null) {
            query = " name = '" + subFolderName + "' " //
                    + " and mimeType = 'application/vnd.google-apps.folder' " //
                    + " and 'root' in parents";
        } else {
            query = " name = '" + subFolderName + "' " //
                    + " and mimeType = 'application/vnd.google-apps.folder' " //
                    + " and '" + googleFolderIdParent + "' in parents";
        }

        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive") //
                    .setFields("nextPageToken, files(id, name, mimeType, parents, createdTime)")//
                    .setPageToken(pageToken).execute();
            list.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        //
        return list;
    }



    boolean checkPath(String previousId, String name, Drive service){
        try {   // ukoliko ga pronadje u parentu vraca true
            return !getGoogleSubFolderByName(previousId, name, service).isEmpty();
        }catch (Exception e){
            return false;
        }
    }

    //TODO koristi ovaj
    static boolean isValidPath(String path, Drive service){
        if (path == null || path.equals("") || path.toLowerCase().equals("root"))
            return true;
        try {
            String[] folders = path.split("/");
            String previous = null;
            for(int i = 0; i < folders.length; i++){
//                if(!checkPath(previous, folders[i], service))
//                    return false;
                previous = getGoogleSubFolderByName(previous, folders[i], service).get(0).getId();
            }
                return true;
        }catch (Exception e){
            System.out.println("Invalid path: " + path);
            return false;
        }
    }
    // if correct returns file, else null
    static File getFolderbyPath(String path, Drive service){
        try {
            if (path == null || path.equals("") || path.toLowerCase().equals("root"))
                return service.files().get("root").execute();
            String[] folders = path.split("/");
            File previous = service.files().get("root").execute();
            for (String folder : folders) {
                previous = getGoogleSubFolderByName(previous.getId(), folder, service).get(0);
            }
            return previous;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    static boolean isFolderNameValid(String googleFolderIdParent, String name, Drive service){
        try {

            String pageToken = null;
            List<File> list = new ArrayList<File>();
            String query = null;
                query = " name = '" + name + "' " //
                        + " and mimeType = 'application/vnd.google-apps.folder' " //
                        + " and '" + googleFolderIdParent + "' in parents";
            do {
                FileList result = service.files().list().setQ(query).setSpaces("drive") //
                        .setPageToken(pageToken).execute();
                list.addAll(result.getFiles());
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
            return list.isEmpty();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    // returns folder or null
    static File makeFolder(String path, String name, Drive service){

        String realFolderId = null;
        if(isValidPath(path,service)){
            realFolderId = getFolderbyPath(path,service).getId();
            if(!isFolderNameValid(realFolderId, name, service)){
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
            ispis(file);
            System.out.println("-----------------");
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void ispis(File f){
        System.out.println("Name: " + f.getName() + "   Id: " + f.getId() + "   Parents: " + f.getParents() + " Type: " + f.getMimeType() + "   CreatedTime: " + f.getCreatedTime());
    }

    public static void main(String[] args) {
        try {
            GDrive gDrive = new GDrive();
            Drive service = gDrive.getDrive();

            makeFolder("", "Proba1", service);
            makeFolder("Proba1", "Proba2", service);
            makeFolder("Proba1/Proba2", "Proba3", service);
            makeFolder("Proba1/Proba2", "Proba31", service);
            makeFolder("Proba1/Proba2", "Proba31", service);


            //TODO sacuvaj liniju !!!
//            System.out.println(service.files().get("root").execute().getId());

//            for(File f : service.files().list().setFields("files(id, name, parents, mimeType)").execute().getFiles())
//                ispis(f);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
