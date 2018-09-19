package manimage.common.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Settings {

    private HashMap<String, Boolean> bools = new HashMap<>();
    private HashMap<String, String> strings = new HashMap<>();
    private HashMap<String, Integer> ints = new HashMap<>();
    private HashMap<String, Double> doubles = new HashMap<>();

    private HashSet<SettingListener<Boolean>> boolListeners = new HashSet<>();
    private HashSet<SettingListener<String>> stringListeners = new HashSet<>();
    private HashSet<SettingListener<Integer>> intListeners = new HashSet<>();
    private HashSet<SettingListener<Double>> doubleListeners = new HashSet<>();


    public Settings(File settingsFile) throws FileNotFoundException {
        load(settingsFile);
    }

    public Settings() {
    }

    public synchronized void load(File file) throws FileNotFoundException {
        clean();

        Scanner scan = new Scanner(file);
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (!line.isEmpty() && !line.startsWith("#")) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    if (parts[0].equals("bool")) {
                        setBoolean(parts[1], Boolean.parseBoolean(parts[2]));
                    } else if (parts[0].equals("string")) {
                        setString(parts[1], parts[2]);
                    } else if (parts[0].equals("int")) {
                        setInteger(parts[1], Integer.parseInt(parts[2]));
                    } else if (parts[0].equals("double")) {
                        setDouble(parts[1], Double.parseDouble(parts[2]));
                    }
                }
            }
        }
    }

    public synchronized void save(File file) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(file);

        bools.forEach((t, b) -> writer.println("bool:" + t + ":" + b));
        strings.forEach((t, s) -> writer.println("string:" + t + ":" + s));
        ints.forEach((t, i) -> writer.println("int:" + t + ":" + i));
        doubles.forEach((t, d) -> writer.println("double:" + t + ":" + d));


        writer.close();
    }

    public synchronized void clean() {
        bools.clear();
        strings.clear();
        ints.clear();
        doubles.clear();
    }

    //--------------------- Setters ------------------------------------------------------------------------------------

    public synchronized void setBoolean(String tag, boolean b) {
        Boolean previous = bools.put(tag, b);
        if (previous == null || previous != b)
            boolListeners.forEach(listener -> listener.settingChanged(previous, b));
    }

    public synchronized void setString(String tag, String s) {
        String previous = strings.put(tag, s);
        if (previous == null || !previous.equals(s))
            stringListeners.forEach(listener -> listener.settingChanged(previous, s));
    }

    public synchronized void setInteger(String tag, int i) {
        Integer previous = ints.put(tag, i);
        if (previous == null || previous != i)
            intListeners.forEach(listener -> listener.settingChanged(previous, i));
    }

    public synchronized void setDouble(String tag, double d) {
        Double previous = doubles.put(tag, d);
        if (previous == null || previous != d)
            doubleListeners.forEach(listener -> listener.settingChanged(previous, d));
    }

    //-------------------- Getters -------------------------------------------------------------------------------------

    public boolean getBoolean(String tag, boolean fallback) {
        Boolean b = bools.get(tag);
        if (b == null) return fallback;
        return b;
    }

    public String getString(String tag, String fallback) {
        String s = strings.get(tag);
        if (s == null) return fallback;
        return s;
    }

    public int getInt(String tag, int fallback) {
        Integer i = ints.get(tag);
        if (i == null) return fallback;
        return i;
    }

    public double getDouble(String tag, double fallback) {
        Double d = doubles.get(tag);
        if (d == null) return fallback;
        return d;
    }

    //--------------- Adders -------------------------------------------------------------------------------------------

    public synchronized boolean addBoolListener(SettingListener<Boolean> listener) {
        return boolListeners.add(listener);
    }

    public synchronized boolean addStringListener(SettingListener<String> listener) {
        return stringListeners.add(listener);
    }

    public synchronized boolean addIntListener(SettingListener<Integer> listener) {
        return intListeners.add(listener);
    }

    public synchronized boolean addDoubleListener(SettingListener<Double> listener) {
        return doubleListeners.add(listener);
    }

    //--------------------- Removers -----------------------------------------------------------------------------------

    public synchronized boolean removeBoolListener(SettingListener<Boolean> listener) {
        return boolListeners.remove(listener);
    }

    public synchronized boolean removeStringListener(SettingListener<String> listener) {
        return stringListeners.remove(listener);
    }

    public synchronized boolean removeIntListener(SettingListener<Integer> listener) {
        return intListeners.remove(listener);
    }

    public synchronized boolean removeDoubleListener(SettingListener<Double> listener) {
        return doubleListeners.remove(listener);
    }

}
