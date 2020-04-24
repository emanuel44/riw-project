package com.riw.proiect;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.*;

public class InverseIndex {
    private MongoCollection<Document> inputDirectIndex;
    private MongoCollection<Document> outputInverseIndex;

    public static int nrDocuments = 0;


    public InverseIndex(MongoCollection<Document> inputDirectIndex, MongoCollection<Document> outputInverseIndex){
        this.inputDirectIndex = inputDirectIndex;
        this.outputInverseIndex = outputInverseIndex;
    }
    
    public void createInverseIndex(){
        FindIterable<Document> documentFindIterable = inputDirectIndex.find();
        Iterator iterator = documentFindIterable.iterator();
        HashMap<String, Integer> hashMap = new HashMap<>();
        while (iterator.hasNext())
        {
            Document document = (Document)iterator.next();
            String docName = document.getString("doc");
            List<Document> terms = (List<Document>) document.get("terms");
            for(Document docTerm: terms)
            {
                String term = docTerm.getString("term");
                Integer count = docTerm.getInteger("count");
                if (hashMap.containsKey(term)){
                    Document newDocument = new Document("docs", (new Document("doc", docName).append("count", count)));
                    Document modifyDocument = new Document();
                    modifyDocument.put("$push", newDocument);
                    outputInverseIndex.updateOne(new Document("term", term), modifyDocument);
                }
                else{
                    Document newDocument = new Document("term", term)
                            .append("docs", Arrays.asList(new Document("doc", docName).append("count", count)));
                    outputInverseIndex.insertOne(newDocument);
                    hashMap.put(term, 1);
                }
            }
            System.out.println(" Hash size = " + hashMap.size());
        }
    }

    public void createInverseIndexLocal(){
        FindIterable<Document> documentFindIterable = inputDirectIndex.find();
        Iterator iterator = documentFindIterable.iterator();
        HashMap<String, HashMap<String, Integer>> inverseIndexeHashMap = new HashMap<>();
        while (iterator.hasNext())
        {
            Document document = (Document)iterator.next();
            String docName = document.getString("doc");
            List<Document> terms = (List<Document>) document.get("terms");
            for(Document docTerm: terms)
            {
                String term = docTerm.getString("term");
                Integer count = docTerm.getInteger("count");

                if (inverseIndexeHashMap.containsKey(term))
                {
                    inverseIndexeHashMap.get(term).put(docName, count);
                }
                else
                {
                    HashMap<String, Integer> newDocHash = new HashMap<>();
                    newDocHash.put(docName, count);
                    inverseIndexeHashMap.put(term, newDocHash);
                }
            }
        }
        long D = inputDirectIndex.count();
        for(Map.Entry<String, HashMap<String, Integer>> entry : inverseIndexeHashMap.entrySet()){
            String term = entry.getKey();
            Document documentTerm = new Document("term", term);
            HashMap<String, Integer> documents = entry.getValue();
            List<Document> documentList = new ArrayList<>();
            for(Map.Entry<String, Integer> entry1 : documents.entrySet()){
                Document document = new Document("doc", entry1.getKey()).append("count", entry1.getValue());
                documentList.add(document);
            }

            long keyD = documentList.size(); 
            documentTerm.append("idf", (double) D/keyD);
            documentTerm.append("docs", documentList);
            outputInverseIndex.insertOne(documentTerm);
        }
    }

    public void createVectorDocuments(){

        HashMap<String, Double> idfDocs = new HashMap<>();  
        FindIterable<Document> findIterableInverseIndex = outputInverseIndex.find();
        Iterator iteratorInverseIndex = findIterableInverseIndex.iterator();
        while(iteratorInverseIndex.hasNext()){
            Document document = (Document) iteratorInverseIndex.next();
            idfDocs.put(document.getString("term"), document.getDouble("idf"));
        }
        FindIterable<Document> findIterableDirectIndex = inputDirectIndex.find();
        Iterator iteratorDirectIndex = findIterableDirectIndex.iterator();
        int i=1;
        while (iteratorDirectIndex.hasNext())
        {
            Document document = (Document) iteratorDirectIndex.next();
            Document vectorAndModule = createVectorAndModule(document, idfDocs);
            Document modifyDocument = new Document();
            modifyDocument.put("$set", vectorAndModule);
            inputDirectIndex.updateOne(document, modifyDocument);
        }
    }
    
    private static Document createVectorAndModule(Document document, HashMap<String, Double> idfs)
    {
        List<Document> terms = (List<Document>) document.get("terms");
        double sumSquare = 0;
        List<Document> vector = new ArrayList<>();
        for(Document docTerm : terms){
            String term = docTerm.getString("term");
            Double tf = docTerm.getDouble("tf");
            double idf = idfs.get(term);
            double element = tf * Math.log(idf);
            sumSquare += element * element;
            vector.add(new Document("term", term).append("value", element));
        }
        double module = Math.sqrt(sumSquare);
        Document vectorAndModule = new Document("module", module)
                .append("vector", vector);
        return vectorAndModule;
    }
}
