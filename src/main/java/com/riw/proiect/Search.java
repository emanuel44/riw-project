package com.riw.proiect;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;


import java.util.*;

public class Search {
    private MongoCollection<Document> directIndex = null;

    public Search(MongoCollection<Document> directIndex){
        this.directIndex = directIndex;
    }

    public List<org.bson.Document> getList(Document query){
        FindIterable<org.bson.Document> findIterable = directIndex.find();
        Iterator iterator = findIterable.iterator();
        List<org.bson.Document> documents = new ArrayList<>();
        while (iterator.hasNext()){
            org.bson.Document document = (org.bson.Document) iterator.next();
            double cos = cosDistance(document, query);
            if(cos != 0.0){
                document.append("cosDistanceToQuery", cos);
                documents.add(document);
            }
        }
        return documents;
    }

    private static double cosDistance(Document document1, Document document2){
        List<Document> vectorDoc1 = (List<Document>) document1.get("vector");
        Double moduleDoc1 = document1.getDouble("module");
        List<Document> vectorDoc2 = (List<Document>) document2.get("vector");
        Double moduleDoc2 = document2.getDouble("module");
        HashMap<String , Double> hashDocLongVector = new HashMap<>();
        double sumNumarator = 0;
        if(vectorDoc1.size()> vectorDoc2.size()){
            for(Document docVector1 : vectorDoc1){
                hashDocLongVector.put(docVector1.getString("term"), docVector1.getDouble("value"));
            }
            for(Document doc : vectorDoc2){
                String word = doc.getString("term");
                Double value1 = doc.getDouble("value");
                Double value2 = hashDocLongVector.get(word);
                if(value2 !=null){
                    sumNumarator += value1 * value2;
                }
            }
            double numitor = moduleDoc1 * moduleDoc2;
            return numitor == 0.0 ? 0 : sumNumarator/numitor;
        }
        else{
            for(Document docVector2 : vectorDoc2){
                hashDocLongVector.put(docVector2.getString("term"), docVector2.getDouble("value"));
            }
            for (Document doc : vectorDoc1){
                String word = doc.getString("term");
                Double value1 = doc.getDouble("value");

                Double value2 = hashDocLongVector.get(word);
                if(value2 !=null){
                    sumNumarator += value1 * value2;
                }
            }
            double numitor = moduleDoc1 * moduleDoc2;
            return numitor == 0.0 ? 0 : sumNumarator/numitor;
        }
    }
}
