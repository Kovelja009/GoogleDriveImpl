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

    static void ispis(File f){
        System.out.println("Name: " + f.getName() + "   Id: " + f.getId() + "   Parents: " + f.getParents() + " Type: " + f.getMimeType() + "   CreatedTime: " + f.getCreatedTime() + "Size: " + f.getSize());
    }

    public static void main(String[] args) {
        try {
            GoogleDriveImpl gdg = new GoogleDriveImpl();
            Configuration configuration = new Configuration();
            System.out.println(gdg.createRoot("", "Root887345678113", configuration, 2));
            gdg.mkdir("", "Folder");
            gdg.mkdir("Folder", "Folder2");
            gdg.upload("src/main/resources/photo.png", "Folder");
            gdg.upload("src/main/resources/photo2.jpg","Folder");
            System.out.println(gdg.searchDir("Folder"));
            gdg.saveConfig();
//            System.out.println("Renaming: " + gdg.rename("photo2.jpg", "steins_gate.jpg"));
//            gdg.delete("Folder1");

//            for(File f : gdg.getService().files().list()
//                    .setFields("files(name, id, parents, mimeType, createdTime, size)")
//                    .execute().getFiles())
//                ispis(f);




            //TODO sacuvaj liniju !!!
//            System.out.println(service.files().get("root").execute().getId());

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
