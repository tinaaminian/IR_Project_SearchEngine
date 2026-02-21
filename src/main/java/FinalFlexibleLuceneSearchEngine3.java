import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;
import java.util.*;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class FinalFlexibleLuceneSearchEngine3 {
    private static final String CONTENT_FIELD = "content";
    private static final String KGRAM_FIELD = "kgram";
    private static int kgramWordLimit = 5;
    private static boolean useEntireTextForKgram = false;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java " + FinalFlexibleLuceneSearchEngine.class.getName() + " <index dir> <data dir>");
        }

        String indexDir = args[0];
        String dataDir = args[1];

        try {
            // Scenario 1: K-gram on first 2 words
            System.out.println("Scenario 1: K-gram on first 5 words");
            createIndexes(dataDir, indexDir);

            // Scenario 2: K-gram on entire text
            System.out.println("\nScenario 2: K-gram on entire text");
            useEntireTextForKgram = true;
            createIndexes(dataDir, indexDir + "_full");

            searchIndexes(indexDir, indexDir + "_full");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createIndexes(String dataDir, String indexDir) throws IOException {
        long standardTime = createStandardIndex(dataDir, indexDir);
        long kgramTime = createKgramIndex(dataDir, indexDir + "_kgram");
        long kWordgramTime = createKWordgramIndex(dataDir, indexDir + "_kwordgram");

        System.out.println("Standard indexing time: " + standardTime + " ms");
        System.out.println("K-gram indexing time: " + kgramTime + " ms");
        System.out.println("K-Word-gram indexing time: " + kWordgramTime + " ms");
    }

    private static long createStandardIndex(String dataDir, String indexDir) throws IOException {
        long startTime = System.currentTimeMillis();

        FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(dir, config);

        indexDocuments(writer, dataDir, false, false);

        writer.commit();
        writer.close();

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private static long createKgramIndex(String dataDir, String indexDir) throws IOException {
        long startTime = System.currentTimeMillis();

        FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
        Analyzer kgramAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                return new TokenStreamComponents(new NGramTokenizer(2, 2));
            }
        };
        IndexWriterConfig config = new IndexWriterConfig(kgramAnalyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        indexDocuments(writer, dataDir, true, false);

        writer.commit();
        writer.close();

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
    
    private static long createKWordgramIndex(String dataDir, String indexDir) throws IOException {
        long startTime = System.currentTimeMillis();

        // Open the index directory
        FSDirectory dir = FSDirectory.open(Paths.get(indexDir));

        // Define the analyzer to create word-level bigrams using ShingleFilter
        Analyzer kgramAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                // Use a StandardTokenizer or WhitespaceTokenizer for word tokenization
                WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
                // Create a ShingleFilter to produce word-level bigrams
                ShingleFilter shingleFilter = new ShingleFilter(tokenizer, 2, 2);
                shingleFilter.setOutputUnigrams(false); // Only output bigrams
                return new TokenStreamComponents(tokenizer, shingleFilter);
            }
        };

        // Configure the index writer
        IndexWriterConfig config = new IndexWriterConfig(kgramAnalyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        // Index the documents
        indexDocuments(writer, dataDir, false, true);

        writer.commit();
        writer.close();

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private static void indexDocuments(IndexWriter writer, String dataDir, boolean isKgram, boolean isWordgram) throws IOException {
        File docDir = new File(dataDir);
        if (!docDir.exists() || !docDir.isDirectory()) {
            throw new IllegalArgumentException(dataDir + " does not exist or is not a directory.");
        }

        for (File file : docDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                Document doc = new Document();
                String content = readFileContent(file);

                if (isKgram) {
                    String kgramContent = useEntireTextForKgram ? content : limitToWords(content, kgramWordLimit);
                    doc.add(new TextField(KGRAM_FIELD, kgramContent, Field.Store.YES));
                    System.out.println("2-gram indexing for file: " + file.getName());
                    // Generate and display 3-gram indexing for each word
                    if (!useEntireTextForKgram) {
                        String[] words = kgramContent.split("\\s+");
                        for (String word : words) {
                            if (word.length() < 3) continue; // Skip words shorter than 3 chars
                            System.out.println("Word: " + word);

                            // Create 2-gram indexing for each word
                            for (int i = 0; i <= word.length() - 2; i++) {
                                String kgram = word.substring(i, i + 2);
                                System.out.println("  2-gram: " + kgram);
                            }
                        }

                    }
                } else if (isWordgram) {
                	String kgramContent = useEntireTextForKgram ? content : limitToWords(content, kgramWordLimit);
                    doc.add(new TextField(KGRAM_FIELD, kgramContent, Field.Store.YES));
                    System.out.println("3-gram indexing for file: " + file.getName());
                    // Generate and display 3-gram indexing for each word
                    if (!useEntireTextForKgram) {
                        String[] words = kgramContent.split("\\s+");
                        List<String> bigrams = new ArrayList<>();

                        for (int i = 0; i < words.length - 1; i++) {
                            bigrams.add(words[i] + " " + words[i + 1]); // Create bigram
                        }
                        
                        bigrams.forEach(System.out::println);
                    }
                } else {
                    doc.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                }

                writer.addDocument(doc);
                System.out.println("Indexed " + file.getName() + (isKgram ? " (K-gram)" : " (Standard)"));
            }
        }
    }

    private static List<String> suggestSpellCorrections(String queryTerm, IndexReader kgramReader) throws IOException {
        Set<String> suggestions = new HashSet<>();
        String[] queryKgrams = generateKgrams(queryTerm, 2);

        for (int i = 0; i < kgramReader.maxDoc(); i++) {
            Document doc = kgramReader.document(i);
            String content = doc.get(KGRAM_FIELD);
            if (content == null) continue;

            String[] candidateWords = content.split("\\s+");// \\s represents whitespace
            for (String candidate : candidateWords) {
                String[] candidateKgrams = generateKgrams(candidate, 2);
                int commonKgrams = countCommonKgrams(queryKgrams, candidateKgrams);

                // Use a threshold for common k-grams
                if (commonKgrams > 0.6 * queryKgrams.length) { // Example threshold
                    suggestions.add(cleanWord(candidate));
                }
            }
        }
        return new ArrayList<>(suggestions);
    }
    private static String cleanWord(String word) {
        return word.replaceAll("[^a-zA-Z]", ""); // Removes all non-letter characters
    }
    // Helper method to generate K-grams for a word
    private static String[] generateKgrams(String word, int k) {
        if (word.length() < k) return new String[0];
        String[] kgrams = new String[word.length() - k + 1];
        for (int i = 0; i <= word.length() - k; i++) {
            kgrams[i] = word.substring(i, i + k);
        }
        return kgrams;
    }

    // Helper method to count common k-grams between two words
    private static int countCommonKgrams(String[] kgrams1, String[] kgrams2) {
        Set<String> kgramSet1 = new HashSet<>(Arrays.asList(kgrams1));
        Set<String> kgramSet2 = new HashSet<>(Arrays.asList(kgrams2));
        kgramSet1.retainAll(kgramSet2);
        return kgramSet1.size();
    }

    private static String limitToWords(String text, int wordLimit) {
        String[] words = text.split("\\s+");
        StringBuilder limited = new StringBuilder();
        for (int i = 0; i < Math.min(words.length, wordLimit); i++) {
            limited.append(words[i]).append(" ");
        }
        return limited.toString().trim();
    }

    private static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private static void searchIndexes(String standardIndexDir, String kgramIndexDir) throws Exception {
        IndexReader standardReader = DirectoryReader.open(FSDirectory.open(Paths.get(standardIndexDir)));
        IndexSearcher standardSearcher = new IndexSearcher(standardReader);
        QueryParser standardParser = new QueryParser(CONTENT_FIELD, new StandardAnalyzer());

        IndexReader kgramReader = DirectoryReader.open(FSDirectory.open(Paths.get(kgramIndexDir + "_kgram")));
        IndexSearcher kgramSearcher = new IndexSearcher(kgramReader);


        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Enter your search query (or 'quit' to exit): ");
            String queryString = scanner.nextLine();

            if ("quit".equalsIgnoreCase(queryString)) {
                break;
            }

            // First, try searching the standard index
            Query query = standardParser.parse(queryString);
            TopDocs results = standardSearcher.search(query, 10);

            if (results.totalHits.value == 0) {
                // No exact match, try to suggest corrections
                System.out.println("No exact matches found. Checking for spelling suggestions...");
                List<String> suggestions = suggestSpellCorrections(queryString, kgramReader);
                if (!suggestions.isEmpty()) {
                    System.out.println("Did you mean:");
                    for (String suggestion : suggestions) {
                        System.out.println("  - " + suggestion);
                    }
                } else {
                    System.out.println("No spelling suggestions available.");
                }
            } else {
                // Display results from the standard index
                System.out.println("Found " + results.totalHits + " hits.");
                for (ScoreDoc scoreDoc : results.scoreDocs) {
                    Document doc = standardSearcher.doc(scoreDoc.doc);
                    String content = doc.get(CONTENT_FIELD);
                    System.out.println("Score: " + scoreDoc.score);
                    System.out.println("Content: " + content.substring(0, Math.min(content.length(), 200)) + "...");
                    System.out.println("--------------------");
                }
            }
        }

        standardReader.close();
        kgramReader.close();
        scanner.close();
    }
}


