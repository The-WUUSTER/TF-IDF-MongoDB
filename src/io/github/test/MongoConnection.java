package io.github.test;

import com.mongodb.*;
import java.util.*;
import io.github.sqlconnection.BaseConnection;

public class MongoConnection {
	
	/**
	 * Main method containing cosine similarity comparison algorithm
	 */
	public static void main(String[] args) {		
		BaseConnection bc = new BaseConnection();
		bc.connect();
		
		bc.setDBAndCollection("cs336", "unlabel_review");
		DBCursor no_split = bc.showRecords();		
		ArrayList<Review> reviews = new ArrayList<Review>(); //ArrayList masterlist of reviews
		HashMap<String, Double> idfs = new HashMap<String, Double>(); //IDF's HashMap for all terms in all reviews
		
		while(no_split.hasNext()){
			DBObject no_split_dbo = no_split.next();
			Review review = new Review((String) no_split_dbo.get("id"), (String) no_split_dbo.get("review"));
			review.updateTFs();
			reviews.add(review);
		}
		updateIDFs(reviews, idfs);
		
		/*
		for (Review r : reviews) {
			updateTFIDF(r, idfs);
		}
		*/
		
		//Forming R, a list of 6 random reviews and storing their TF information
		ArrayList<Review> R = new ArrayList<Review>();
		int count = 0;
		while (count < 6) {
			int rand = randInt(0, reviews.size() - 1);
			Review rand_review = reviews.get(rand);
			if (!R.contains(rand_review)) {
				R.add(rand_review);
				count++;
			}
		}
		
		for (Review sample_review : R) {
			updateTFIDFs(sample_review, idfs);
		}
		
		//Picking r* from R
		int rand2 = randInt(0, R.size() - 1);
		Review r_star = R.get(rand2);
		
		//Calculates N, number of documents that contain each word in the query
		Review query = makeQuery(r_star);
		HashMap<String, Integer> N = new HashMap<String, Integer>();
		System.out.println("Review (r*): " + r_star.getReview());
		System.out.println("Query: " + query.getReview());
		String[] query_text = query.getReview().split("['-]\\W+|[^\\w'-]\\W*");
		for (int y = 1; y < query_text.length; y++) {
			int n = 0;
			for (Review r : reviews) {
				String review_text[] = r.getReview().toLowerCase().split("['-]\\W+|[^\\w'-]\\W*");
				if (containsQuery(review_text, query_text[y])) {
					n++;
				}
			}
			N.put(query_text[y], n);
		}
		for (String word : N.keySet()) {
			System.out.println("Number of reviews containing " + "\"" + word + "\"" + ": " + N.get(word));
		}
		System.out.println("Number of unique words: " + idfs.size());
		System.out.println();
		
		//Calculates cosine similarity for each review to Q
		query.updateTFs();
		updateTFIDFs(query, idfs);
		System.out.println("TF values for query:");
		printHashMap(query.getTF());
		System.out.println();
		System.out.println("TF-IDF values for query:");
		printHashMap(query.getTFIDF());
		System.out.println();
		cosineSimilarity(query, R);
				
		//Cosine similarity for reviews compared to r*
		/*
		R.remove(r_star);
		cosineSimilarity(r_star, R);
		*/
		
		bc.close();
	}
	
	/**
	 * Returns a random integer between min and max (inclusive)
	 */
	public static int randInt(int min, int max) {
		return min + (int)(Math.random() * ((max - min) + 1));
	}
	
	/**
	 * Updates the IDF values for each term in all reviews in masterlist
	 */
	public static void updateIDFs(ArrayList<Review> masterlist, HashMap<String, Double> idfs) {
		for (Review m_review : masterlist) {
			String[] review_words = m_review.getReview().toLowerCase().split("['-]\\W+|[^\\w'-]\\W*");
			HashSet<String> uniques = new HashSet<String>();
			for (int i = 1; i < review_words.length; i++) {
				uniques.add(review_words[i]);
			}
			for (String word : uniques) {
				if (!idfs.containsKey(word)) {
					idfs.put(word, 1.0);
				}
				else {
					idfs.put(word, 1.0 + idfs.get(word));
				}
			}
		}
		
		//Updates it with log10-weighted values
		for (String word : idfs.keySet()) {
			idfs.put(word, Math.log10(masterlist.size()/idfs.get(word)));
		}
	}
	
	/**
	 * Sets the TFIDF values for a review given an idf HashMap
	 */
	public static void updateTFIDFs(Review r, HashMap<String, Double> idf) {
		for (String word : r.getTF().keySet()) {
			r.getTFIDF().put(word, r.getTF().get(word) * idf.get(word));
		}
	}
	
	/**
	 * Returns a query in the form of a review (picks 2 random words from given review r)
	 */
	public static Review makeQuery(Review r) {
		String[] review_words = r.getReview().toLowerCase().split("['-]\\W+|[^\\w'-]\\W*");
		int word_index1 = randInt(0, review_words.length - 1), word_index2 = randInt(0, review_words.length - 1);
		while (word_index1 == word_index2) {
			word_index2 = randInt(0, review_words.length - 1);
		}
		
		//Forms review with an arbitrary ID number and a "review" of 2 words in the review
		Review query = new Review("1337", "\"" + review_words[word_index1] + " " + review_words[word_index2] + "\"");
		return query;
	}
	
	/**
	 * Compares a random review to a set of reviews and calculates its cosine similarity value
	 */
	public static void cosineSimilarity(Review random, ArrayList<Review> masterlist) {
		double cosine_value;
		for (Review review : masterlist) {
			double random_total = 0.0, review_total = 0.0, rev_ran_total = 0.0;
			
			HashSet<String> union = new HashSet<String>();
			union.addAll(review.getTFIDF().keySet());
			union.addAll(random.getTFIDF().keySet());
			
			for (String word : union) {
				double tfidf_review = 0.0, tfidf_random = 0.0;
				
				if (random.getTFIDF().containsKey(word)) {
					tfidf_random = random.getTFIDF().get(word);
				}
				if (review.getTFIDF().containsKey(word)) {
					tfidf_review = review.getTFIDF().get(word);
				}
				rev_ran_total += (tfidf_review * tfidf_random);
				random_total += Math.pow(tfidf_random, 2);
				review_total += Math.pow(tfidf_review, 2);
			}
			random_total = Math.sqrt(random_total);
			review_total = Math.sqrt(review_total);
			cosine_value = rev_ran_total / (random_total * review_total);
			
			//Print out relevant info
			System.out.println(random.toString());
			System.out.println(review.toString());
			System.out.println("Cosine value: " + cosine_value + "\n");
		}
	}
	
	/**
	 * Prints HashMap key-value pairs in the form: (key, value)
	 */
	public static void printHashMap(HashMap<String, Double> map) {
		for (String word : map.keySet()) {
			System.out.println("(" + word + ", " + map.get(word) + ")");
		}
	}
	
	/**
	 * Checks if the array form of a review's transcript contains query
	 * Returns true if it does, false otherwise
	 */
	public static boolean containsQuery(String[] text, String query) {
		for (int i = 1; i < text.length; i++) {
			if (text[i].equals(query)) {
				return true;
			}
		}
		return false;
	}
}
