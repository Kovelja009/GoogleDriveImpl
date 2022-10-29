package paket;

import Data.MyFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

//TODO postaviti kao GitHubPackage
//TODO postaviti scope runtime
public class GoogleDriveImpl extends FileManager{
    private GDrive gDrive;

    @Override
    public boolean createRoot(String s, String name, Configuration configuration)throws IOException {
        gDrive = new GDrive();
        return false;
    }

    @Override
    boolean checkConfig(String s, String s1, long l, int n_number) {
        return false;
    }

    @Override
    public boolean mkdir(String s, String s1) {
        return false;
    }

    @Override
    public boolean mkdir(String s, List<String> list) {
        return false;
    }

    @Override
    public boolean mkdir(String s, String s1, int i) {
        return false;
    }

    @Override
    public boolean delete(String s) {
        return false;
    }

    @Override
    public boolean move(String s, String s1) {
        return false;
    }

    @Override
    public boolean download(String s, String s1) {
        return false;
    }

    @Override
    public boolean rename(String s, String s1) {
        return false;
    }

    @Override
    public void saveConfig(String s) {

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
    public List<MyFile> sortBy(List<String> list, Metadata metadata) {
        return null;
    }

    @Override
    public List<String> filterData(List<String> list, List<Metadata> list1) {
        return null;
    }

    @Override
    public List<MyFile> filterByPeriod(String s, Date date, Date date1, boolean b) {
        return null;
    }
}
