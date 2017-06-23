package com.example.android.camera2raw;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Matthew on 1/17/2017.
 */

public class SaveCSV {
    private FileOutputStream outputStream;
    private FileInputStream inputStream;
    private Context _context;
    public String _fileName;
    public String[] titles;
    private File ParentDir;

    public SaveCSV(String name, Context context, File parent) {
        _context = context;
        _fileName = name + "_" + generateTimestamp() + ".csv";
        ParentDir=parent;
    }

    public void setTitleBar(String[] s) {
        titles = s;
    }

    public void saveData(String[] s) {

        //Log.e("" + _fileName, "saved");
        Calendar calendar = Calendar.getInstance();
        try {
            String string = "";
            string += load();
            for (int i = 0; i < s.length - 1; i++) {
                string += s[i] + ",";
            }
            string += s[s.length - 1] + ";";
            outputStream = _context.openFileOutput(_fileName, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
            File file = new File(ParentDir, _fileName);
            PrintWriter csvWriter;
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
                csvWriter = new PrintWriter(new FileWriter(file, true));
                int last = 0;
                int count = 0;
                String title = "";
                for (int i = 0; i < titles.length; i++) {
                    title += titles[i] + ",";
                }
                csvWriter.print(title);
                csvWriter.append('\n');
                for (int i = 0; i < string.length(); i++) {
                    if (string.charAt(i) == ';') {
                        csvWriter.print(string.substring(last + 1, i));
                        csvWriter.append('\n');
                        last = i;
                    }
                }
                csvWriter.print(string.substring(last, string.length() - 1));
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String load() {
        String line1 = "";
        try {
            inputStream = _context.openFileInput(_fileName);
            if (inputStream != null) {
                InputStreamReader inputreader = new InputStreamReader(inputStream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line = "";
                try {
                    while ((line = buffreader.readLine()) != null) {
                        line1 += line;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {

            String error = "";
            error = e.getMessage();
        }
        return line1;
    }

    public static String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
        return sdf.format(new Date());
    }

}