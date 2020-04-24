package com.riw.proiect;

import java.io.*;
import java.util.HashMap;

public class ProcessFile {
    public static String getStringFromFile(File file)
    {
        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String text = "";
            String line = "";
            while((line = bufferedReader.readLine()) != null)
            {
                text += line + " ";
            }
            bufferedReader.close();
            fileReader.close();
            return text;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            System.out.println("Unable to open file '" + file.getName() + "'");
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("Error reading file '" + file.getName() + "'");
            e.printStackTrace();
        }
        return "";
    }

    public static HashMap<String, Integer> hashWithWordFromFile(File file)
    {
        HashMap<String, Integer> words = new HashMap<>();
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            while((line = bufferedReader.readLine()) != null)
            {
                words.put(line.toLowerCase().trim(), 1);
            }
            bufferedReader.close();
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Unable to open file '" + file.getName() + "'");
            e.printStackTrace();
        }catch (IOException e) {
            System.out.println("Error reading file '" + file.getName() + "'");
            e.printStackTrace();
        }
        return words;
    }
}
