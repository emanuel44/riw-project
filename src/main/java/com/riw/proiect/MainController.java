package com.riw.proiect;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.riw.proiect.*;

import org.bson.Document;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

@Controller
public class MainController {
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model){
        model.addAttribute("results", false);
        model.addAttribute("notResult", false);
        return "index";
    }
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String getResultSearch(@RequestParam(name="query")String query, Model model)
    {
        MongoCollection<Document> directIndex = riwApplication.directIndexForSearch;
        MongoCollection<Document> inverseIndex = riwApplication.inverseIndexForSearch;
        String[] queryWords = query.toLowerCase().split("[^a-zA-Z0-9]");
        HashMap<String, Integer> stopWords = ProcessFile.hashWithWordFromFile(new File("stopWords.txt"));
        HashMap<String, Integer> exceptionWords = ProcessFile.hashWithWordFromFile(new File("exceptionWords.txt"));
        HashMap<String, Integer> wordsFrequency = new HashMap<>();

        for(String word : queryWords)
        {
            if(wordsFrequency.containsKey(word)){
                wordsFrequency.replace(word, wordsFrequency.get(word) + 1);
            }
            else{
                if(!stopWords.containsKey(word)){
                    wordsFrequency.put(word, 1);
                }
                else if(exceptionWords.containsKey(word)){
                    wordsFrequency.put(word, 1);
                }
            }
        }
        
        List<Document> vectorDocQuery = new ArrayList<>();
        double sumSquare = 0;
        for(Map.Entry<String, Integer> entry : wordsFrequency.entrySet()){
            Document idfDoc = inverseIndex.find(eq("term", entry.getKey())).first();
            Double logIDF = 0.0;
            if(idfDoc!=null){
                logIDF = Math.log(idfDoc.getDouble("idf"));
            }
            Double tf = (double) entry.getValue()/queryWords.length;
            Double value = tf * logIDF;
            sumSquare += value * value;
            Document document = new Document("term", entry.getKey())
                    .append("value", value);
            vectorDocQuery.add(document);
        }

        Document document = new Document("module", Math.sqrt(sumSquare))
                .append("vector", vectorDocQuery);
		System.out.println("Searching query : " + query);
        Search searchInstance = new Search(directIndex);
        ArrayList<Document> documents = (ArrayList<Document>) searchInstance.getList(document);
       if(documents.size() > 0){
            Collections.sort(documents, (d1, d2) -> d2.getDouble("cosDistanceToQuery").compareTo(d1.getDouble("cosDistanceToQuery")));
            List<String> docs = new ArrayList<>();
            double epsilon = 0.000000001;

            for(Document doc : documents){
                String docName = doc.getString("doc");
                Double distToQuery = doc.getDouble("cosDistanceToQuery");
                if(distToQuery > epsilon && docs.size() < 50){
                    docs.add(docName);
                }
            }
            model.addAttribute("results", true);
            model.addAttribute("docs", docs);
            model.addAttribute("notResult", false);
            System.out.println(docs.size() + " de rezultate afisate in browser!");
        }else{
            model.addAttribute("notResult", true);
            System.out.println("Niciun rezultat!");
        }
        return "/index";
    }
}
