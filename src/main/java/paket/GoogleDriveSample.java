package paket;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GoogleDriveSample {
//    /**
//     * Application name.
//     */
//    private static final String APPLICATION_NAME = "Google Drive File Manager";
//    /**
//     * Global instance of the JSON factory.
//     */
//    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//    /**
//     * Directory to store authorization tokens for this application.
//     */
//    private static final String TOKENS_DIRECTORY_PATH = "tokens";
//
//    /**
//     * Global instance of the HTTP transport.
//     */
//    private static HttpTransport HTTP_TRANSPORT;
//
//    /**
//     * Global instance of the scopes required by this quickstart.
//     * If modifying these scopes, delete your previously saved tokens/ folder.
//     */
//
//    static {
//        try {
//            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        } catch (Throwable t) {
//            t.printStackTrace();
//            System.exit(1);
//        }
//    }
//    private static final List<String> SCOPES =
//            Collections.singletonList(DriveScopes.DRIVE);
//    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
//
//    /**
//     * Creates an authorized Credential object.
//     *
//     * @return An authorized Credential object.
//     * @throws IOException If the credentials.json file cannot be found.
//     */
//    private static Credential authorize()
//            throws IOException {
//        // Load client secrets.
//        InputStream in = GoogleDriveSample.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
//        if (in == null) {
//            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
//        }
//        GoogleClientSecrets clientSecrets =
//                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
//
//        // Build flow and trigger user authorization request.
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
//                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
//                .setAccessType("offline")
//                .build();
//        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
//        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
//        //returns an authorized Credential object.
//        return credential;
//    }
//
//    public static Drive getDriveService() throws IOException{
//        Credential credential = authorize();
//        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//    }

    static void makeFile(String name, Drive service){
        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        try {
            File file = service.files().create(fileMetadata)
                    .setFields("id, parents")
                    .execute();
            System.out.println("Folder ID: " + file.getId());
        } catch (Exception e) {
            // TODO(developer) - handle error appropriately
//            System.err.println("Unable to create folder: " + e.getDetails());
        }
    }

    //TODO Repo ne mora imati roditelja za pocetak
    //TODO Kao root path sacuvati repo id
    //TODO svaki sledeci parent mu je ili repo ili nesto unutar repo-a
    //TODO putanju splitujem i rekurzivno proveravam da li postoji i ako postoji uzmem najdublju stvar i na nju dodam novo dete
    public static void main(String[] args) throws IOException {
        // Build a new authorized API client service.
        Drive service = new GDrive().getDrive();
//        makeFile("Repo", service);
//        System.out.println(service.files().list()
//                .setQ("name contains 'Repo'")
//                .execute().getFiles().get(0).setP);

//        FileList result = service.files().list()
//                .setQ("name = 'aa'")
//                .execute();
//        String realFolderId = result.getFiles().get(0).getId();
//        System.out.println(realFolderId);
//        File fileMetadata = new File();
//        fileMetadata.setName("photo.jpg");
//        fileMetadata.setParents(Collections.singletonList(realFolderId));
//        java.io.File filePath = new java.io.File(result.getFiles().get(0).getName());
//        FileContent mediaContent = new FileContent("image/jpeg", filePath);
//        try {
//            File file = service.files().create(fileMetadata, mediaContent)
//                    .setFields("id, parents")
//                    .execute();
//            System.out.println("File ID: " + file.getId() + "File name " + file.getProperties());
//        } catch (GoogleJsonResponseException e) {
////             TODO(developer) - handle error appropriately
//            System.err.println("Unable to upload file: " + e.getDetails());
//            throw e;
//        }

//        System.out.println(service.getServicePath());
//        service.files().list().setPageToken().execute();
//        FileList result = service.files().list()
//                .setQ("name = 'Rod/aa'")
//                .execute();
//
//        for(File f : result.getFiles())
//            System.out.println(f.getName() + " " + f.getId() + " " + f.getProperties());
//        File fileMetadata = new File();
//        fileMetadata.setName("Test");
//        fileMetadata.setMimeType("application/vnd.google-apps.folder");
//        try {
//            File file = service.files().create(fileMetadata)
//                    .setFields("id")
//                    .execute();
//            System.out.println("Folder ID: " + file.getId());
//        } catch (GoogleJsonResponseException e) {
//            // TODO(developer) - handle error appropriately
//            System.err.println("Unable to create folder: " + e.getDetails());
//            throw e;
//        }
//                String realFolderId = "1nK-40hTTLuV28ct1Ixit_gIewzx5WJhj";
//                FileList result = service.files().list()
//                .setQ("pare")
//                .execute();
////                new java.io.File("asdf").
//            for(File f : result.getFiles())
//                System.out.println(f.getFileExtension());
//                System.out.println(f.getName());
        // File's metadata.
//        File fileMetadata = new File();
//        fileMetadata.setName("photo.png");
////        fileMetadata.setParents(Collections.singletonList(realFolderId));
//        java.io.File filePath = new java.io.File("src/main/resources/photo.png");
//        FileContent mediaContent = new FileContent("image/jpeg", filePath);
//        try {
//            File file = service.files().create(fileMetadata, mediaContent)
//                    .setFields("id")
//                    .execute();
//            System.out.println("File ID: " + file.getId());
//        } catch (GoogleJsonResponseException e) {
//            // TODO(developer) - handle error appropriately
//            System.err.println("Unable to upload file: " + e.getDetails());
//            throw e;
//        }

    }
}
