package com.riw.proiect;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.print.Doc;
import java.io.File;

@SpringBootApplication
public class riwApplication {

	private static final String HOST = "localhost";
	private static final int PORT = 27017;

	private static final String userName = "emanuel";
	private static final String databaseName = "riwDB";
	private static final char[] password = "RIW_mongoDB_pass".toCharArray();

	private static final String directIndexCollectionName = "directIndex";
	private static final String inverseIndexCollectionName = "inverseIndex";

	private static final String  sourceFolderPath = "C:\\Users\\Emanuel\\Desktop\\aaaa";

	public static MongoCollection<Document> directIndexForSearch = null;
	public static MongoCollection<Document> inverseIndexForSearch = null;

	public static void main(String[] args) {

		SpringApplication.run(riwApplication.class, args);
		boolean DIRECT_INDEX_IS_CREATED_ALREADY = true;
		try{
			MongoClient mongoClient = new MongoClient(HOST , PORT);
			System.out.println("Server connection successfully to locahost on port 27017!");
			MongoDatabase database = mongoClient.getDatabase(databaseName);
			MongoCollection<Document> directIndexCollection = database.getCollection(directIndexCollectionName);
			directIndexForSearch = directIndexCollection;
			System.out.println("Collection directIndex selected successfully!");

			//------------- Create direct index ------------------
			long startDirectIndex = System.currentTimeMillis();
			System.out.println("Start indexing...");
			if(directIndexCollection.count() == 0)
			{
				System.out.println("Begin direct indexing ...");
				DirectIndex directIndex = new DirectIndex();

				directIndex.setStopWords(ProcessFile.hashWithWordFromFile(new File("stopWords.txt"))); //source: https://www.ranks.nl/stopwords
				directIndex.setExceptionWords(ProcessFile.hashWithWordFromFile(new File("exceptionWords.txt")));
				directIndex.setOutputMongoCollection(directIndexCollection);

				directIndex.createIndex(new File(sourceFolderPath));
				DIRECT_INDEX_IS_CREATED_ALREADY = false;
   			    System.out.println("Direct index is created!");
			}
			long stopStart = System.currentTimeMillis();
			System.out.println("Time elapsed for creating direct index: " + ((double)(stopStart - startDirectIndex)/1000) + " seconds!");

			//---------------- Create inverse index ----------------------
			MongoCollection<Document> inverseIndexCollection = database.getCollection(inverseIndexCollectionName);
			InverseIndex inverseIndex = new InverseIndex(directIndexCollection, inverseIndexCollection);
			if((DIRECT_INDEX_IS_CREATED_ALREADY == true) && (inverseIndexCollection.count() > 0)){
				//System.out.println("Inverse index is already created!");
			}else{
				System.out.println("Begin inverse indexing...");
				inverseIndex.createInverseIndexLocal();
				inverseIndex.createVectorDocuments();
				System.out.println("Inverse index is created successfully!");
			}

			inverseIndexForSearch = inverseIndexCollection;
			long stopInverseIndex = System.currentTimeMillis();
			System.out.println("Time elapsed for creating inverse index: " + ((double)(stopInverseIndex - stopStart)/1000) + " seconds!");

		}catch (Exception exeption){
			System.out.println("Error!");
			exeption.printStackTrace();
		}finally {
		}
	}
}
